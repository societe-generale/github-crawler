package com.societegenerale.githubcrawler.parsers

import com.jayway.jsonpath.JsonPath.read
import com.jayway.jsonpath.PathNotFoundException
import com.societegenerale.githubcrawler.IndicatorDefinition
import org.slf4j.LoggerFactory
import java.util.*

class NpmDependencyVersionParser : FileContentParser {

    companion object {
        const val FIND_NPM_PACKAGE_VERSION_METHOD = "findNpmDependencyVersion"
        const val JSON_PATH = "jsonPath"
        const val NOT_FOUND = "not found"
    }

    val log = LoggerFactory.getLogger(this.javaClass)

    override fun getNameInConfig(): String {
        return FIND_NPM_PACKAGE_VERSION_METHOD
    }

    override fun parseFileContentForIndicator(fileContent: String, pathToFileToGetIndicatorsFrom: String, kpi: IndicatorDefinition): Map<String, String> {

        return internalParseFileContentForIndicator(fileContent, kpi)
    }

    private fun internalParseFileContentForIndicator(fileContent: String, kpi: IndicatorDefinition): Map<String, String> {

        val indicator = HashMap<String, String>()

        val dependencyVersion: String = try {
            read(fileContent, kpi.params[JSON_PATH])
        } catch (e: PathNotFoundException) {
            NOT_FOUND
        }

        indicator.put(kpi.name, dependencyVersion)

        return indicator
    }


}

