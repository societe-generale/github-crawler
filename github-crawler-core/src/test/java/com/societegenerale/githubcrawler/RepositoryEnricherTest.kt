package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

import org.mockito.Mockito
import java.util.*

class RepositoryEnricherTest {

    private val mockRemoteGitHub: RemoteGitHub = Mockito.mock(RemoteGitHub::class.java)

    private val repositoryEnricher = RepositoryEnricher(mockRemoteGitHub)

    private val masterBranch=Branch("master")

    val repository = Repository(name = "someRepo",
            creationDate = Date(),
            config= null,
            defaultBranch = "master",
            fullName = "orgName/someRepo",
            lastUpdateDate = Date(),
            ownerTeam = null,
            reason = null,
            url="http://hello",
            excluded = false,
            branchesToParse = listOf(masterBranch)
    )

    @Test
    fun shouldHaveEmptyIndicatorsWhenNoneIsConfigured() {

        val properties = GitHubCrawlerProperties()

        val repoAfterProcessing=repositoryEnricher.fetchIndicatorsValues(repository,properties)

        assertThat(repoAfterProcessing.indicators[masterBranch]).isEmpty()

    }
}