package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.parsers.*
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.repoTaskToPerform.CountHitsOnRepoSearchBuilder
import com.societegenerale.githubcrawler.repoTaskToPerform.PathsForHitsOnRepoSearchBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class GitHubCrawlerParserConfig {

    @Bean
    open fun pomXmlParserForDependencyVersion(): PomXmlParserForDependencyVersion {

        return PomXmlParserForDependencyVersion()
    }

    @Bean
    open fun firstMatchingRegexpParser(): FirstMatchingRegexpParser {

        return FirstMatchingRegexpParser()
    }


    @Bean
    open fun simpleFilePathParser(): SimpleFilePathParser {

        return SimpleFilePathParser()
    }

    @Bean
    open fun yamlParserForPropertyVal(): YamlParserForPropertyValue {

        return YamlParserForPropertyValue()
    }

    @Bean
    open fun countXmlElementsParser(): CountXmlElementsParser {

        return CountXmlElementsParser()
    }

    @Bean
    open fun countHitsOnRepoSearchBuilder(remoteGitHub: RemoteGitHub): CountHitsOnRepoSearchBuilder{

        return CountHitsOnRepoSearchBuilder(remoteGitHub)
    }

    @Bean
    open fun pathsForHitsOnRepoSearchBuilder(remoteGitHub: RemoteGitHub): PathsForHitsOnRepoSearchBuilder {

        return PathsForHitsOnRepoSearchBuilder(remoteGitHub)
    }


}


