package com.societegenerale.githubcrawler.repoTaskToPerform

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.RemoteSourceControl



class NbBranchesOnRepo( private val taskName: String,
                       private val remoteSourceControl: RemoteSourceControl) : RepoTaskToPerform {

    override fun perform(repository: Repository): Map<Branch, Pair<String, Any>> {

        val nbBranches= remoteSourceControl.fetchRepoBranches(repository.fullName).size

        return hashMapOf(Pair(Branch(repository.defaultBranch), Pair(taskName,nbBranches)))

    }

}

class NbBranchesOnRepoBuilder(private val remoteSourceControl: RemoteSourceControl) : RepoTaskBuilder  {

    override val type="nbBranchesOnRepo"


    override fun buildTask(name: String, params : Map<String,String>) : RepoTaskToPerform{

        return NbBranchesOnRepo(name,remoteSourceControl)

    }

}