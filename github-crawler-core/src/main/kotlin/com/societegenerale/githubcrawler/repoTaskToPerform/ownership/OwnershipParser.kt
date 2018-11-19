package com.societegenerale.githubcrawler.repoTaskToPerform.ownership


interface OwnershipParser {
    fun computeOwnershipFor(repositoryFullName: String, lastCommitNumber: Int): String
}