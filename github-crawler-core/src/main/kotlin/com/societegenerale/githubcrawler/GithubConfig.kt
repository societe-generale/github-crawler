package com.societegenerale.githubcrawler

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties("github-crawler.source-control")
class GithubConfig(var type: SourceControlType = SourceControlType.GITHUB,
                   var url: String="",
                   var oauthToken: String="",
                   var organizationName: String="",
                   var crawlUsersRepoInsteadOfOrgasRepos: Boolean=false)

enum class SourceControlType {
  GITLAB, GITHUB,AZURE_DEVOPS
}