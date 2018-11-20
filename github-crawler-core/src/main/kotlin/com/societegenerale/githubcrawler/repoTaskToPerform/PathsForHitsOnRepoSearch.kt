package com.societegenerale.githubcrawler.repoTaskToPerform

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.RemoteGitHub


class PathsForHitsOnRepoSearch( private val taskName: String,
                                private val searchQuery: String,
                                private val remoteGitHub: RemoteGitHub) : RepoTaskToPerform {

    override fun perform(repository: Repository): Map<Branch, Map<String, Any>> {

        val searchResult= remoteGitHub.fetchCodeSearchResult(repository, searchQuery)

        val paths=if(searchResult.totalCount>0){
                searchResult.items.map { i -> i.path }
            }
            else{
                "not found"
            }


        return hashMapOf(Pair(Branch(repository.defaultBranch), hashMapOf(Pair(taskName,paths))))

    }

}

class PathsForHitsOnRepoSearchBuilder(private val remoteGitHub: RemoteGitHub) : RepoTaskBuilder  {

    override val type="pathsForHitsOnRepoSearch"


    override fun buildTask(name: String, params : Map<String,String>) : RepoTaskToPerform{

        return PathsForHitsOnRepoSearch(name, params["searchQuery"]!!,remoteGitHub)

    }

}