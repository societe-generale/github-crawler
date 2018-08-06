package com.societegenerale.githubcrawler

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class ConfigValidatorTest {


    @Test
    fun shouldNotHaveEmptyGitHubUrl() {

        val configValidator = ConfigValidator(GitHubCrawlerProperties(),"","notEmpty")

        assertThat(configValidator.getValidationErrors()).containsOnly("gitHub.url can't be empty");

    }

    @Test
    fun shouldNotHaveEmptyOrganization() {

        val configValidator = ConfigValidator(GitHubCrawlerProperties(),"notEmpty","")

        assertThat(configValidator.getValidationErrors()).containsOnly("organization can't be empty");

    }

}