package com.societegenerale.githubcrawler.parsers


import com.societegenerale.githubcrawler.model.Author
import com.societegenerale.githubcrawler.model.commit.Commit
import com.societegenerale.githubcrawler.model.commit.CommitStats
import com.societegenerale.githubcrawler.model.commit.DetailedCommit
import com.societegenerale.githubcrawler.model.team.Membership
import com.societegenerale.githubcrawler.model.team.Team
import com.societegenerale.githubcrawler.model.team.TeamMember
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.repoTaskToPerform.ownership.MembershipParser
import com.societegenerale.githubcrawler.repoTaskToPerform.ownership.OwnershipParserImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class OwnershipParserImplTest {

    private val githubClient: RemoteGitHub = mock(RemoteGitHub::class.java)
    private val membershipParser: MembershipParser = mock(MembershipParser::class.java)
    private val organizationName = "FCC_OSD"
    private val repositoryFullName = "declarative-process"
    private val memberIdToTeamName = Membership()
    private val lastCommitNumber = 5

    @Test
    fun should_compute_team_ownership_for_a_repo() {
        //Given
        memberIdToTeamName.add(Team("2", "Stark"), setOf(TeamMember("U1", "userFive"), TeamMember("U2", "userOne")))
        memberIdToTeamName.add(Team("3", "White Walkers"), setOf(TeamMember("U3", "UserFour"), TeamMember("U4", "UserThree")))

        `when`(membershipParser.computeMembership())
                .thenReturn(memberIdToTeamName)


        doReturn(setOf(Commit("1"), Commit("2"), Commit("3")))
                .`when`(githubClient)
                .fetchCommits(repositoryFullName, lastCommitNumber)

        doReturn(DetailedCommit("1", Author("12", "userFive"), CommitStats(500)))
                .`when`(githubClient)
                .fetchCommit(repositoryFullName, "1")
        doReturn(DetailedCommit("2", Author("13", "UserFour"), CommitStats(450)))
                .`when`(githubClient)
                .fetchCommit(repositoryFullName, "2")
        doReturn(DetailedCommit("3", Author("14", "UserThree"), CommitStats(49)))
                .`when`(githubClient)
                .fetchCommit(repositoryFullName, "3")

        //When
        val teamOwner: String = OwnershipParserImpl(githubClient, membershipParser, organizationName).computeOwnershipFor( repositoryFullName, lastCommitNumber)

        //Then
        verify(githubClient).fetchCommits(repositoryFullName, lastCommitNumber)
        assertThat(teamOwner).isEqualTo("Stark")
    }

    @Test
    fun should_not_compute_team_ownership_for_a_repo_if_membership_not_provided() {

        //Given
        `when`(membershipParser.computeMembership())
                .thenReturn(memberIdToTeamName)

        //When
        val teamOwner: String = OwnershipParserImpl(githubClient, membershipParser, organizationName).computeOwnershipFor(repositoryFullName, lastCommitNumber)

        //Then
        verifyNoInteractions(githubClient)
        assertThat(teamOwner).isEqualTo("Undefined")
    }

}