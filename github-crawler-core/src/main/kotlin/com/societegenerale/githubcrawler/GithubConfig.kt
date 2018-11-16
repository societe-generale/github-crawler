package com.societegenerale.githubcrawler

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties("github-crawler.githubConfig")
class GithubConfig(val url: String="",
                        val oauthToken: String="",
                        val organizationName: String="",
                        val crawlUsersRepoInsteadOfOrgasRepos: Boolean=false)
