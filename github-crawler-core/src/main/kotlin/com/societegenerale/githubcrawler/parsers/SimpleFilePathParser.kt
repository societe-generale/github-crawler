package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition
import org.slf4j.LoggerFactory
import java.util.*


/**
 * This parser is a bit special - it ignores the file content, and just returns the file path as an indicator value.
 * This can be useful when we need to list the files path, for example with a CI-droid bulk updates output.
 */
class SimpleFilePathParser : FileContentParser {

    companion object {
        const val FILE_PATH_INFO = "findFilePath"
    }

    val log = LoggerFactory.getLogger(this.javaClass)

    override fun getNameInConfig(): String {
        return FILE_PATH_INFO
    }

    override fun parseFileContentForIndicator(fileContent: String, pathToFileToGetIndicatorsFrom:String, kpi: IndicatorDefinition): Map<String, String> {
         return internalParseFileContentForIndicator(pathToFileToGetIndicatorsFrom, kpi)
    }

    fun internalParseFileContentForIndicator(pathToFileToGetIndicatorsFrom:String, kpi: IndicatorDefinition): Map<String, String> {

        val indicator = HashMap<String, String>()
        indicator.put(kpi.name, pathToFileToGetIndicatorsFrom)

        return indicator
    }

}