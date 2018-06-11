package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Repository
import java.io.IOException


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
