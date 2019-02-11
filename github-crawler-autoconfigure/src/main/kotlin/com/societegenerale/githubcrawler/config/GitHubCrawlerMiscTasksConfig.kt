package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.repoTaskToPerform.CountHitsOnRepoSearchBuilder
import com.societegenerale.githubcrawler.repoTaskToPerform.NbBranchesOnRepoBuilder
import com.societegenerale.githubcrawler.repoTaskToPerform.NbOpenPRsOnRepoBuilder
import com.societegenerale.githubcrawler.repoTaskToPerform.PathsForHitsOnRepoSearchBuilder
import com.societegenerale.githubcrawler.repoTaskToPerform.ownership.MembershipParser
import com.societegenerale.githubcrawler.repoTaskToPerform.ownership.RepoOwnershipComputerBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Defining the tasks builder that will be available at runtime
 */
@Configuration
open class GitHubCrawlerMiscTasksConfig {

    @Bean
    open fun countHitsOnRepoSearchBuilder(remoteGitHub: RemoteGitHub): CountHitsOnRepoSearchBuilder{

        return CountHitsOnRepoSearchBuilder(remoteGitHub)
    }

    @Bean
    open fun pathsForHitsOnRepoSearchBuilder(remoteGitHub: RemoteGitHub): PathsForHitsOnRepoSearchBuilder {

        return PathsForHitsOnRepoSearchBuilder(remoteGitHub)
    }

    @Bean
    open fun nbOpenPRsOnRepoBuilder(remoteGitHub: RemoteGitHub): NbOpenPRsOnRepoBuilder {

        return NbOpenPRsOnRepoBuilder(remoteGitHub)
    }

    @Bean
    open fun nbBranchesOnRepoBuilder(remoteGitHub: RemoteGitHub): NbBranchesOnRepoBuilder {

        return NbBranchesOnRepoBuilder(remoteGitHub)
    }

    @Bean
    open fun repoOwnershipComputer(remoteGitHub: RemoteGitHub, gitHubCrawlerProperties: GitHubCrawlerProperties): RepoOwnershipComputerBuilder {

        val organizationName=gitHubCrawlerProperties.githubConfig.organizationName

        return RepoOwnershipComputerBuilder(remoteGitHub, MembershipParser(remoteGitHub, organizationName), organizationName,150);
    }

}


