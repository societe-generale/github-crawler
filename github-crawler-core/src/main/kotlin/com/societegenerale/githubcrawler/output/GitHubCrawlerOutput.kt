package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Branch
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
    fun finalizeOutput(){

    }

    fun getAllIndicatorsToOutput(indicatorsFromFiles: Map<Branch, Map<String, Any>>, miscTasksResults: Map<Branch, Map<String, Any>>): Map<Branch, Map<String, Any>> {

        val result = LinkedHashMap<Branch, MutableMap<String, Any>>()

        //TODO there's probably a better, more kotlin-esque way to merge the 2 maps..

        for ((key, value) in miscTasksResults) {
            result[key] = HashMap(value)

            if (indicatorsFromFiles.keys.contains(key)) {
                result[key]?.putAll(indicatorsFromFiles[key]!!)
            }
        }

        for ((key, value) in indicatorsFromFiles) {

            if (!result.containsKey(key)) {
                result[key] = HashMap(value)
            }
        }

        return result
    }
}
