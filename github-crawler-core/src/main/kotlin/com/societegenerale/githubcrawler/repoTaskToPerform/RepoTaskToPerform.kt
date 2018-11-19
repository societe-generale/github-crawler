package com.societegenerale.githubcrawler.repoTaskToPerform

import com.societegenerale.githubcrawler.model.Repository

interface RepoTaskToPerform {

    fun perform(repository: Repository): Repository

}
