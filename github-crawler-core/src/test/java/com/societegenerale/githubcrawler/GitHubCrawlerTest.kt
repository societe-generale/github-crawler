package com.societegenerale.githubcrawler

import com.google.common.collect.ImmutableList
import com.jayway.awaitility.Awaitility.await
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput
import com.societegenerale.githubcrawler.parsers.SimpleFilePathParser
import com.societegenerale.githubcrawler.remote.RemoteSourceControl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.mockito.Mockito
import org.mockito.Mockito.*
import org.springframework.core.env.Environment
import java.io.IOException
import java.util.*


class GitHubCrawlerTest {

    private val mockRemoteSourceControl: RemoteSourceControl = mock(RemoteSourceControl::class.java)

    private val output = InMemoryGitHubCrawlerOutput()
    private val outputs: List<GitHubCrawlerOutput> = arrayListOf(output)


    private val fileToParse="pom.xml"
    private val indicator=IndicatorDefinition("indicName","findFilePath")

    private val availableParsersAndTasks = AvailableParsersAndTasks(listOf(SimpleFilePathParser()), emptyList())

    private val repositoryEnricher = RepositoryEnricher(mockRemoteSourceControl,availableParsersAndTasks)

    private val gitHubCrawlerProperties = GitHubCrawlerProperties(sourceControl= SourceControlConfig(), indicatorsToFetchByFile = mapOf(Pair(FileToParse(fileToParse, null), listOf(indicator))))
    private val mockEnvironment = mock(Environment::class.java)
    private val organizationName = "myOrg"
    private val mockConfigValidator = mock(ConfigValidator::class.java)

    private lateinit var gitHubCrawler: GitHubCrawler

    @BeforeEach
    fun setUp() {

        gitHubCrawler = GitHubCrawler(mockRemoteSourceControl, outputs, repositoryEnricher, gitHubCrawlerProperties, mockEnvironment, organizationName, mockConfigValidator,availableParsersAndTasks)

        `when`(mockRemoteSourceControl.fetchRepositories(organizationName)).thenReturn(setOf(
                Repository(url = "url1", fullName = "fullRepo1", name = "repo1", defaultBranch = "master", creationDate = Date(), lastUpdateDate = Date(), topics = listOf("topic1a", "topic1b")),
                Repository(url = "url2", fullName = "fullRepo2", name = "repo2", defaultBranch = "master", creationDate = Date(), lastUpdateDate = Date(), groups = listOf("group2a", "group2b"))
        ))

        `when`(mockRemoteSourceControl.fetchRepoConfig(anyString(), anyString())).thenReturn(RepositoryConfig())

        `when`(mockRemoteSourceControl.fetchFileContent(any(String::class.java), any(String::class.java),eq(fileToParse))).thenReturn("")


        `when`(mockConfigValidator.getValidationErrors()).thenReturn(ImmutableList.of())
        `when`(mockEnvironment.activeProfiles).thenReturn(arrayOf("profile1"))
    }


    @Test
    fun gitHubOrganisationPollerWorks() {

        val processedRepositories = crawlAndWaitUntilWeHaveRecordsInOutput(2)

        assertThat(processedRepositories.keys).containsExactlyInAnyOrder("repo1", "repo2")
        assertThat(processedRepositories.values.map { r -> r.defaultBranch }).containsOnly("master")

    }

    @Test
    fun excludedRepositoriesOnRepoConfigSideAreNotOutputByDefault() {

        `when`(mockRemoteSourceControl.fetchRepoConfig("fullRepo1", "master")).thenReturn(RepositoryConfig(excluded = true))

        val processedRepositories = crawlAndWaitUntilWeHaveRecordsInOutput(1)

        assertThat(processedRepositories.keys).containsOnly("repo2")
        assertThat(output.analyzedRepositories["repo2"]?.excluded).isFalse()
    }

    @Test
    fun excludedRepositoriesOnRepoConfigSideShouldBeOutputIfConfigured() {

        `when`(mockRemoteSourceControl.fetchRepoConfig("fullRepo1", "master")).thenReturn(RepositoryConfig(excluded = true))

        gitHubCrawlerProperties.publishExcludedRepositories = true

        val processedRepositories = crawlAndWaitUntilWeHaveRecordsInOutput(2)

        assertThat(processedRepositories.keys).containsExactlyInAnyOrder("repo1", "repo2")
        assertThat(processedRepositories["repo2"]?.excluded).isFalse()
        assertThat(processedRepositories["repo1"]?.excluded).isTrue()
    }

    @Test
    fun reposExcludedOnCrawlerConfigSideAreNotInOutput() {

        gitHubCrawlerProperties.repositoriesToExclude = Arrays.asList("repo2")

        val processedRepositories = crawlAndWaitUntilWeHaveRecordsInOutput(1)

        assertThat(processedRepositories.keys).containsExactly("repo1")
    }

    @Test
    fun excludingRepositoriesOnServerConfigSideWithMultipleRegexp() {

        gitHubCrawlerProperties.repositoriesToExclude = Arrays.asList(".*1$", ".*2$")

        val processedRepositories = crawlAndWaitUntilWeHaveRecordsInOutput(0)

        assertThat(processedRepositories.keys).isEmpty()
    }

    @Test
    fun excludingRepositoriesOnServerConfigSideWithSingleRegexp() {

        gitHubCrawlerProperties.repositoriesToExclude = Arrays.asList(".*1$")

        val processedRepositories = crawlAndWaitUntilWeHaveRecordsInOutput(1)

        verify(mockRemoteSourceControl, times(1)).fetchFileContent("fullRepo2", "master", "pom.xml")
        //TODO we shouldn't check for pom.xml but for anyString()
        verify(mockRemoteSourceControl, never()).fetchFileContent("fullRepo1", "master", "pom.xml")

        assertThat(processedRepositories.keys).containsExactly("repo2")
    }

    @Test
    fun shouldCopyTagsFromRepoTopicsOnRepoResult() {

        val processedRepositories = crawlAndWaitUntilWeHaveRecordsInOutput(2)

        assertThat(processedRepositories.get("repo1")?.tags).containsExactlyInAnyOrder("topic1a", "topic1b")
        assertThat(processedRepositories.get("repo2")?.tags).isEmpty()
    }

    private fun crawlAndWaitUntilWeHaveRecordsInOutput(nbExpectedRecords: Int): HashMap<String, Repository> {

        gitHubCrawler.crawl()

        await().atMost(2, java.util.concurrent.TimeUnit.SECONDS)
                .until({ assertThat(output.analyzedRepositories.values).hasSize(nbExpectedRecords) })

        return output.analyzedRepositories

    }

    inner class InMemoryGitHubCrawlerOutput : GitHubCrawlerOutput {

        val analyzedRepositories = HashMap<String, Repository>()

        @Throws(IOException::class)
        override fun output(analyzedRepository: Repository) {
            this.analyzedRepositories[analyzedRepository.name] = analyzedRepository
        }

        fun reset() {
            analyzedRepositories.clear()
        }

        @Throws(IOException::class)
        override fun finalizeOutput() {
            //do nothing
        }
    }

    private fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

    private fun <T> eq(obj: T): T = Mockito.eq<T>(obj)


}

