package com.societegenerale.githubcrawler

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import java.io.IOException

@SpringBootApplication
open class GitHubCrawlerApplication : CommandLineRunner {

    @Autowired
    private val crawler: GitHubCrawler? = null

    val log = LoggerFactory.getLogger(this.javaClass)

    @Throws(IOException::class)
    override fun run(vararg args: String) {

        try {
            crawler!!.crawl()
        }
        catch(e : Exception ){
            log.error("problem while running github crawler",e)
        }

    }

    @Suppress("SpreadOperator") //no performance impact given the number of values in args..
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder(GitHubCrawlerApplication::class.java).web(WebApplicationType.NONE).run(*args)
        }
    }


}
