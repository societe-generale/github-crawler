package com.societegenerale.githubcrawler

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties("github-crawler.githubConfig")
class GithubConfig(var type: SourceControlType = SourceControlType.GITHUB,
                   var apiUrl: String="",
                   var oauthToken: String="",
                   var organizationName: String="",
                   var crawlUsersRepoInsteadOfOrgasRepos: Boolean=false)

enum class SourceControlType {
  GITLAB, GITHUB
}