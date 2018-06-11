package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.IOException
import java.io.StringReader
import java.util.*
import javax.xml.parsers.SAXParserFactory

@Component
class PomXmlParserForDependencyVersion : FileContentParser {

    companion object {
        const val FIND_DEPENDENCY_VERSION_IN_XML_METHOD = "findDependencyVersionInXml"
        const val ARTIFACT_ID = "artifactId"
        const val VERSION = "version"
        const val PROPERTIES = "properties"
    }

    val log = LoggerFactory.getLogger(this.javaClass)

    override fun getNameInConfig(): String {
        return FIND_DEPENDENCY_VERSION_IN_XML_METHOD
    }

    override fun parseFileContentForIndicator(fileContent: String, pathToFileToGetIndicatorsFrom: String, kpi: IndicatorDefinition): Map<String, String> {

        return internalParseFileContentForIndicator(fileContent, kpi)
    }

    @Suppress("TooGenericExceptionThrown") // SAX parser may
    private fun internalParseFileContentForIndicator(fileContent: String, kpi: IndicatorDefinition): Map<String, String> {

        val indicator = HashMap<String, String>()

        val pomDependencyHandler = PomDependencyHandler(kpi,kpi.params[ARTIFACT_ID],fileContent)

        try {

            val factory = SAXParserFactory.newInstance()
            val saxParser = factory.newSAXParser()

            saxParser.parse(InputSource(StringReader(fileContent)), pomDependencyHandler)

            indicator.putAll(pomDependencyHandler.indicator)
        } catch (e: BreakParsingException) {
            indicator.putAll(pomDependencyHandler.indicator)
        }

        catch (e: SAXException) {
            log.warn("problem while parsing", e)
            indicator.put(kpi.name, "error while processing " + e.message)
        }

        if (indicator.isEmpty()) {
            indicator.put(kpi.name, "not found")
        }

        return indicator
    }

    private class BreakParsingException : SAXException()

    private class PomDependencyHandler (val kpi: IndicatorDefinition, val expectedArtifactId : String?, val fileContent : String): DefaultHandler() {

        val log = LoggerFactory.getLogger(this.javaClass)

        internal var inAnyArtifactElement = false

        internal var inExpectedArtifactElement = false

        internal var inVersionForExpectedArtifactElement = false

        internal var versionHasBeenFound = false

        internal var hasSeenExpectedArtifactElement = false

        val indicator = HashMap<String, String>()

        @Throws(SAXException::class)
        override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes?) {

            if (entersArtifactElement(qName)) {
                inAnyArtifactElement = true
            }

            if (entersExpectedElementVersionElement(qName)) {
                inVersionForExpectedArtifactElement = true
            }
        }

        private fun entersExpectedElementVersionElement(qName: String) = inExpectedArtifactElement && qName.equals(VERSION, ignoreCase = true)

        private fun entersArtifactElement(qName: String) = qName.equals(ARTIFACT_ID, ignoreCase = true)

        @Throws(SAXException::class)
        override fun endElement(uri: String?, localName: String?, qName: String?) {
            if (exitsArtifactElement(qName)) {
                inAnyArtifactElement = false

                if (!hasSeenExpectedArtifactElement) {
                    indicator.put(kpi.name, "not found")
                }
                else if (!versionHasBeenFound) {
                    indicator.put(kpi.name, "artifact found, but not the version")
                }
            }

            if (exitsExpectedElementVersionElement(qName)) {
                inVersionForExpectedArtifactElement = false
            }
        }

        private fun exitsArtifactElement(qName: String?) = inAnyArtifactElement && qName.equals(ARTIFACT_ID, ignoreCase = true)

        private fun exitsExpectedElementVersionElement(qName: String?) =
                inVersionForExpectedArtifactElement && qName!!.equals(VERSION, ignoreCase = true)


        @Throws(SAXException::class)
        override fun characters(ch: CharArray?, start: Int, length: Int) {

            if (inAnyArtifactElement) {
                val actualArtifactId = String(ch!!, start, length)
                log.debug("artifactId is {}", actualArtifactId)

                inExpectedArtifactElement = (actualArtifactId == expectedArtifactId)

                if (inExpectedArtifactElement) {
                    hasSeenExpectedArtifactElement = true
                }

            }

            if (inVersionForExpectedArtifactElement) {
                var version = String(ch!!, start, length)
                log.debug("\tfound version is $version")

                if (version.startsWith("\${")) {
                    version = findPropertyValueIn(fileContent, version)
                }

                indicator.put(kpi.name, version)

                versionHasBeenFound = true

                throw BreakParsingException()
            }

        }

        private fun findPropertyValueIn(fileContent: String, propertyName: String): String {

            // removing prefix / suffix ${...}
            val propertyNameWithoutPrefixSuffix = propertyName.substring(2, propertyName.length - 1)

            var versionValuesFromProperties : String

            val versionValueHandler = VersionInPropertiesHandler(propertyNameWithoutPrefixSuffix)

            try {

                val saxParser = SAXParserFactory.newInstance().newSAXParser()

                saxParser.parse(InputSource(StringReader(fileContent)), versionValueHandler)

                //if exception is not thronw, it means value hasn't been found
                versionValuesFromProperties= "$propertyName not found in properties section"
            }
            catch( e : BreakParsingException){
                versionValuesFromProperties=versionValueHandler.versionValuesFromProperties
            }
            catch (e: IOException) {
                throw SAXException()
            }

            return versionValuesFromProperties
        }

    }

    private class VersionInPropertiesHandler(val propertyNameWithoutPrefixSuffix:String): DefaultHandler(){

        internal var inProperties = false

        internal var inExpectedProperty = false

        val log = LoggerFactory.getLogger(this.javaClass)

        var versionValuesFromProperties:String ="not found"

        @Throws(SAXException::class)
        override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes?) {

            if (entersPropertiesSection(qName)) {
                inProperties = true
            }

            if (entersExpectedPropertyElement(qName)) {
                inExpectedProperty = true
            }
        }

        private fun entersExpectedPropertyElement(qName: String) = inProperties && qName.equals(propertyNameWithoutPrefixSuffix, ignoreCase = true)

        @Throws(SAXException::class)
        override fun endElement(uri: String?, localName: String?, qName: String?) {
            if (exitsPropertiesSection(qName)) {
                inProperties = false
            }

        }

        private fun exitsPropertiesSection(qName: String?) = inProperties && entersPropertiesSection(qName!!)

        private fun entersPropertiesSection(qName: String) = qName.equals(PROPERTIES, ignoreCase = true)

        @Throws(SAXException::class)
        override fun characters(ch: CharArray?, start: Int, length: Int) {

            if (insideExpectedPropertyElement()) {
                versionValuesFromProperties = String(ch!!, start, length)
                log.debug("property value is {}", versionValuesFromProperties)

                throw BreakParsingException()
            }

        }

        private fun insideExpectedPropertyElement() = inProperties && inExpectedProperty

    }

}

