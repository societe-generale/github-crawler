package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Repository

class ConsoleOutput : GitHubCrawlerOutput {


    override fun output(analyzedRepository: Repository) {
        println(analyzedRepository)
    }
}
