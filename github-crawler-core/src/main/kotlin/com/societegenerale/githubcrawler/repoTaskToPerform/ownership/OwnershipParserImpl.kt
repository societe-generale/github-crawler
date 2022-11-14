package com.societegenerale.githubcrawler.repoTaskToPerform.ownership

import com.societegenerale.githubcrawler.model.team.Membership
import com.societegenerale.githubcrawler.remote.RemoteSourceControl
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Example of implementation for repository ownership computation.
 * Just instantiate a class implementing OwnershipParser interface in your Spring Boot config. If none is found, a dummy one will be instantiated by default.
 */
class OwnershipParserImpl(private val githubClient: RemoteSourceControl, private val membershipParser: MembershipParser, private val organizationName: String) : OwnershipParser {

    companion object {
        private val log = LoggerFactory.getLogger(OwnershipParserImpl::class.java)
    }

    private val UNDEFINED = "Undefined"

    private var memberIdToTeamName: Membership = Membership()

    private var isParserInitiated = AtomicBoolean(false)


    override fun computeOwnershipFor(repositoryFullName: String, lastCommitNumber: Int): String {

        //TODO see if we can make this an init block - currently, problem for tests, because it requires a remote server to be available
        // to fetch teams...  and the OwnershipParserImpl is instantiated (as part of the whole config) BEFORE the fake remote webserver is available

        if (!isParserInitiated.get()) {
            memberIdToTeamName = membershipParser.computeMembership()
            isParserInitiated.set(true)
        }

        if (memberIdToTeamName.isEmpty()) {
            log.info("Membership is empty, unable to compute repository owner...")
            return UNDEFINED
        }

        val commits = githubClient.fetchCommits(repositoryFullName, lastCommitNumber)
        val commitsWithStats = commits.map { (sha) -> githubClient.fetchCommit(repositoryFullName, sha) }
        log.debug("${commitsWithStats.size} fetched for $organizationName and $repositoryFullName (with max fetch to $lastCommitNumber)")
        return commitsWithStats
                .filter { it.author != null }
                .flatMap { (_, author, stats) -> memberIdToTeamName.getTeams(author!!.login).map { team -> Pair(team, stats.total) } }
                .groupBy { it.first }
                .mapValues { it.value.sumOf { stat -> stat.second } }
                .entries
                .sortedByDescending { it.value }
                .firstOrNull()?.key ?: UNDEFINED
    }

}