package com.societegenerale.githubcrawler.repoTaskToPerform.ownership

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.model.team.Membership
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.repoTaskToPerform.RepoTaskBuilder
import com.societegenerale.githubcrawler.repoTaskToPerform.RepoTaskToPerform
import org.slf4j.LoggerFactory


class RepoOwnershipComputer(private val githubClient: RemoteGitHub,
                            private val membershipParser: MembershipParser,
                            private val organizationName: String,
                            private val lastCommitNumber: Int) : RepoTaskToPerform{


    companion object {
        private val log = LoggerFactory.getLogger(RepoOwnershipComputer::class.java)
    }

    private val UNDEFINED = "Undefined"

    private val INDICATOR_NAME = "owningTeam"

    private var memberIdToTeamName: Membership = Membership()

    private var isParserInitiated: Boolean = false


    override fun perform(repository: Repository): Map<Branch, Map<String, Any>> {

        //TODO see if we can make this an init block - currently, problem for tests, because it requires a remote server to be available
        // to fetch teams...  and the OwnershipParserImpl is instanciated (as part of the whole config) BEFORE the fake remote webserver is available
        synchronized(isParserInitiated) {
            if (!isParserInitiated) {
                memberIdToTeamName = membershipParser.computeMembership()
                isParserInitiated = true
            }
        }

        if (memberIdToTeamName.isEmpty()) {
            log.info("Membership is empty, unable to compute repository owner...")

            return hashMapOf(Pair(Branch(repository.defaultBranch), hashMapOf(Pair(INDICATOR_NAME, UNDEFINED))))
        }

        val commits = githubClient.fetchCommits(repository.fullName, lastCommitNumber)
        val commitsWithStats = commits.map { (sha) -> githubClient.fetchCommit(repository.fullName, sha) }
        log.debug("${commitsWithStats.size} fetched for $organizationName and $repository.fullName (with max fetch to $lastCommitNumber)")
        val owner = commitsWithStats
                .filter { it.author != null }
                .flatMap { (_, author, stats) -> memberIdToTeamName.getTeams(author!!.login).map { team -> Pair(team, stats.total) } }
                .groupBy { it.first }
                .mapValues { it.value.sumBy { stat -> stat.second } }
                .entries
                .sortedByDescending { it.value }
                .firstOrNull()?.key ?: UNDEFINED

        return hashMapOf(Pair(Branch(repository.defaultBranch), hashMapOf(Pair(INDICATOR_NAME, owner))))


    }


}

class RepoOwnershipComputerBuilder(private val githubClient: RemoteGitHub,
                                   private val membershipParser: MembershipParser,
                                   private val organizationName: String,
                                   private val lastCommitNumber: Int) : RepoTaskBuilder  {

    override val type="repositoryOwnershipComputation"

    override fun buildTask(name: String, params : Map<String,String>) : RepoTaskToPerform{
        return RepoOwnershipComputer(githubClient, membershipParser, organizationName, lastCommitNumber)
    }

}