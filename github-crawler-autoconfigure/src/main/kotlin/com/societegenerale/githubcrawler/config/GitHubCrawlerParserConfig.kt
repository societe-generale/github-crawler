package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.parsers.*
import com.societegenerale.githubcrawler.repoTaskToPerform.PathsForHitsOnRepoSearch
import com.societegenerale.githubcrawler.repoTaskToPerform.RepoTaskToPerform
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
    open fun pathSearchResultParser(): ItemPathSearchResultParser{

        return ItemPathSearchResultParser()
    }

    @Bean
    open fun pathsForHitsOnRepoSearch(): RepoTaskToPerform {

        return PathsForHitsOnRepoSearch()
    }


}


