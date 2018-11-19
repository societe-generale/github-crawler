package com.societegenerale.githubcrawler.repoTaskToPerform

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.RemoteGitHub


class CountHitsOnRepoSearch(private val remoteGitHub: RemoteGitHub,
                            private val searchQuery: String) : RepoTaskToPerform {

    override fun perform(repository: Repository): Repository {

        val nbHitsMatching= remoteGitHub.fetchCodeSearchResult(repository, searchQuery).totalCount.toString()

        val existingMiscTasksResults=repository.miscTasksResults.toMutableMap()

        val existingMiscTasksResultsForDefaultBranch=existingMiscTasksResults[Branch(repository.defaultBranch)].orEmpty()

        existingMiscTasksResultsForDefaultBranch.plus(Pair(repository.defaultBranch,nbHitsMatching))

        return repository.copy(miscTasksResults = existingMiscTasksResults)

    }

}