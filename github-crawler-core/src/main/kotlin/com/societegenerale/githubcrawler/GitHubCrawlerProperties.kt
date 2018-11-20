package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty


@ConfigurationProperties("github-crawler")
@EnableConfigurationProperties
class GitHubCrawlerProperties(@NestedConfigurationProperty
                              val githubConfig : GithubConfig=GithubConfig(),
                              val searchesPerRepo: Map<String, SearchParam> = HashMap(),
                              val indicatorsToFetchByFile: Map<com.societegenerale.githubcrawler.FileToParse, List<IndicatorDefinition>> = HashMap(),
                              var repositoriesToExclude: List<String> = ArrayList(),
                              var publishExcludedRepositories: Boolean = false,
                              var crawlAllBranches: Boolean = false,
                              var crawlInParallel: Boolean = true,
                              val outputs: List<GitHubCrawlerOutput> = emptyList(),
                              val miscRepositoryTasks: List<TaskDefinition>  = ArrayList()
                              )

