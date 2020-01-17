package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.remote.RemoteGitLabImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("gitLab")
open class GitLabConfiguration {

    @Bean
    open fun remoteGitLab(gitHubCrawlerProperties: GitHubCrawlerProperties): RemoteGitHub {

        return RemoteGitLabImpl(gitHubCrawlerProperties.githubConfig.apiUrl,gitHubCrawlerProperties.githubConfig.oauthToken)
    }

}
