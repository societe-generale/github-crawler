package com.societegenerale.githubcrawler.repoTaskToPerform


/**
 *
 * Thanks to these builders, we'll be able to build actual instances of RepoTaskToPerform, by combining properties from config file and possibly other "core" objects, like RemoteGitHub.
 *
 * To be available at runtime, they need to be instantiated in GitHubCrawlerMiscTasksConfig
 *
 * @see: RepoTaskToPerform
 * @see GitHubCrawlerMiscTasksConfig
 */
interface RepoTaskBuilder {

    /**
     * name by which the RepoTaskBuilder will be referenced, in config
     */
    val type: String

    /**
     * @param name: the name of the task, that will also become the name of the generated indicator
     * @param params: the additional config properties required to build and actual RepoTaskToPerform
     */
    fun buildTask(name: String, params : Map<String,String>) : RepoTaskToPerform

}