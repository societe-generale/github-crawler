package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern


class FirstMatchingRegexpParser : FileContentParser {

    companion object {
        const val FIND_FIRST_VALUE_WITH_REGEXP_CAPTURE_METHOD = "findFirstValueWithRegexpCapture"
        const val PATTERN = "pattern"
    }

    val log = LoggerFactory.getLogger(this.javaClass)

    override fun getNameInConfig(): String {
        return FIND_FIRST_VALUE_WITH_REGEXP_CAPTURE_METHOD
    }

    override fun parseFileContentForIndicator(fileContent: String, pathToFileToGetIndicatorsFrom:String, kpi: IndicatorDefinition): Map<String, String> {

        return internalParseFileContentForIndicator(fileContent,kpi)
    }

    fun internalParseFileContentForIndicator(fileContent: String,kpi: IndicatorDefinition): Map<String, String> {

        val patternFromConfig = kpi.params[PATTERN]

        val pattern = Pattern.compile(patternFromConfig)
        val matcher = pattern.matcher(fileContent)

        var foundValue = FileContentParser.NOT_FOUND

        if(matcher.groupCount()!=1){
            log.warn("double check the regex config, it needs to have a capturing group, ie something between parenthesis")
            foundValue="issue in config, check logs"
        }
        else if(matcher.find()){
            foundValue = matcher.group(1)

            if (log.isDebugEnabled()) {
                log.debug("file content matches the configured regex : {}", foundValue !== FileContentParser.NOT_FOUND)
            }

        }

        val indicator = HashMap<String, String>()
        indicator.put(kpi.name, foundValue)

        return indicator
    }

}