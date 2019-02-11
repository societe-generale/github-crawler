package com.societegenerale.githubcrawler.repoTaskToPerform

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.RemoteGitHub



class NbBranchesOnRepo( private val taskName: String,
                       private val remoteGitHub: RemoteGitHub) : RepoTaskToPerform {

    override fun perform(repository: Repository): Map<Branch, Pair<String, Any>> {

        val nbBranches= remoteGitHub.fetchRepoBranches(repository.fullName).size

        return hashMapOf(Pair(Branch(repository.defaultBranch), Pair(taskName,nbBranches)))

    }

}

class NbBranchesOnRepoBuilder(private val remoteGitHub: RemoteGitHub) : RepoTaskBuilder  {

    override val type="nbBranchesOnRepo"


    override fun buildTask(name: String, params : Map<String,String>) : RepoTaskToPerform{

        return NbBranchesOnRepo(name,remoteGitHub)

    }

}