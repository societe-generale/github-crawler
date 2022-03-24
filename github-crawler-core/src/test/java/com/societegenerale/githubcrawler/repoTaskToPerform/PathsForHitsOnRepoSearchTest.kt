package com.societegenerale.githubcrawler.repoTaskToPerform

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.model.SearchResult
import com.societegenerale.githubcrawler.model.SearchResultItem
import com.societegenerale.githubcrawler.remote.RemoteSourceControl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.*

class PathsForHitsOnRepoSearchTest {

    private val mockRemoteGithub = mock(RemoteSourceControl::class.java)

    private val repoToSearch = Repository(name = "repo1",
            creationDate = Date(),
            config= null,
            defaultBranch = "master",
            fullName = "orgName/repoName1",
            lastUpdateDate = Date(),
            reason = null,
            url="http://hello"
    )

    private val testTaskName = "myTestSearch"

    private val pathsForHitsOnRepoSearch=PathsForHitsOnRepoSearch(testTaskName, "someSearch",mockRemoteGithub)

    @Test
    fun shouldYieldListOfPathsFound() {

        val mockSearchResults = listOf(SearchResultItem("path1"), SearchResultItem("path2"))

        `when`(mockRemoteGithub.fetchCodeSearchResult(repoToSearch.fullName, "someSearch"))
                .thenReturn(SearchResult(mockSearchResults.size,mockSearchResults))

        val searchResult = pathsForHitsOnRepoSearch.perform(repoToSearch)

        val searchResultOnRepo=searchResult[Branch("master")] as Pair

        val valueForBranch = searchResultOnRepo.second as List<*>

        assertThat(valueForBranch).containsExactly("path1", "path2")

    }


    @Test
    fun shouldYield_NotFound_WhenNoMatch() {

        `when`(mockRemoteGithub.fetchCodeSearchResult(repoToSearch.fullName, "someSearch"))
                .thenReturn(SearchResult(0, emptyList()))

        val searchResult = pathsForHitsOnRepoSearch.perform(repoToSearch)

        val searchResultOnRepo=searchResult[Branch("master")] as Pair

        assertThat(searchResultOnRepo.second as List<String>).containsOnly("not found")
    }
}