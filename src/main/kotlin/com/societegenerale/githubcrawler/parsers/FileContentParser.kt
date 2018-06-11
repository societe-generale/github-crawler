package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition


interface FileContentParser {

    fun getNameInConfig(): String

    fun parseFileContentForIndicator(fileContent: String, pathToFileToGetIndicatorsFrom:String, kpi: IndicatorDefinition): Map<String, String>


    companion object {
        val NOT_FOUND = "not found"
    }
}
