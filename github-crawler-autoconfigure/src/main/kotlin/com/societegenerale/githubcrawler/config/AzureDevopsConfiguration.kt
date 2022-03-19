package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.remote.RemoteAzureDevopsImpl
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "github-crawler.githubConfig", name = ["type"], havingValue = "AZURE_DEVOPS")
open class AzureDevopsConfiguration {

    @Bean
    open fun remoteAzureDevops(gitHubCrawlerProperties: GitHubCrawlerProperties): RemoteGitHub {

        return RemoteAzureDevopsImpl(gitHubCrawlerProperties.githubConfig.organizationName,gitHubCrawlerProperties.githubConfig.oauthToken)
    }

}
