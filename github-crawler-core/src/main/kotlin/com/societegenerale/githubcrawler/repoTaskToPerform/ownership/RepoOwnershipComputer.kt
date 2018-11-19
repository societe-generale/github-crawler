package com.societegenerale.githubcrawler.repoTaskToPerform.ownership

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.model.team.Membership
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.repoTaskToPerform.RepoTaskToPerform
import org.slf4j.LoggerFactory


class RepoOwnershipComputer(private val githubClient: RemoteGitHub,
                            private val membershipParser: MembershipParser,
                            private val organizationName: String,
                            private val lastCommitNumber: Int) : RepoTaskToPerform {

    companion object {
        private val log = LoggerFactory.getLogger(RepoOwnershipComputer::class.java)
    }

    private val UNDEFINED = "Undefined"

    private val INDICATOR_NAME = "owningTeam"

    private var memberIdToTeamName: Membership = Membership()

    private var isParserInitiated: Boolean = false


    override fun perform(repository: Repository): Repository {

            //TODO see if we can make this an init block - currently, problem for tests, because it requires a remote server to be available
            // to fetch teams...  and the OwnershipParserImpl is instanciated (as part of the whole config) BEFORE the fake remote webserver is available
            synchronized(isParserInitiated) {
                if (!isParserInitiated) {
                    memberIdToTeamName = membershipParser.computeMembership()
                    isParserInitiated = true
                }
            }

            val existingMiscTasksResults=repository.miscTasksResults.toMutableMap()

            val existingMiscTasksResultsForDefaultBranch=existingMiscTasksResults[Branch(repository.defaultBranch)].orEmpty()

            if (memberIdToTeamName.isEmpty()) {
                log.info("Membership is empty, unable to compute repository owner...")

                existingMiscTasksResultsForDefaultBranch.plus(Pair(INDICATOR_NAME ,UNDEFINED))

                return repository.copy(miscTasksResults = existingMiscTasksResults)
            }

            val commits = githubClient.fetchCommits(organizationName, repository.fullName, lastCommitNumber)
            val commitsWithStats = commits.map { (sha) -> githubClient.fetchCommit(organizationName, repository.fullName, sha) }
            log.debug("${commitsWithStats.size} fetched for $organizationName and $repository.fullName (with max fetch to $lastCommitNumber)")
            val owner= commitsWithStats
                    .filter { it.author != null }
                    .flatMap { (_, author, stats) -> memberIdToTeamName.getTeams(author!!.login).map { team -> Pair(team, stats.total) } }
                    .groupBy { it.first }
                    .mapValues { it.value.sumBy { stat -> stat.second } }
                    .entries
                    .sortedByDescending { it.value }
                    .firstOrNull()?.key ?: UNDEFINED

            existingMiscTasksResultsForDefaultBranch.plus(Pair(INDICATOR_NAME ,owner))

            return repository.copy(miscTasksResults = existingMiscTasksResults)

    }


}