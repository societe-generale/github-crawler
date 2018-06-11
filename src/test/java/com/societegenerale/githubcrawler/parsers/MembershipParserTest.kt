package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.model.team.Team
import com.societegenerale.githubcrawler.model.team.TeamMember
import com.societegenerale.githubcrawler.ownership.MembershipParser
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*


class MembershipParserTest {

    @Test
    fun should_build_membership() {
        //Given
        val githubClient: RemoteGitHub = mock(RemoteGitHub::class.java)
        val organizationName = "FCC_OSD"

        doReturn(setOf(Team("2", "Stark"),Team("3", "White Walkers")))
                .`when`(githubClient)
                .fetchTeams("A_FAKE_TOKEN", organizationName)

        doReturn(setOf(TeamMember("U1", "userFive"), TeamMember("U2", "userOne")))
                .`when`(githubClient)
                .fetchTeamsMembers("A_FAKE_TOKEN", "2")
        doReturn(setOf(TeamMember("U3", "UserFour"), TeamMember("U4", "UserThree")))
                .`when`(githubClient)
                .fetchTeamsMembers("A_FAKE_TOKEN", "3")

        //When
        val membership = MembershipParser(githubClient, "A_FAKE_TOKEN", organizationName).computeMembership()

        //Then
        verify(githubClient).fetchTeams("A_FAKE_TOKEN", organizationName)
        verify(githubClient).fetchTeamsMembers("A_FAKE_TOKEN", "2")
        verify(githubClient).fetchTeamsMembers("A_FAKE_TOKEN", "3")

        assertThat(membership).isNotNull()
        assertThat(membership.isEmpty()).isFalse()
    }

    @Test
    fun should_filter_developers_team_when_building_membership() {
        //Given
        val githubClient: RemoteGitHub = mock(RemoteGitHub::class.java)
        doReturn(setOf(Team("2", "Developers")))
                .`when`(githubClient)
                .fetchTeams("A_FAKE_TOKEN", "FCC_OSD")

        //When
        MembershipParser(githubClient, "A_FAKE_TOKEN", "FCC_OSD").computeMembership()

        //Then
        verify(githubClient).fetchTeams("A_FAKE_TOKEN", "FCC_OSD")
        verify(githubClient, never()).fetchTeamsMembers(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
    }

    @Test
    fun should_filter_tech_leads_team_when_building_membership() {
        //Given
        val githubClient: RemoteGitHub = mock(RemoteGitHub::class.java)
        doReturn(setOf(Team("2", "Tech Leads")))
                .`when`(githubClient)
                .fetchTeams("A_FAKE_TOKEN", "FCC_OSD")

        //When
        MembershipParser(githubClient, "A_FAKE_TOKEN", "FCC_OSD").computeMembership()

        //Then
        verify(githubClient).fetchTeams("A_FAKE_TOKEN", "FCC_OSD")
        verify(githubClient, never()).fetchTeamsMembers(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
    }


//  Do we keep this test or not ? token is not nullable
//    @Test
//    fun should_not_build_membership_if_no_token() {
//        //Given
//        val githubClient: RemoteGitHub = mock(RemoteGitHub::class.java)
//        val organizationName = "FCC_OSD"
//
//        //When
//        val membership = MembershipParser(githubClient,null, organizationName).computeMembership()
//
//        //Then
//        verifyZeroInteractions(githubClient)
//
//        assertThat(membership).isNotNull()
//        assertThat(membership.isEmpty()).isTrue()
//    }

    @Test
    fun should_not_build_membership_if_blank_token() {
        //Given
        val githubClient: RemoteGitHub = mock(RemoteGitHub::class.java)
        val organizationName = "FCC_OSD"

        //When
        val membership = MembershipParser(githubClient, "", organizationName).computeMembership()

        //Then
        verifyZeroInteractions(githubClient)

        assertThat(membership).isNotNull()
        assertThat(membership.isEmpty()).isTrue()
    }

}

