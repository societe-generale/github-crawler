package com.societegenerale.githubcrawler.config


import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.remote.RemoteSourceControl
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
    open fun countHitsOnRepoSearchBuilder(remoteSourceControl: RemoteSourceControl): CountHitsOnRepoSearchBuilder{

        return CountHitsOnRepoSearchBuilder(remoteSourceControl)
    }

    @Bean
    open fun pathsForHitsOnRepoSearchBuilder(remoteSourceControl: RemoteSourceControl): PathsForHitsOnRepoSearchBuilder {

        return PathsForHitsOnRepoSearchBuilder(remoteSourceControl)
    }

    @Bean
    open fun nbOpenPRsOnRepoBuilder(remoteSourceControl: RemoteSourceControl): NbOpenPRsOnRepoBuilder {

        return NbOpenPRsOnRepoBuilder(remoteSourceControl)
    }

    @Bean
    open fun nbBranchesOnRepoBuilder(remoteSourceControl: RemoteSourceControl): NbBranchesOnRepoBuilder {

        return NbBranchesOnRepoBuilder(remoteSourceControl)
    }

    @Bean
    open fun repoOwnershipComputer(remoteSourceControl: RemoteSourceControl, gitHubCrawlerProperties: GitHubCrawlerProperties): RepoOwnershipComputerBuilder {

        val organizationName=gitHubCrawlerProperties.sourceControl.organizationName

        return RepoOwnershipComputerBuilder(remoteSourceControl, MembershipParser(remoteSourceControl, organizationName), organizationName,150);
    }

}


