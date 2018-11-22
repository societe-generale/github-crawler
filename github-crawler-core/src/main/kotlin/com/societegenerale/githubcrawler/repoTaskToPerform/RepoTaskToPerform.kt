package com.societegenerale.githubcrawler.repoTaskToPerform

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository

/**
 * Implementing classes, if configured, will be called for each repository being analyzed.
 *
 * Since the repository tasks need to be configured from the properties, but also from the infrastructure (most of them will likely need a RemoteGitHub instance),
 * each implementing class needs its dedicated builder, implementing RepoTaskBuilder interface. we like to keep both classes in the same file, named after the task.
 *
 * To be visible at runtime, matching RepoTaskBuilder need to be instantiated.
 *
 * @see RepoTaskBuilder
 *
 */
interface RepoTaskToPerform {

    /**
     * Implementing classes may or may not look at all the branches of the repository when performing their action.
     *
     * By returning a Map with Branch as the key, we allow implementing classes to look at all the branches of the repository or not :
     * - If they don't, they will usually perform the action at default branch level, and the key will represent that branch
     * - If they do, then the action will be performed on each branch, and the (indicatorName,indicatorValue) pair will be stored as a value in the map, for each branch
     *
     */
    fun perform(repository: Repository): Map<Branch, Pair<String, Any>>

}
