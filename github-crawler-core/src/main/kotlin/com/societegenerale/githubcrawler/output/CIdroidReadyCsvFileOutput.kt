package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CIdroidReadyCsvFileOutput(val indicatorsToOutput: List<String>) : CsvFileOutput(indicatorsToOutput) {

    override val log: Logger
        get() = LoggerFactory.getLogger(this.javaClass)

    override fun getCsvHeaderFrom(initParam: Any): String {
        return "repositoryFullName;branchName;" + (initParam as List<*>).joinToString(separator = ";")
    }

    override fun getPrefix(): String {
        return "CIdroidReadyContent_"
    }

    override fun outputRepository(analyzedRepository: Repository): StringBuilder {

        val sb = StringBuilder()

        val allIndicators = getAllIndicatorsToOutput(analyzedRepository.indicators, analyzedRepository.miscTasksResults)

        for ((branch, actualIndicators) in allIndicators) {
            sb.append(analyzedRepository.fullName).append(";")
            sb.append(branch.name).append(";")

            for (indicatorToOutput in indicatorsToOutput) {
                sb.append(actualIndicators.getOrDefault(indicatorToOutput, "N/A")).append(";")
            }

            sb.append(System.lineSeparator());
        }

        return sb
    }

    private fun getAllIndicatorsToOutput(indicatorsFromFiles: Map<Branch, Map<String, Any>>, miscTasksResults: Map<Branch, Map<String, Any>>): Map<Branch, Map<String, Any>> {

        val result = LinkedHashMap<Branch, MutableMap<String, Any>>();

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
