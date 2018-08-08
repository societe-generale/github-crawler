package com.societegenerale.githubcrawler.config

import com.societegenerale.githubcrawler.GitHubCrawler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import java.io.IOException

@SpringBootApplication
open class GitHubCrawlerApplication : CommandLineRunner {

    @Autowired
    private val crawler: GitHubCrawler? = null


    @Throws(IOException::class)
    override fun run(vararg args: String) {

        crawler!!.crawl()


    }

    @Suppress("SpreadOperator") //no performance impact given the number of values in args..
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder(GitHubCrawlerApplication::class.java).web(false).run(*args)
        }
    }


}
