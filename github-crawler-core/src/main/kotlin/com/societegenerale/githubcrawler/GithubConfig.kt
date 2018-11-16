package com.societegenerale.githubcrawler

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties("github-crawler.githubConfig")
class GithubConfig(var url: String="",
                        var oauthToken: String="",
                        var organizationName: String="",
                        var crawlUsersRepoInsteadOfOrgasRepos: Boolean=false)
