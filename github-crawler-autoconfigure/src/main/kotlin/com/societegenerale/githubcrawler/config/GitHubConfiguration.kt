package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.remote.RemoteGitHubImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!gitLab")
open class GitHubConfiguration {

    @Bean
    open fun remoteGitHub(gitHubCrawlerProperties: GitHubCrawlerProperties): RemoteGitHub {

        return RemoteGitHubImpl(gitHubCrawlerProperties.githubConfig.apiUrl,gitHubCrawlerProperties.githubConfig.crawlUsersRepoInsteadOfOrgasRepos,gitHubCrawlerProperties.githubConfig.oauthToken)
    }

}
