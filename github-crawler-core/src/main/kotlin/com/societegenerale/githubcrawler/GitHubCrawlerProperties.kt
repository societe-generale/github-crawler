package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty


@ConfigurationProperties("github-crawler")
@EnableConfigurationProperties
class GitHubCrawlerProperties(@NestedConfigurationProperty
                              val sourceControl : GithubConfig=GithubConfig(),
                              val indicatorsToFetchByFile: Map<com.societegenerale.githubcrawler.FileToParse, List<IndicatorDefinition>> = HashMap(),
                              var repositoriesToExclude: List<String> = ArrayList(),
                              var publishExcludedRepositories: Boolean = false,
                              var crawlAllBranches: Boolean = false,
                              var crawlInParallel: Boolean = true,
                              //even if we're not using it, declaring outputs here so that it shows in completion
                              val outputs: List<GitHubCrawlerOutput> = emptyList(),
                              val miscRepositoryTasks: List<TaskDefinition>  = ArrayList()
                              )

