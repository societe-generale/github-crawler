package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition
import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.xml.sax.InputSource
import java.io.StringReader

@Component
class CountXmlElementsParser : FileContentParser {

    companion object {
        const val COUNT_XML_ELEMENTS_METHOD = "countMatchingXmlElements"
        const val XPATH_TO_MATCH = "xpathToMatch"
        const val NOT_FOUND = "not found"
    }

    val log = LoggerFactory.getLogger(this.javaClass)

    override fun getNameInConfig(): String {
        return COUNT_XML_ELEMENTS_METHOD
    }

    override fun parseFileContentForIndicator(fileContent: String, pathToFileToGetIndicatorsFrom: String, kpi: IndicatorDefinition): Map<String, String> {

        return internalParseFileContentForIndicator(fileContent, kpi)
    }

    private fun internalParseFileContentForIndicator(fileContent: String, kpi: IndicatorDefinition): Map<String, String> {

        val originalDocument = parseStringIntoDocument(fileContent)

        return mapOf(Pair(kpi.name,originalDocument.selectNodes(kpi.params[XPATH_TO_MATCH]).size.toString()))
    }

    private fun parseStringIntoDocument(documentToProcess: String): Document {

        return SAXReader().read(InputSource(StringReader(documentToProcess)))

    }

}

