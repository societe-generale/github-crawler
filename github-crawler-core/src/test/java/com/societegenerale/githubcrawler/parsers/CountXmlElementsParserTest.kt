package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition
import com.societegenerale.githubcrawler.parsers.CountXmlElementsParser.Companion.XPATH_TO_MATCH
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.util.ResourceUtils

class CountXmlElementsParserTest {

    @Test
    fun should_count_elements_matching_the_xpath() {

        var nbModulesIndicator = IndicatorDefinition("nbProperties", CountXmlElementsParser.COUNT_XML_ELEMENTS_METHOD, mapOf(Pair(XPATH_TO_MATCH, "//*[local-name()='project']/*[local-name()='properties']/*")))
        val countXmlElementsParser = CountXmlElementsParser()

        val result = countXmlElementsParser.parseFileContentForIndicator(FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_pom.xml"), "UTF-8"), "", nbModulesIndicator)

        assertThat(result.get("nbProperties")).isEqualTo("5")
    }


}