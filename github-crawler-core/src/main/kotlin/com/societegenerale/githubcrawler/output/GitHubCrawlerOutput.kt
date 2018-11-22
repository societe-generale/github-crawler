package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Repository
import java.io.IOException


/**
 * Once a repository has been analyzed, it will passed to one or several GitHubCrawlerOutput(s).
 *
 * GitHubCrawlerOutput are activated at start-up time, based on properties in config file.
 *
 * @see GitHubCrawlerOutputConfig
 *
 */
interface GitHubCrawlerOutput {

    @Throws(IOException::class)
    fun output(analyzedRepository: Repository)


    /**
     * Some implementations may need a finalize method after all elements have been written in output
     */
    @Throws(IOException::class)
    @JvmDefault
    fun finalizeOutput(){

    }
}
