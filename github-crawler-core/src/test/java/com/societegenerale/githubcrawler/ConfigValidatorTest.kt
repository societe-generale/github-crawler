package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.remote.NoReachableRepositories
import com.societegenerale.githubcrawler.remote.RemoteSourceControl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.slf4j.LoggerFactory


class ConfigValidatorTest {

    val log = LoggerFactory.getLogger(this.javaClass)

    val mockRemoteSourceControl = mock(RemoteSourceControl::class.java)

    @Test
    fun shouldNotHaveEmptyGitHubUrl() {

        val configValidator = ConfigValidator(GitHubCrawlerProperties(SourceControlConfig(url = "",organizationName="notEmpty")),mockRemoteSourceControl)

        assertThat(configValidator.getValidationErrors()).containsOnly("source-control.url can't be empty");

    }

    @Test
    fun shouldNotHaveEmptyOrganization() {

        val configValidator = ConfigValidator(GitHubCrawlerProperties(SourceControlConfig(url = "notEmpty",organizationName="")),mockRemoteSourceControl)

        assertThat(configValidator.getValidationErrors()).containsOnly("organization can't be empty");

    }

    @Test
    fun shouldLogProperMessageIfNotAbleToHitAPI() {

        val configValidator = ConfigValidator(GitHubCrawlerProperties(SourceControlConfig(url = "someIncorrectURL",organizationName="someOrg")),mockRemoteSourceControl)

        `when`(mockRemoteSourceControl.validateRemoteConfig("someOrg")).thenThrow(NoReachableRepositories("problem !"))

        try {
            val validationErrors = configValidator.getValidationErrors()

            assertThat(validationErrors).hasSize(1);

            assertThat(validationErrors[0]).startsWith("Not able to fetch repositories from the organization someOrg on URL someIncorrectURL");

        }
        catch(e : Exception){
            log.info("problem during test",e)
            fail("test failed")
        }



    }



}
