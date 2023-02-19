package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.remote.RemoteBitBucketImpl
import com.societegenerale.githubcrawler.remote.RemoteSourceControl
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "crawler.source-control", name = ["type"], havingValue = "BITBUCKET")
open class BitBucketConfiguration {

    val log = LoggerFactory.getLogger(this.javaClass)

    @Bean
    open fun remoteBitBucket(gitHubCrawlerProperties: GitHubCrawlerProperties): RemoteSourceControl {

        return RemoteBitBucketImpl(gitHubCrawlerProperties.sourceControl.url,
                                gitHubCrawlerProperties.sourceControl.projectName,
                                gitHubCrawlerProperties.sourceControl.apiToken)
    }

}
