package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.remote.RemoteGitLabImpl
import com.societegenerale.githubcrawler.remote.RemoteSourceControl
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "crawler.source-control", name = ["type"], havingValue = "GITLAB")
open class GitLabConfiguration {

    @Bean
    open fun remoteGitLab(gitHubCrawlerProperties: GitHubCrawlerProperties): RemoteSourceControl {

        return RemoteGitLabImpl(gitHubCrawlerProperties.sourceControl.url,gitHubCrawlerProperties.sourceControl.apiToken)
    }

}
