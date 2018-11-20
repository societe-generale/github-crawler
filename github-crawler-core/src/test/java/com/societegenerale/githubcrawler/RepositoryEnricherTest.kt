package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.repoTaskToPerform.RepoTaskToPerform
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
            reason = null,
            url="http://hello",
            excluded = false,
            branchesToParse = listOf(masterBranch)
    )

    @Test
    fun shouldHaveEmptyIndicatorsWhenNoneIsConfigured() {

        val properties = GitHubCrawlerProperties(GithubConfig())

        val repoAfterProcessing=repositoryEnricher.fetchIndicatorsValues(repository,properties)

        assertThat(repoAfterProcessing.indicators[masterBranch]).isEmpty()
    }

    @Test
    fun shouldMergeMiscTaskResultsAsExpected() {

        val masterBranch=Branch("master")
        val branch1=Branch("branch1")

        val tasksToPerform= listOf(DummyActionToPerform(mapOf(Pair(masterBranch, mapOf(Pair("search1","value1"))))),
                                   DummyActionToPerform(mapOf(Pair(masterBranch, mapOf(Pair("search2","value2"))))),
                                   DummyActionToPerform(mapOf(Pair(branch1, mapOf(Pair("search1","value1a"))))))

        val repoAfterProcessing=repositoryEnricher.performMiscTasks(repository,tasksToPerform)

        val resultForMasterBranch=repoAfterProcessing.miscTasksResults[masterBranch]!!
        assertThat(resultForMasterBranch.keys).hasSize(2)
        assertThat(resultForMasterBranch["search1"]).isEqualTo("value1")
        assertThat(resultForMasterBranch["search2"]).isEqualTo("value2")

        val resultForBranch1=repoAfterProcessing.miscTasksResults[branch1]!!
        assertThat(resultForBranch1.keys).hasSize(1)
        assertThat(resultForBranch1["search1"]).isEqualTo("value1a")
    }

    class DummyActionToPerform(val resultToReturn : Map<Branch, Map<String, Any>>): RepoTaskToPerform{

        override fun perform(repository: Repository): Map<Branch, Map<String, Any>> {
           return resultToReturn
        }
    }
}