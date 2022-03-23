package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.remote.RemoteAzureDevopsImpl
import com.societegenerale.githubcrawler.remote.RemoteAzureDevopsImpl.Companion.AZURE_DEVOPS_URL
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "github-crawler.githubConfig", name = ["type"], havingValue = "AZURE_DEVOPS")
open class AzureDevopsConfiguration {

    val log = LoggerFactory.getLogger(this.javaClass)

    @Bean
    open fun remoteAzureDevops(gitHubCrawlerProperties: GitHubCrawlerProperties): RemoteGitHub {

        val targetUrl:String

        if(gitHubCrawlerProperties.githubConfig.apiUrl.isEmpty()){
            targetUrl=  AZURE_DEVOPS_URL
        }
        else{
            targetUrl=gitHubCrawlerProperties.githubConfig.apiUrl
        }

        log.info("URL for AzureDevops : $targetUrl")

        return RemoteAzureDevopsImpl(targetUrl,
                                     gitHubCrawlerProperties.githubConfig.organizationName,
                                     gitHubCrawlerProperties.githubConfig.oauthToken)
    }

}
