package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition
import com.societegenerale.githubcrawler.parsers.SimpleFilePathParser.Companion.FILE_PATH_INFO
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class SimpleFilePathParserTest {

    internal val indicatorName = "actualFilePathToSomeInterestingFile"
    internal var simpleFilePathIndicator = IndicatorDefinition()
    internal var fileContentParser = SimpleFilePathParser()

    @Before
    fun setup() {

        simpleFilePathIndicator.name = indicatorName
        simpleFilePathIndicator.method = FILE_PATH_INFO
    }


    @Test
    fun returnsTheFilePathAsIndicatorValue() {

        val indicatorsFound = fileContentParser.parseFileContentForIndicator("some content that we don't really care of",
                                                                                "path/to/the/interesting/file.txt",
                                                                                simpleFilePathIndicator)

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("path/to/the/interesting/file.txt")
    }



}