package com.societegenerale.githubcrawler.repoTaskToPerform.ownership

import org.slf4j.LoggerFactory


class NoOpOwnershipParser : OwnershipParser {

    companion object {
        private val log = LoggerFactory.getLogger(NoOpOwnershipParser::class.java)
    }


    constructor(){
      log.info("Using "+ NoOpOwnershipParser::class+" as a repository Ownership parser")
    }

    override fun computeOwnershipFor(repositoryFullName: String, lastCommitNumber: Int): String {
        return "N/A"
    }
}