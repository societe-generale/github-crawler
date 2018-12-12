package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition
import com.societegenerale.githubcrawler.parsers.NpmDependencyVersionParser.Companion.JSON_PATH
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.util.ResourceUtils


class NpmDependencyVersionParserTest {

    val npmDependencyVersionParser = NpmDependencyVersionParser()

    val sampleNpmPackageJsonFile = FileUtils.readFileToString(ResourceUtils.getFile("classpath:sampleNpmPackage.json"), "UTF-8")

    @Test
    fun should_find_angular_version_when_defined() {

        var angularCoreVersionIndicator = IndicatorDefinition("angularCoreVersion",
                                                     NpmDependencyVersionParser.FIND_NPM_PACKAGE_VERSION_METHOD,
                                                     mapOf(Pair(JSON_PATH, "dependencies.@angular/core")))

        val result = npmDependencyVersionParser.parseFileContentForIndicator(sampleNpmPackageJsonFile, "", angularCoreVersionIndicator)

        assertThat(result.get("angularCoreVersion")).isEqualTo("^7.0.4")
    }

    @Test
    fun should_return_NOT_FOUND_when_value_not_found() {

        var someLibVersionIndicator = IndicatorDefinition("someLib",
                NpmDependencyVersionParser.FIND_NPM_PACKAGE_VERSION_METHOD,
                mapOf(Pair(JSON_PATH, "dependencies.@someLib")))

        val result = npmDependencyVersionParser.parseFileContentForIndicator(sampleNpmPackageJsonFile, "", someLibVersionIndicator)

        assertThat(result.get("someLib")).isEqualTo("not found")
    }

}