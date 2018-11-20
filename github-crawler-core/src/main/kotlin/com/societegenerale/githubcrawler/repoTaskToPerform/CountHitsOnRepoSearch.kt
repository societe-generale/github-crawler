package com.societegenerale.githubcrawler.repoTaskToPerform

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.RemoteGitHub


class CountHitsOnRepoSearch(private val name : String,
                            private val remoteGitHub: RemoteGitHub,
                            private val searchQuery: String) : RepoTaskToPerform {

    override fun perform(repository: Repository): Map<Branch, Map<String, Any>> {

        val nbHitsMatching= remoteGitHub.fetchCodeSearchResult(repository, searchQuery).totalCount.toString()

        return hashMapOf(Pair(Branch(repository.defaultBranch), hashMapOf(Pair(name,nbHitsMatching))))

    }

}

class CountHitsOnRepoSearchBuilder(private val remoteGitHub: RemoteGitHub) : RepoTaskBuilder  {

    override val type="countHitsOnRepoSearch"


    override fun buildTask(name: String, params : Map<String,String>) : RepoTaskToPerform{

        return CountHitsOnRepoSearch(name, remoteGitHub, params["queryString"]!!)

    }

}