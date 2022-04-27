package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.remote.NoReachableRepositories
import com.societegenerale.githubcrawler.remote.RemoteSourceControl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class ConfigValidatorTest {

    private val mockRemoteSourceControl = mock(RemoteSourceControl::class.java)

    @Test
    fun shouldNotHaveEmptyGitLabUrl() {

        val configValidator = ConfigValidator(GitHubCrawlerProperties(SourceControlConfig(type=SourceControlType.GITLAB, url = "",organizationName="notEmpty")),mockRemoteSourceControl)

        assertThat(configValidator.getValidationErrors()).containsOnly("source-control.url can't be empty");

    }

    @Test
    fun gitHubUrlCanBeEmpty() {

        val configValidator = ConfigValidator(GitHubCrawlerProperties(SourceControlConfig(type=SourceControlType.GITHUB, url = "",organizationName="notEmpty")),mockRemoteSourceControl)

        assertThat(configValidator.getValidationErrors()).isEmpty();

    }

    @Test
    fun shouldNotHaveEmptyOrganization() {

        val configValidator = ConfigValidator(GitHubCrawlerProperties(SourceControlConfig(url = "notEmpty",organizationName="")),mockRemoteSourceControl)

        assertThat(configValidator.getValidationErrors()).containsOnly("organization can't be empty");

    }

    @Test
    fun shouldLogProperMessageIfNotAbleToHitAPI() {

        val configValidator = ConfigValidator(GitHubCrawlerProperties(SourceControlConfig(url = "someIncorrectURL",organizationName="someOrg")),mockRemoteSourceControl)

        `when`(mockRemoteSourceControl.validateRemoteConfig("someOrg")).thenThrow(NoReachableRepositories("problem !", Exception()))

        val validationErrors = configValidator.getValidationErrors()

        assertThat(validationErrors).hasSize(1);

        assertThat(validationErrors[0]).startsWith("Not able to fetch repositories from the organization someOrg on URL someIncorrectURL");

    }
}
