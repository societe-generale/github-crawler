package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty


@ConfigurationProperties("crawler")
@EnableConfigurationProperties
class GitHubCrawlerProperties(@NestedConfigurationProperty
                              val sourceControl : SourceControlConfig=SourceControlConfig(),
                              val indicatorsToFetchByFile: Map<FileToParse, List<IndicatorDefinition>> = HashMap(),
                              var repositoriesToExclude: List<String> = ArrayList(),
                              var repositoriesToInclude: List<String> = ArrayList(),
                              var publishExcludedRepositories: Boolean = false,
                              var crawlAllBranches: Boolean = false,
                              var crawlInParallel: Boolean = true,
                              //even if we're not using it, declaring outputs here so that it shows in completion
                              val outputs: List<GitHubCrawlerOutput> = emptyList(),
                              val miscRepositoryTasks: List<TaskDefinition>  = ArrayList()
                              ){
  init{
    if(repositoriesToExclude.isNotEmpty() && repositoriesToInclude.isNotEmpty()){
      throw IllegalStateException("we can't have both exclusion AND inclusion patterns configured : please pick only one")
    }
  }

}

