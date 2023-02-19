package com.societegenerale.githubcrawler

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties("crawler.source-control")
class SourceControlConfig(var type: SourceControlType = SourceControlType.GITHUB,
                          var url: String="",
                          var apiToken: String="",
                          var organizationName: String="",
                          var crawlUsersRepoInsteadOfOrgasRepos: Boolean=false)

enum class SourceControlType {
  GITLAB, GITHUB,AZURE_DEVOPS, BITBUCKET
}