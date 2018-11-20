package com.societegenerale.githubcrawler.repoTaskToPerform

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.RemoteGitHub


class PathsForHitsOnRepoSearch( private val taskName: String,
                                private val searchQuery: String,
                                private val remoteGitHub: RemoteGitHub) : RepoTaskToPerform {

    override fun perform(repository: Repository): Repository {

        val searchResult= remoteGitHub.fetchCodeSearchResult(repository, searchQuery)

        val paths=if(searchResult.totalCount>0){
                searchResult.items.map { i -> i.path }
            }
            else{
                "not found"
            }


        val existingMiscTasksResultsForDefaultBranch=repository.miscTasksResults[Branch(repository.defaultBranch)].orEmpty().toMutableMap()

        existingMiscTasksResultsForDefaultBranch.put(taskName,paths)

        val updatedMiscTasksResults=repository.miscTasksResults.toMutableMap()
        updatedMiscTasksResults.put(Branch(repository.defaultBranch),existingMiscTasksResultsForDefaultBranch)

        return repository.copy(miscTasksResults = updatedMiscTasksResults)

    }

}

class PathsForHitsOnRepoSearchBuilder(private val remoteGitHub: RemoteGitHub) : RepoTaskBuilder  {

    override val type="pathsForHitsOnRepoSearch"


    override fun buildTask(name: String, params : Map<String,String>) : RepoTaskToPerform{

        return PathsForHitsOnRepoSearch(name, params["searchQuery"]!!,remoteGitHub)

    }

}