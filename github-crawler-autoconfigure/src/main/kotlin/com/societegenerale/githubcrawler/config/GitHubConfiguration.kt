package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.remote.RemoteGitHubImpl
import com.societegenerale.githubcrawler.remote.RemoteSourceControl
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "crawler.source-control", name = ["type"], havingValue = "GITHUB")
open class GitHubConfiguration {

    @Bean
    open fun remoteGitHub(gitHubCrawlerProperties: GitHubCrawlerProperties): RemoteSourceControl {

        return RemoteGitHubImpl(gitHubCrawlerProperties.sourceControl.url,
                                gitHubCrawlerProperties.sourceControl.crawlUsersRepoInsteadOfOrgasRepos,
                                gitHubCrawlerProperties.sourceControl.apiToken)
    }

}
