package com.societegenerale.githubcrawler.repoTaskToPerform

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.RemoteSourceControl



class NbOpenPRsOnRepo( private val taskName: String,
                       private val remoteSourceControl: RemoteSourceControl) : RepoTaskToPerform {

    override fun perform(repository: Repository): Map<Branch, Pair<String, Any>> {

        val nbOpenPrs= remoteSourceControl.fetchOpenPRs(repository.fullName).size

        return hashMapOf(Pair(Branch(repository.defaultBranch), Pair(taskName,nbOpenPrs)))

    }

}

class NbOpenPRsOnRepoBuilder(private val remoteSourceControl: RemoteSourceControl) : RepoTaskBuilder  {

    override val type="nbOpenPRsOnRepo"


    override fun buildTask(name: String, params : Map<String,String>) : RepoTaskToPerform{

        return NbOpenPRsOnRepo(name,remoteSourceControl)

    }

}