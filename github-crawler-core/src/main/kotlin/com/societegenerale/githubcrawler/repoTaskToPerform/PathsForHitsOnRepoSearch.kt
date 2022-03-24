package com.societegenerale.githubcrawler.repoTaskToPerform

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.RemoteSourceControl


/**
 * This task will perform a search on the repository (using Github search API), and return an indicator with the provided name, <b>and the list of items' paths (relative to that repository) that matched the search</b>.
 *
 * Note : It will NOT perform the search in each branch of the repository
 */
class PathsForHitsOnRepoSearch( private val taskName: String,
                                private val searchQuery: String,
                                private val remoteSourceControl: RemoteSourceControl) : RepoTaskToPerform {

    override fun perform(repository: Repository): Map<Branch, Pair<String, Any>> {

        val searchResult= remoteSourceControl.fetchCodeSearchResult(repository.fullName, searchQuery)

        val paths=if(searchResult.totalCount>0){
                searchResult.items.map { i -> i.path }
            }
            else{
                listOf("not found")
            }


        return hashMapOf(Pair(Branch(repository.defaultBranch), Pair(taskName,paths)))

    }

}

class PathsForHitsOnRepoSearchBuilder(private val remoteSourceControl: RemoteSourceControl) : RepoTaskBuilder  {

    override val type="pathsForHitsOnRepoSearch"


    override fun buildTask(name: String, params : Map<String,String>) : RepoTaskToPerform{

        return PathsForHitsOnRepoSearch(name, params["queryString"]!!,remoteSourceControl)

    }

}