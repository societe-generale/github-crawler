package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.IndicatorDefinition
import com.societegenerale.githubcrawler.parsers.JsonPathParser.Companion.JSON_PATH
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.util.ResourceUtils


class JsonPathParserTest {

    val npmDependencyVersionParser = JsonPathParser()

    val sampleNpmPackageJsonFile = FileUtils.readFileToString(ResourceUtils.getFile("classpath:sampleNpmPackage.json"), "UTF-8")

    @Test
    fun should_find_angular_version_when_defined() {

        val angularCoreVersionIndicator = IndicatorDefinition("angularCoreVersion",
                                                     JsonPathParser.FIND_JSONPATH_VALUE_METHOD,
                                                     mapOf(Pair(JSON_PATH, "dependencies.@angular/core")))

        val result = npmDependencyVersionParser.parseFileContentForIndicator(sampleNpmPackageJsonFile, "", angularCoreVersionIndicator)

        assertThat(result.get("angularCoreVersion")).isEqualTo("^7.0.4")
    }

    @Test
    fun should_return_NOT_FOUND_when_value_not_found() {

        val someLibVersionIndicator = IndicatorDefinition("someLib",
                JsonPathParser.FIND_JSONPATH_VALUE_METHOD,
                mapOf(Pair(JSON_PATH, "dependencies.@someLib")))

        val result = npmDependencyVersionParser.parseFileContentForIndicator(sampleNpmPackageJsonFile, "", someLibVersionIndicator)

        assertThat(result.get("someLib")).isEqualTo("not found")
    }

    @Test
        /**
         * see https://github.com/societe-generale/github-crawler/issues/115
         */
    fun whenResultIsAnArray_returnsTheFirstElement() {

        val sampleComposerLockFile = FileUtils.readFileToString(ResourceUtils.getFile("classpath:sampleComposer.lock"), "UTF-8")

        val someNameIndicator = IndicatorDefinition("somePackageName",
            JsonPathParser.FIND_JSONPATH_VALUE_METHOD,
            mapOf(Pair(JSON_PATH, "\$.packages[?(@.name == 'sulu/sulu')].name")))

        val result = npmDependencyVersionParser.parseFileContentForIndicator(sampleComposerLockFile, "", someNameIndicator)

        assertThat(result.get("somePackageName")).isEqualTo("sulu/sulu")
    }

    @Test
    fun throwsIllegalStateExceptionWhenNotConfiguredCorrectly(){
        val someLibVersionIndicator = IndicatorDefinition("someLib",
            JsonPathParser.FIND_JSONPATH_VALUE_METHOD,
            //wrong attribute configured !!
            mapOf(Pair("propertyName" , "dependencies.@someLib")))

        assertThrows<IllegalStateException> {
            npmDependencyVersionParser.parseFileContentForIndicator(sampleNpmPackageJsonFile, "", someLibVersionIndicator)
        }

    }

}