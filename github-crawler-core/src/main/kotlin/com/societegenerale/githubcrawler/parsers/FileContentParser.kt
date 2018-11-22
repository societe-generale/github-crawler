package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition


/**
 * A parser that takes a given file content as a parameter, and returns the values that we're looking for. Implementation will typically define a
 * couple of configuration parameter that are required to perform its duty. See how [PomXmlParserForDependencyVersion] defines and uses [PomXmlParserForDependencyVersion.ARTIFACT_ID] for instance.
 *
 * Implementing classes need to be instantiated explicitly in config to be available at runtime - see [GitHubCrawlerParserConfig].
 * Once instantiated, all FileContentParser classes will be place in map, with the value returned by getNameInConfig as the key : by referencing that same name in config (through the <i>type</i> attribute), you'll be able to activate the parser on the file you're interested in.
 *
 * @note : implementations have to handle internally any kind of runtime exception that would happen during parsing, otherwise crawler will crash.
 *
 * @see GitHubCrawlerParserConfig
 *
 */
interface FileContentParser {

    /**
     * @return the parser's ID that we need to use in config to refer to it. Typically, a String describing the parser type.
     * At application startup, beans implementing the interface will be instantiated and stored in a Map as a value, with getNameInConfig value as the key.
     */
    fun getNameInConfig(): String

    /**
     * @param fileContent the file content that needs to be parsed
     * @param pathToFileToGetIndicatorsFrom the path to the file. Rarely used, but sometimes useful, like in [SimpleFilePathParser]
     * @param kpi the indicator we need to find in the file
     * @return a Map with the indicator name as key, and the value found as the value. Most of the time, the map will have a single entry.
     */
    fun parseFileContentForIndicator(fileContent: String, pathToFileToGetIndicatorsFrom:String, kpi: IndicatorDefinition): Map<String, String>


    companion object {
        val NOT_FOUND = "not found"
    }
}
