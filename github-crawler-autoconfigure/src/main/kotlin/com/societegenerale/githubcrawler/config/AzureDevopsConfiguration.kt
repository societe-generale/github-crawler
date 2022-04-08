package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.remote.RemoteAzureDevopsImpl
import com.societegenerale.githubcrawler.remote.RemoteAzureDevopsImpl.Companion.AZURE_DEVOPS_SEARCH_URL
import com.societegenerale.githubcrawler.remote.RemoteAzureDevopsImpl.Companion.AZURE_DEVOPS_URL
import com.societegenerale.githubcrawler.remote.RemoteSourceControl
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "crawler.source-control", name = ["type"], havingValue = "AZURE_DEVOPS")
open class AzureDevopsConfiguration {

    val log = LoggerFactory.getLogger(this.javaClass)

    @Bean
    open fun remoteAzureDevops(gitHubCrawlerProperties: GitHubCrawlerProperties): RemoteSourceControl {

        val targetUrl:String
        val searchUrl:String


        if(gitHubCrawlerProperties.sourceControl.url.isEmpty()){
            targetUrl = AZURE_DEVOPS_URL
            searchUrl = AZURE_DEVOPS_SEARCH_URL
        }
        else{
            targetUrl=gitHubCrawlerProperties.sourceControl.url
            searchUrl=gitHubCrawlerProperties.sourceControl.url
        }

        log.info("URL for AzureDevops : $targetUrl")

        return RemoteAzureDevopsImpl(targetUrl,
                                     searchUrl,
                                     gitHubCrawlerProperties.sourceControl.organizationName,
                                     gitHubCrawlerProperties.sourceControl.apiToken)
    }

}
