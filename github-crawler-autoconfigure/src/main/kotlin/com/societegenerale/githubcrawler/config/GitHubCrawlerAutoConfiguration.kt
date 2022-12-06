package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.*
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput
import com.societegenerale.githubcrawler.parsers.FileContentParser
import com.societegenerale.githubcrawler.remote.RemoteSourceControl
import com.societegenerale.githubcrawler.repoTaskToPerform.RepoTaskBuilder
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.convert.ConversionService
import org.springframework.core.env.Environment

@Configuration
@Import(GitHubCrawlerParserConfig::class,GitHubCrawlerOutputConfig::class,GitHubCrawlerMiscTasksConfig::class,
    GitHubConfiguration::class,GitLabConfiguration::class,AzureDevopsConfiguration::class, BitBucketConfiguration::class)
@EnableConfigurationProperties(GitHubCrawlerProperties::class)
open class GitHubCrawlerAutoConfiguration {

    val log = LoggerFactory.getLogger(this.javaClass)

    @Bean
    open fun conversionService(): ConversionService {
        //TODO custom converter should be an annotation if possible - right now, it works because we instantiate a bean named conversionService
        // If we rename it, Spring won't find it, and it will fail
        return com.societegenerale.githubcrawler.FileToParseConversionService()
    }

    @Bean
    open fun crawler(remoteSourceControl: RemoteSourceControl,
                     output: List<GitHubCrawlerOutput>,
                     gitHubCrawlerProperties: GitHubCrawlerProperties,
                     environment : Environment,
                     configValidator: ConfigValidator,
                     fileContentParsers: List<FileContentParser>,
                     repoTasksBuilders: List<RepoTaskBuilder>
                     ): GitHubCrawler {

        val availableParsersAndTasks = AvailableParsersAndTasks(fileContentParsers,repoTasksBuilders)

        val repositoryEnricher = RepositoryEnricher(remoteSourceControl,availableParsersAndTasks)

        log.info("using repositoryEnricher "+repositoryEnricher+" when building the crawler...")

        return GitHubCrawler(remoteSourceControl, output, repositoryEnricher,gitHubCrawlerProperties,environment,gitHubCrawlerProperties.sourceControl.organizationName,configValidator,availableParsersAndTasks)
    }

    @Bean
    open fun configValidator(gitHubCrawlerProperties: GitHubCrawlerProperties,
                             remoteSourceControl: RemoteSourceControl): ConfigValidator {

        return ConfigValidator(gitHubCrawlerProperties,remoteSourceControl)
    }

}
