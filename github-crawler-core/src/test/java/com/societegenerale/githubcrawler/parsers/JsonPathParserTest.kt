package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition
import com.societegenerale.githubcrawler.parsers.JsonPathParser.Companion.JSON_PATH
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.util.ResourceUtils


class JsonPathParserTest {

    val npmDependencyVersionParser = JsonPathParser()

    val sampleNpmPackageJsonFile = FileUtils.readFileToString(ResourceUtils.getFile("classpath:sampleNpmPackage.json"), "UTF-8")

    @Test
    fun should_find_angular_version_when_defined() {

        var angularCoreVersionIndicator = IndicatorDefinition("angularCoreVersion",
                                                     JsonPathParser.FIND_NPM_PACKAGE_VERSION_METHOD,
                                                     mapOf(Pair(JSON_PATH, "dependencies.@angular/core")))

        val result = npmDependencyVersionParser.parseFileContentForIndicator(sampleNpmPackageJsonFile, "", angularCoreVersionIndicator)

        assertThat(result.get("angularCoreVersion")).isEqualTo("^7.0.4")
    }

    @Test
    fun should_return_NOT_FOUND_when_value_not_found() {

        var someLibVersionIndicator = IndicatorDefinition("someLib",
                JsonPathParser.FIND_NPM_PACKAGE_VERSION_METHOD,
                mapOf(Pair(JSON_PATH, "dependencies.@someLib")))

        val result = npmDependencyVersionParser.parseFileContentForIndicator(sampleNpmPackageJsonFile, "", someLibVersionIndicator)

        assertThat(result.get("someLib")).isEqualTo("not found")
    }

}