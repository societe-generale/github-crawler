package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Repository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

        val searchItemPaths=analyzedRepository.searchResults.get(searchNameToOutput)

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
