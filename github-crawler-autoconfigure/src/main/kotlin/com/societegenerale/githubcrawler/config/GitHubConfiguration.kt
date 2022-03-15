package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.remote.RemoteGitHubImpl
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "github-crawler.githubConfig", name = ["type"], havingValue = "GITHUB")
open class GitHubConfiguration {

    @Bean
    open fun remoteGitHub(gitHubCrawlerProperties: GitHubCrawlerProperties): RemoteGitHub {

        return RemoteGitHubImpl(gitHubCrawlerProperties.githubConfig.apiUrl,gitHubCrawlerProperties.githubConfig.crawlUsersRepoInsteadOfOrgasRepos,gitHubCrawlerProperties.githubConfig.oauthToken)
    }

}
