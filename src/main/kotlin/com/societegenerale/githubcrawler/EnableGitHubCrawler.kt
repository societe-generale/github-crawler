package com.societegenerale.githubcrawler

import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(GitHubCrawlerConfig::class)
annotation class EnableGitHubCrawler

