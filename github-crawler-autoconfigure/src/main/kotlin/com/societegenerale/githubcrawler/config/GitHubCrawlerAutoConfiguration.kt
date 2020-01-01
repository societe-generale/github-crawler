package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.ConfigValidator
import com.societegenerale.githubcrawler.GitHubCrawler
import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.RepositoryEnricher
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput
import com.societegenerale.githubcrawler.parsers.FileContentParser
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.remote.RemoteGitHubImpl
import com.societegenerale.githubcrawler.remote.RemoteGitLabImpl
import com.societegenerale.githubcrawler.repoTaskToPerform.RepoTaskBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.ConversionService
import org.springframework.core.env.Environment

@Configuration
@Import(GitHubCrawlerParserConfig::class,GitHubCrawlerOutputConfig::class,GitHubCrawlerMiscTasksConfig::class,GitHubConfiguration::class,GitLabConfiguration::class)
@EnableConfigurationProperties(GitHubCrawlerProperties::class)
open class GitHubCrawlerAutoConfiguration {

    @Bean
    open fun conversionService(): ConversionService {
        //TODO custom converter should be an annotation if possible - right now, it works because we instantiate a bean named conversionService
        // If we rename it, Spring won't find it, and it will fail
        return com.societegenerale.githubcrawler.FileToParseConversionService()
    }

    @Bean
    open fun crawler(remoteGitHub: RemoteGitHub,
                     output: List<GitHubCrawlerOutput>,
                     gitHubCrawlerProperties: GitHubCrawlerProperties,
                     environment : Environment,
                     configValidator: ConfigValidator,
                     fileContentParsers: List<FileContentParser>,
                     repoTasksBuilder: List<RepoTaskBuilder>
                     ): GitHubCrawler {

        val repositoryEnricher = RepositoryEnricher(remoteGitHub)

        return GitHubCrawler(remoteGitHub, output, repositoryEnricher,gitHubCrawlerProperties,environment,gitHubCrawlerProperties.githubConfig.organizationName,configValidator,fileContentParsers,repoTasksBuilder)
    }

    @Bean
    open fun configValidator(gitHubCrawlerProperties: GitHubCrawlerProperties,
                             remoteGitHub: RemoteGitHub): ConfigValidator {

        return ConfigValidator(gitHubCrawlerProperties,remoteGitHub)
    }

}
