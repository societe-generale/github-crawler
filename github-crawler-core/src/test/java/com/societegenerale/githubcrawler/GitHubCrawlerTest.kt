package com.societegenerale.githubcrawler

import com.google.common.collect.ImmutableList
import com.jayway.awaitility.Awaitility.await
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput
import com.societegenerale.githubcrawler.parsers.SimpleFilePathParser
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.repoTaskToPerform.ownership.NoOpOwnershipParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.springframework.core.env.Environment
import java.io.IOException
import java.util.*


class GitHubCrawlerTest {

    val mockRemoteGitHub: RemoteGitHub = mock(RemoteGitHub::class.java)
    val ownershipParser = NoOpOwnershipParser()
    val output = InMemoryGitHubCrawlerOutput()
    val outputs: List<GitHubCrawlerOutput> = arrayListOf(output)
    val repositoryEnricher = RepositoryEnricher(mockRemoteGitHub)

    val fileToParse="pom.xml"
    val indicator=IndicatorDefinition("indicName","findFilePath")

    val fileContentParsers=listOf(SimpleFilePathParser())

    val gitHubCrawlerProperties = GitHubCrawlerProperties(githubConfig= GithubConfig(), indicatorsToFetchByFile = mapOf(Pair(FileToParse(fileToParse, null), listOf(indicator))))
    val mockEnvironment = mock(Environment::class.java)
    val organizationName = "myOrg"
    val mockConfigValidator = mock(ConfigValidator::class.java)

    lateinit var gitHubCrawler: GitHubCrawler

    @Before
    fun setUp() {

        gitHubCrawler = GitHubCrawler(mockRemoteGitHub, outputs, repositoryEnricher, gitHubCrawlerProperties, mockEnvironment, organizationName, mockConfigValidator,fileContentParsers)

        `when`(mockRemoteGitHub.fetchRepositories(organizationName)).thenReturn(setOf(
                Repository(url = "url1", fullName = "fullRepo1", name = "repo1", defaultBranch = "master", creationDate = Date(), lastUpdateDate = Date(), topics = listOf("topic1a", "topic1b")),
                Repository(url = "url2", fullName = "fullRepo2", name = "repo2", defaultBranch = "master", creationDate = Date(), lastUpdateDate = Date(), groups = listOf("group2a", "group2b"))
        ))

        `when`(mockRemoteGitHub.fetchRepoConfig(anyString(), anyString())).thenReturn(RepositoryConfig())

        `when`(mockRemoteGitHub.fetchFileContent(any(String::class.java), any(String::class.java),eq(fileToParse))).thenReturn("")


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

        `when`(mockRemoteGitHub.fetchRepoConfig("fullRepo1", "master")).thenReturn(RepositoryConfig(excluded = true))

        val processedRepositories = crawlAndWaitUntilWeHaveRecordsInOutput(1)

        assertThat(processedRepositories.keys).containsOnly("repo2")
        assertThat(output.analyzedRepositories["repo2"]?.excluded).isFalse()
    }

    @Test
    fun excludedRepositoriesOnRepoConfigSideShouldBeOutputIfConfigured() {

        `when`(mockRemoteGitHub.fetchRepoConfig("fullRepo1", "master")).thenReturn(RepositoryConfig(excluded = true))

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

        verify(mockRemoteGitHub, times(1)).fetchFileContent("fullRepo2", "master", "pom.xml")
        //TODO we shouldn't check for pom.xml but for anyString()
        verify(mockRemoteGitHub, never()).fetchFileContent("fullRepo1", "master", "pom.xml")

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

