package com.societegenerale.githubcrawler.ownership

import com.societegenerale.githubcrawler.model.team.Membership
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import org.slf4j.LoggerFactory
import java.util.function.Consumer

class MembershipParser(private val githubClient: RemoteGitHub, private val organizationName: String) {

    companion object {
        private val log = LoggerFactory.getLogger(MembershipParser::class.java)
    }

    private val EXCLUDED_TEAMS = setOf("Developers", "Tech Leads", "Architects")

    fun computeMembership(): Membership {
        val membership = Membership()

        val teams = githubClient.fetchTeams(organizationName)
        log.debug("fetching teams for $organizationName organization returned $teams")
        teams.forEach(Consumer {
            if (EXCLUDED_TEAMS.contains(it.name).not()) {
                val members = githubClient.fetchTeamsMembers(it.id)
                membership.add(it, members)
                log.debug("fetching teams members for ${it.name} team returned $members")
            }
        })
        return membership
    }

}