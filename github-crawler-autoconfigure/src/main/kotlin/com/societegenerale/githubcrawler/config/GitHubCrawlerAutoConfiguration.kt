package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.ConfigValidator
import com.societegenerale.githubcrawler.GitHubCrawler
import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.RepositoryEnricher
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput
import com.societegenerale.githubcrawler.ownership.NoOpOwnershipParser
import com.societegenerale.githubcrawler.ownership.OwnershipParser
import com.societegenerale.githubcrawler.parsers.FileContentParser
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.remote.RemoteGitHubImpl
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
//
//    @Bean
//    open fun githubConfig(
//                          @Value("\${gitHub.url}")
//                          url : String,
//                          @Value("\${gitHub.url}")
//                          oauthToken : String,
//                          @Value("\${organizationName}")
//                          organizationName: String,
//                          @Value("\${gitHub.url}")
//                          crawlUsersRepoInsteadOfOrgasRepos : Boolean
//                          ) :GithubConfig{
//
//        return GithubConfig(url,oauthToken,organizationName,crawlUsersRepoInsteadOfOrgasRepos)
//    }


    @Bean
    open fun crawler(remoteGitHub: RemoteGitHub,
                     ownershipParser: OwnershipParser,
                     output: List<GitHubCrawlerOutput>,
                     gitHubCrawlerProperties: GitHubCrawlerProperties,
                     environment : Environment,
                     configValidator: ConfigValidator,
                     fileContentParsers: List<FileContentParser>
                     ): GitHubCrawler {

        val repositoryEnricher = RepositoryEnricher(remoteGitHub)

        return GitHubCrawler(remoteGitHub, ownershipParser, output, repositoryEnricher,gitHubCrawlerProperties,environment,gitHubCrawlerProperties.githubConfig.organizationName,configValidator,fileContentParsers)
    }

    @Bean
    open fun configValidator(gitHubCrawlerProperties: GitHubCrawlerProperties,
                             remoteGitHub: RemoteGitHub): ConfigValidator {

        return ConfigValidator(gitHubCrawlerProperties,remoteGitHub)
    }


    @Bean
    open fun remoteGitHub(gitHubCrawlerProperties: GitHubCrawlerProperties): RemoteGitHub {

        return RemoteGitHubImpl(gitHubCrawlerProperties.githubConfig.url,gitHubCrawlerProperties.githubConfig.crawlUsersRepoInsteadOfOrgasRepos,gitHubCrawlerProperties.githubConfig.oauthToken)
    }

    @Bean
    @ConditionalOnMissingBean(OwnershipParser::class)
    open fun dummyOwnershipParser(): OwnershipParser {

        return NoOpOwnershipParser()
    }

}
