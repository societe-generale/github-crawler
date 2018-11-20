package com.societegenerale.githubcrawler.repoTaskToPerform


interface RepoTaskBuilder {

    val type: String

    fun buildTask(name: String, params : Map<String,String>) : RepoTaskToPerform

}