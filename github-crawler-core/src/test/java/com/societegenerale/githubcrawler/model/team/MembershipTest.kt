package com.societegenerale.githubcrawler.model.team

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class MembershipTest {

    @Test
    fun should_return_user_login_if_no_matching_team() {
        Assertions.assertThat(Membership().getTeams("a.user")).containsExactly("a.user")
    }

    @Test
    fun should_add_team_and_members_with_multiple_team_support() {
        //Given
        val membership = Membership()
        membership.add(Team("1", "First Team"), setOf(TeamMember("U1", "a.user")))
        membership.add(Team("2", "Second Team"), setOf(TeamMember("U1", "a.user")))

        //When
        val team = membership.getTeams("a.user")

        //Then
        Assertions.assertThat(team).containsExactlyInAnyOrder("First Team", "Second Team")
    }

}
