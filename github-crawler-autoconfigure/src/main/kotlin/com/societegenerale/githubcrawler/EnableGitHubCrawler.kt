package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.GitHubCrawlerAutoConfiguration
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(GitHubCrawlerAutoConfiguration::class)
annotation class EnableGitHubCrawler

