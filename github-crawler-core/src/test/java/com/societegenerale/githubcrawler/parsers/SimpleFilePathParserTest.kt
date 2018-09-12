package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition
import com.societegenerale.githubcrawler.parsers.SimpleFilePathParser.Companion.FILE_PATH_INFO
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SimpleFilePathParserTest {

    internal val indicatorName = "actualFilePathToSomeInterestingFile"
    internal var simpleFilePathIndicator = IndicatorDefinition(indicatorName,FILE_PATH_INFO)
    internal var fileContentParser = SimpleFilePathParser()


    @Test
    fun returnsTheFilePathAsIndicatorValue() {

        val indicatorsFound = fileContentParser.parseFileContentForIndicator("some content that we don't really care of",
                                                                                "path/to/the/interesting/file.txt",
                                                                                simpleFilePathIndicator)

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("path/to/the/interesting/file.txt")
    }



}