package com.societegenerale.githubcrawler.repoTaskToPerform

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.RemoteGitHub


/**
 * This task will perform a search on the repository (using Github search API), and return an indicator with the provided name, <b>and the number of hits returned by the search</b>.
 *
 * Note : It will NOT perform the search in each branch of the repository
 */
class CountHitsOnRepoSearch(private val name : String,
                            private val remoteGitHub: RemoteGitHub,
                            private val searchQuery: String) : RepoTaskToPerform {

    override fun perform(repository: Repository): Map<Branch, Pair<String, Any>> {

        val nbHitsMatching= remoteGitHub.fetchCodeSearchResult(repository.fullName, searchQuery).totalCount.toString()

        return hashMapOf(Pair(Branch(repository.defaultBranch), Pair(name,nbHitsMatching)))

    }

}

class CountHitsOnRepoSearchBuilder(private val remoteGitHub: RemoteGitHub) : RepoTaskBuilder  {

    override val type="countHitsOnRepoSearch"


    override fun buildTask(name: String, params : Map<String,String>) : RepoTaskToPerform{

        return CountHitsOnRepoSearch(name, remoteGitHub, params["queryString"]!!)

    }

}