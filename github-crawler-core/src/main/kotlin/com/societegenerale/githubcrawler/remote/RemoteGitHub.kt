package com.societegenerale.githubcrawler.remote

import com.societegenerale.githubcrawler.RepositoryConfig
import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.PullRequest
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.model.SearchResult
import com.societegenerale.githubcrawler.model.commit.Commit
import com.societegenerale.githubcrawler.model.commit.DetailedCommit
import com.societegenerale.githubcrawler.model.team.Team
import com.societegenerale.githubcrawler.model.team.TeamMember

interface RemoteGitHub {

    fun fetchRepoConfig(repositoryFullName: String, defaultBranch: String): RepositoryConfig

    fun fetchRepoBranches(repositoryFullName: String): Set<Branch>

    fun fetchCodeSearchResult(repository: Repository, query: String): SearchResult

    @Throws(NoFileFoundException::class)
    fun fetchFileContent(repositoryFullName: String, branchName: String, fileToFetch: String): String

    fun fetchCommits(repositoryFullName: String,
                     perPage: Int): Set<Commit>

    fun fetchCommit(repositoryFullName: String,
                    commitSha: String): DetailedCommit

    fun fetchTeams(organizationName: String): Set<Team>

    fun fetchTeamsMembers(teamId: String): Set<TeamMember>

    fun fetchRepositories(organizationName: String): Set<Repository>

    /**
     * Enables us to hit the API once and confirm that our configuration is correct. If it's not, we throw an exception with details of what went wrong.
     */
    @Throws(NoReachableRepositories::class)
    fun validateRemoteConfig(organizationName: String)

    fun fetchOpenPRs(repositoryFullName: String): Set<PullRequest>

}

class NoFileFoundException : Exception {

    constructor(message: String) : super(message)

}

class NoReachableRepositories : Exception {

    constructor(message: String, t: Throwable) : super(message,t)

    constructor(message: String) : super(message)

}

