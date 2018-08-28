package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.ConfigValidator
import com.societegenerale.githubcrawler.GitHubCrawler
import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.RepositoryEnricher
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput
import com.societegenerale.githubcrawler.ownership.NoOpOwnershipParser
import com.societegenerale.githubcrawler.ownership.OwnershipParser
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.remote.RemoteGitHubImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.convert.ConversionService
import org.springframework.core.env.Environment

@Configuration
@Import(GitHubCrawlerParserConfig::class,GitHubCrawlerOutputConfig::class)
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
                     ownershipParser: OwnershipParser,
                     output: List<GitHubCrawlerOutput>,
                     gitHubCrawlerProperties: GitHubCrawlerProperties,
                     environment : Environment,
                     @Value("\${organizationName}")
                     organizationName: String,
                     @Value("\${gitHub.url}")
                     gitHubUrl: String,
                     configValidator: ConfigValidator): GitHubCrawler {

        val repositoryEnricher = RepositoryEnricher(remoteGitHub)

        return GitHubCrawler(remoteGitHub, ownershipParser, output, repositoryEnricher,gitHubCrawlerProperties,environment,organizationName,gitHubUrl,configValidator)
    }

    @Bean
    open fun configValidator(gitHubCrawlerProperties: GitHubCrawlerProperties,
                             @Value("\${gitHub.url}")
                             gitHubUrl: String,
                             @Value("\${organizationName}")
                             organizationName: String,
                             remoteGitHub: RemoteGitHub): ConfigValidator {

        return ConfigValidator(gitHubCrawlerProperties, gitHubUrl,organizationName,remoteGitHub)
    }


    @Bean
    open fun remoteGitHub(@Value("\${gitHub.url}") gitHubUrl: String,
                          @Value("\${crawl.usersRepo.insteadOf.orgasRepos:false}" ) usersRepoInsteadOfOrgas: Boolean,
                          @Value("\${gitHub.oauth.token}" ) oauthToken: String): RemoteGitHub {

        return RemoteGitHubImpl(gitHubUrl,usersRepoInsteadOfOrgas,oauthToken)
    }

    @Bean
    @ConditionalOnMissingBean(value = OwnershipParser::class)
    open fun dummyOwnershipParser(): OwnershipParser {

        return NoOpOwnershipParser()
    }

}
