package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.remote.NoReachableRepositories
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock


class ConfigValidatorTest {

    val mockRemoteGitHub = mock(RemoteGitHub::class.java)

    @Test
    fun shouldNotHaveEmptyGitHubUrl() {

        val configValidator = ConfigValidator(GitHubCrawlerProperties(),"","notEmpty",mockRemoteGitHub)

        assertThat(configValidator.getValidationErrors()).containsOnly("gitHub.url can't be empty");

    }

    @Test
    fun shouldNotHaveEmptyOrganization() {

        val configValidator = ConfigValidator(GitHubCrawlerProperties(),"notEmpty","",mockRemoteGitHub)

        assertThat(configValidator.getValidationErrors()).containsOnly("organization can't be empty");

    }

    @Test
    fun shouldLogProperMessageIfNotAbleToHitAPI() {

        val configValidator = ConfigValidator(GitHubCrawlerProperties(),"someIncorrectURL","someOrg",mockRemoteGitHub)

        `when`(mockRemoteGitHub.validateRemoteConfig("someOrg")).thenThrow(NoReachableRepositories("problem !",mock(Exception::class.java)))

        val validationErrors=configValidator.getValidationErrors()

        assertThat(validationErrors).hasSize(1);

        assertThat(validationErrors[0]).startsWith("Not able to fetch repositories from the organization someOrg on URL someIncorrectURL");

    }



}