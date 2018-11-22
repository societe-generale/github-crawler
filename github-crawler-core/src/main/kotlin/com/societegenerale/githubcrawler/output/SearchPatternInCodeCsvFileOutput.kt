package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This is a specific output, to be used mainly in combination with PathsForHitsOnRepoSearch task.
 *
 * use case is like this :
 * - you need to find all files that contain a certain pattern, so you configure PathsForHitsOnRepoSearch, with a given name, say <i>mySearch</i>
 * - by configuring SearchPatternInCodeCsvFileOutput as an output (with <i>mySearch</i> as the name of the indicator to output), it will take the paths found, and generate a csv file, with one location per line, with <i>repositoryFullName;location</i>
 *
 * So if a repository contains 2 files with the pattern, 2 records will be generated.
 *
 * This simple output format enables you to easily reprocess it in a text editor or Excel, before using it for another purpose (as an input for CI-Droid for example)
 *
 */
class SearchPatternInCodeCsvFileOutput (private val searchNameToOutput: String) : CsvFileOutput(searchNameToOutput)  {

    override val log: Logger
        get() = LoggerFactory.getLogger(this.javaClass)


    override fun getCsvHeaderFrom(initParam: Any): String {
        return "repositoryFullName;location"
    }

    override fun getPrefix(): String {
        return "SearchPatternInCode_"
    }

    override fun outputRepository(analyzedRepository: Repository): StringBuilder {

        val sb=StringBuilder()

        val searchItemPaths=analyzedRepository.miscTasksResults[Branch(analyzedRepository.defaultBranch)].orEmpty().get(searchNameToOutput)

        if(searchItemPaths is List<*>){

            for(itemPathFound in searchItemPaths) {
                sb.append(analyzedRepository.fullName).append(";")
                sb.append(itemPathFound).append(";")
                sb.append(System.lineSeparator());
            }

        }
        else {

            sb.append(analyzedRepository.fullName).append(";")
            sb.append("unable to parse item path").append(";")
            sb.append(System.lineSeparator())

        }
        return sb
    }

}
