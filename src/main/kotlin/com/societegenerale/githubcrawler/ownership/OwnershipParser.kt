package com.societegenerale.githubcrawler.ownership


interface OwnershipParser {
    fun computeOwnershipFor(repositoryFullName: String, lastCommitNumber: Int): String
}