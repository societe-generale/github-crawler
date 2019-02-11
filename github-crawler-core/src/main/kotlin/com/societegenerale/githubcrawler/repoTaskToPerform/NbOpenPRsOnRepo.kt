package com.societegenerale.githubcrawler.repoTaskToPerform

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.RemoteGitHub



class NbOpenPRsOnRepo( private val taskName: String,
                       private val remoteGitHub: RemoteGitHub) : RepoTaskToPerform {

    override fun perform(repository: Repository): Map<Branch, Pair<String, Any>> {

        val nbOpenPrs= remoteGitHub.fetchOpenPRs(repository.fullName).size

        return hashMapOf(Pair(Branch(repository.defaultBranch), Pair(taskName,nbOpenPrs)))

    }

}

class NbOpenPRsOnRepoBuilder(private val remoteGitHub: RemoteGitHub) : RepoTaskBuilder  {

    override val type="nbOpenPRsOnRepo"


    override fun buildTask(name: String, params : Map<String,String>) : RepoTaskToPerform{

        return NbOpenPRsOnRepo(name,remoteGitHub)

    }

}