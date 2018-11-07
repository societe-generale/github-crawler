package com.societegenerale.githubcrawler

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties


@ConfigurationProperties
@EnableConfigurationProperties
class GitHubCrawlerProperties(val searchesPerRepo: Map<String, SearchParam> = HashMap(),
                              val indicatorsToFetchByFile: Map<com.societegenerale.githubcrawler.FileToParse, List<IndicatorDefinition>> = HashMap(),
                              var repositoriesToExclude: List<String> = ArrayList(),
                              var publishExcludedRepositories: Boolean = false,
                              var crawlAllBranches: Boolean = false,
                              var crawlInParallel: Boolean = true)