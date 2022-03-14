package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition
import com.societegenerale.githubcrawler.parsers.CountXmlElementsParser.Companion.XPATH_TO_MATCH
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.util.ResourceUtils

class CountXmlElementsParserTest {

    val countXmlElementsParser = CountXmlElementsParser()

    @Test
    fun should_count_elements_matching_the_xpath() {

        var nbPropertiesIndicator = IndicatorDefinition("nbProperties", CountXmlElementsParser.COUNT_XML_ELEMENTS_METHOD, mapOf(Pair(XPATH_TO_MATCH, "//*[local-name()='project']/*[local-name()='properties']/*")))

        val result = countXmlElementsParser.parseFileContentForIndicator(FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_pom.xml"), "UTF-8"), "", nbPropertiesIndicator)

        assertThat(result.get("nbProperties")).isEqualTo("5")
    }

    @Test
    fun should_report_0_if_no_match() {

        var nbModulesIndicator = IndicatorDefinition("nbModules", CountXmlElementsParser.COUNT_XML_ELEMENTS_METHOD, mapOf(Pair(XPATH_TO_MATCH, "//*[local-name()='project']/*[local-name()='modules']/*")))

        val result = countXmlElementsParser.parseFileContentForIndicator(FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_pom.xml"), "UTF-8"), "", nbModulesIndicator)

        assertThat(result.get("nbModules")).isEqualTo("0")
    }

    @Test
    fun should_catch_parsing_exception_internally() {

        var nbModulesIndicator = IndicatorDefinition("nbModules", CountXmlElementsParser.COUNT_XML_ELEMENTS_METHOD, mapOf(Pair(XPATH_TO_MATCH, "project/modules")))

        val result = countXmlElementsParser.parseFileContentForIndicator(FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_pom_with_namespace_issue.xml"), "UTF-8"), "", nbModulesIndicator)

        assertThat(result.get("nbModules")).isEqualTo("issue while parsing the file")
    }

}