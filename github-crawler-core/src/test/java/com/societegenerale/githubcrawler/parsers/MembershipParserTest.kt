package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.model.team.Team
import com.societegenerale.githubcrawler.model.team.TeamMember
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.repoTaskToPerform.ownership.MembershipParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*


class MembershipParserTest {

    companion object {
        const val ORGA_NAME = "A_SPECIFIC_ORGA"
    }

    @Test
    fun should_build_membership() {
        //Given
        val githubClient: RemoteGitHub = mock(RemoteGitHub::class.java)


        doReturn(setOf(Team("2", "Stark"), Team("3", "White Walkers")))
                .`when`(githubClient)
                .fetchTeams(ORGA_NAME)

        doReturn(setOf(TeamMember("U1", "userFive"), TeamMember("U2", "userOne")))
                .`when`(githubClient)
                .fetchTeamsMembers("2")
        doReturn(setOf(TeamMember("U3", "UserFour"), TeamMember("U4", "UserThree")))
                .`when`(githubClient)
                .fetchTeamsMembers("3")

        //When
        val membership = MembershipParser(githubClient, ORGA_NAME).computeMembership()

        //Then
        verify(githubClient).fetchTeams(ORGA_NAME)
        verify(githubClient).fetchTeamsMembers("2")
        verify(githubClient).fetchTeamsMembers("3")

        assertThat(membership).isNotNull()
        assertThat(membership.isEmpty()).isFalse()
    }

    @Test
    fun should_filter_developers_team_when_building_membership() {
        //Given
        val githubClient: RemoteGitHub = mock(RemoteGitHub::class.java)
        doReturn(setOf(Team("2", "Developers")))
                .`when`(githubClient)
                .fetchTeams(ORGA_NAME)

        //When
        MembershipParser(githubClient, ORGA_NAME).computeMembership()

        //Then
        verify(githubClient).fetchTeams(ORGA_NAME)
        verify(githubClient, never()).fetchTeamsMembers(ArgumentMatchers.anyString())
    }

    @Test
    fun should_filter_tech_leads_team_when_building_membership() {
        //Given
        val githubClient: RemoteGitHub = mock(RemoteGitHub::class.java)
        doReturn(setOf(Team("2", "Tech Leads")))
                .`when`(githubClient)
                .fetchTeams("FCC_OSD")

        //When
        MembershipParser(githubClient, ORGA_NAME).computeMembership()

        //Then
        verify(githubClient).fetchTeams(ORGA_NAME)
        verify(githubClient, never()).fetchTeamsMembers(ArgumentMatchers.anyString())
    }

}

