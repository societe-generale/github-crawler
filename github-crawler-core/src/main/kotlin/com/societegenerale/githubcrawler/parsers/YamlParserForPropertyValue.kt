package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.scanner.ScannerException

@Component
class YamlParserForPropertyValue : FileContentParser {

    companion object {
        const val FIND_PROPERTY_VALUE_IN_YAML = "findPropertyValueInYamlFile"
        const val PROPERTY_NAME = "propertyName"
    }

    override fun getNameInConfig(): String {
        return FIND_PROPERTY_VALUE_IN_YAML
    }


    val log = LoggerFactory.getLogger(this.javaClass)

    override fun parseFileContentForIndicator(fileContent: String, pathToFileToGetIndicatorsFrom: String, kpi: IndicatorDefinition): Map<String, String> {

        return internalParseFileContentForIndicator(fileContent, kpi)
    }

    fun internalParseFileContentForIndicator(fileContent: String, kpi: IndicatorDefinition): Map<String, String> {

        val yamlDocuments = Yaml().loadAll(fileContent)

        val result = HashMap<String, String>()

        //TODO write a config validation rule making sure that property name is always provided
        val propertyName = kpi.params.get(PROPERTY_NAME)!!


        try {

            yamlDocuments.forEach {

                val yamlDocument = it as Map<String, Any>

                try {

                    result.put(kpi.name, yamlDocument.get(propertyName)?.toString() ?: parseForNestedProperties(yamlDocument, propertyName))
                } catch (e: PrefixNotFoundInDocumentException) {
                    //do nothing, iterate to next document if any
                }

                if (result.isNotEmpty()) {
                    //returning the result found in first document
                    return result;
                }
            }
        } catch (e: ScannerException) {
            log.warn("problem while parsing yaml file", e)
            result.put(kpi.name, "issue while parsing " + e.message)
        }

        return result
    }

    private fun parseForNestedProperties(content: Map<String, Any?>, propertyName: String): String {

        val propertyNameComponents = propertyName.split(".")

        val propertyPrefix = propertyNameComponents[0]

        var matchingValue: Any? = null

        var nbOfProperties: Int = 0

        while (matchingValue == null && nbOfProperties <= propertyNameComponents.size) {

            //building the property name incrementally
            var tmpPropertyName = propertyNameComponents.subList(0, nbOfProperties).joinToString(".")

            matchingValue = content.get(tmpPropertyName)
            nbOfProperties++
        }

        //TODO refactor this part.. it probably doesn't work for all scenarios
        if (matchingValue == null) {
            throw PrefixNotFoundInDocumentException("can't find property 'starting with' or '' $propertyPrefix")
        }

        return when (matchingValue) {

            is String -> matchingValue

            is Boolean -> matchingValue.toString()

            is Map<*, *> -> {

                val recombinedPropertyNameWithoutFirstElement = propertyNameComponents.subList(1, propertyNameComponents.size).joinToString(".")

                //TODO see why explicit cast is required, while it should be implicit
                return parseForNestedProperties(matchingValue as Map<String, Any?>, recombinedPropertyNameWithoutFirstElement)

            }

            else -> throw ParsingException("don't know  how to process this " + matchingValue)

        }
    }

}

class PrefixNotFoundInDocumentException(message: String) : Throwable()

class ParsingException(message: String) : Throwable()
