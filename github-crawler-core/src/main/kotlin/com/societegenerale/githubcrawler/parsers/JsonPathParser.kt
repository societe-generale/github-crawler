package com.societegenerale.githubcrawler.parsers

import com.jayway.jsonpath.JsonPath.read
import com.jayway.jsonpath.PathNotFoundException
import com.societegenerale.githubcrawler.IndicatorDefinition
import net.minidev.json.JSONArray
import org.slf4j.LoggerFactory

/**
 * Provided with a given jsonPath, will parse the Json file (a NPM package.json for example), and will return the value associated to the element, if found.
 *
 * @note : implementation uses https://github.com/json-path/JsonPath , so refer there if you have doubts on how to specify the jsonP
 */
class JsonPathParser : FileContentParser {

    companion object {
        const val FIND_JSONPATH_VALUE_METHOD = "findValueForJsonPath"
        const val JSON_PATH = "jsonPath"
        const val NOT_FOUND = "not found"
    }

    val log = LoggerFactory.getLogger(this.javaClass)

    override fun getNameInConfig(): String {
        return FIND_JSONPATH_VALUE_METHOD
    }

    override fun parseFileContentForIndicator(fileContent: String, pathToFileToGetIndicatorsFrom: String, kpi: IndicatorDefinition): Map<String, String> {

        return internalParseFileContentForIndicator(fileContent, kpi)
    }

    private fun internalParseFileContentForIndicator(fileContent: String, kpi: IndicatorDefinition): Map<String, String> {

        val indicator = HashMap<String, String>()

        if(kpi.params[JSON_PATH] == null){
            throw IllegalStateException("please define a '$JSON_PATH' attribute in your config for the indicator '${kpi.name}'")
        }

        indicator.put(kpi.name, findValueAccordingToPath(fileContent, kpi.params[JSON_PATH]!!))

        return indicator
    }

    private fun findValueAccordingToPath(fileContent: String, path : String) : String{

        return try {
            val result = read(fileContent, path) as Any

            if (result is String) {
                result
            } else if (result is JSONArray && !result.isEmpty()) {
                result[0] as String
            } else {
                NOT_FOUND
            }
        } catch (e: PathNotFoundException) {
            NOT_FOUND
        }

    }


}

