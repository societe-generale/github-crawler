package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Repository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CIdroidReadyCsvFileOutput(val indicatorsToOutput: List<String>) : CsvFileOutput(indicatorsToOutput) {

    override val log: Logger
        get() = LoggerFactory.getLogger(this.javaClass)

    override fun getCsvHeaderFrom(initParam : Any): String {
        return "repositoryFullName;branchName;" + (initParam as List<*>).joinToString(separator = ";")
    }

    override fun getPrefix(): String {
        return "CIdroidReadyContent_"
    }

    override fun outputRepository(analyzedRepository: Repository): StringBuilder {

        val sb = StringBuilder()

        for ((branch, actualIndicators) in analyzedRepository.indicators) {
            sb.append(analyzedRepository.fullName).append(";")
            sb.append(branch.name).append(";")

            for (indicatorToOutput in indicatorsToOutput) {
                sb.append(actualIndicators.get(indicatorToOutput) ?: "N/A").append(";")
            }

            sb.append(System.lineSeparator());
        }

        return sb
    }
}
