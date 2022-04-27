package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.remote.RemoteGitHubImpl
import com.societegenerale.githubcrawler.remote.RemoteSourceControl
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "crawler.source-control", name = ["type"], havingValue = "GITHUB")
open class GitHubConfiguration {

    val log = LoggerFactory.getLogger(this.javaClass)

    @Bean
    open fun remoteGitHub(gitHubCrawlerProperties: GitHubCrawlerProperties): RemoteSourceControl {

        val targetUrl:String

        if(gitHubCrawlerProperties.sourceControl.url.isEmpty()){
            log.info("'source-control.url' is not provided - defaulting to "+RemoteGitHubImpl.GITHUB_URL)
            targetUrl = RemoteGitHubImpl.GITHUB_URL
        }
        else{
            targetUrl=gitHubCrawlerProperties.sourceControl.url
        }

        return RemoteGitHubImpl(targetUrl,
                                gitHubCrawlerProperties.sourceControl.crawlUsersRepoInsteadOfOrgasRepos,
                                gitHubCrawlerProperties.sourceControl.apiToken)
    }

}
