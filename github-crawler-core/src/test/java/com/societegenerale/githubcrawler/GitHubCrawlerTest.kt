package com.societegenerale.githubcrawler

import com.google.common.collect.ImmutableList
import com.jayway.awaitility.Awaitility.await
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput
import com.societegenerale.githubcrawler.ownership.NoOpOwnershipParser
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.springframework.core.env.Environment
import java.io.IOException
import java.util.*


class GitHubCrawlerTest {

    val mockRemoteGitHub: RemoteGitHub=mock(RemoteGitHub::class.java)
    val ownershipParser =NoOpOwnershipParser()
    val output= InMemoryGitHubCrawlerOutput()
    val outputs: List<GitHubCrawlerOutput> = arrayListOf(output)
    val repositoryEnricher = RepositoryEnricher(mockRemoteGitHub)
    val gitHubCrawlerProperties = GitHubCrawlerProperties()
    val mockEnvironment =mock(Environment::class.java)
    val organizationName ="myOrg"
    val gitHubUrl ="githubUrl"
    val mockConfigValidator= mock(ConfigValidator::class.java)

    lateinit var gitHubCrawler :  GitHubCrawler

    @Before
    fun setUp() {
        gitHubCrawler = GitHubCrawler(mockRemoteGitHub, ownershipParser, outputs, repositoryEnricher, gitHubCrawlerProperties, mockEnvironment, organizationName, gitHubUrl, mockConfigValidator)

        `when`(mockRemoteGitHub.fetchRepositories(organizationName)).thenReturn(setOf(
                                Repository(url="url1",fullName = "fullRepo1",name= "repo1",defaultBranch="master",creationDate = Date(),lastUpdateDate = Date()),
                                Repository(url="url2",fullName = "fullRepo2",name= "repo2",defaultBranch="master",creationDate = Date(),lastUpdateDate = Date())
        ))

        `when`(mockRemoteGitHub.fetchRepoConfig(anyString(),anyString())).thenReturn(RepositoryConfig())

        `when`(mockConfigValidator.getValidationErrors()).thenReturn(ImmutableList.of())
        `when`(mockEnvironment.activeProfiles).thenReturn(arrayOf("profile1"))
    }


    @Test
    @Throws(IOException::class)
    fun gitHubOrganisationPollerWorks() {

        gitHubCrawler.crawl()

        val processedRepositories = output.analyzedRepositories.values

        await().atMost(1, java.util.concurrent.TimeUnit.SECONDS)
                .until({ assertThat(processedRepositories).hasSize(2) })


        assertThat(processedRepositories).hasSize(2)
        assertThat(processedRepositories.map { r -> r.name }).containsExactlyInAnyOrder("repo1","repo2")
        assertThat(processedRepositories.map { r -> r.defaultBranch }).containsOnly("master")

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

}

