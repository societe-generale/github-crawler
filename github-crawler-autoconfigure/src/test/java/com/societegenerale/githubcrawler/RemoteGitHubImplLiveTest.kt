package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.remote.RemoteGitHubImpl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@Disabled("useful class to debug easily in local")
class RemoteGitHubImplLiveTest {

    @Test
    fun shouldFetchTeams() {
        val remoteGitHubForUser = RemoteGitHubImpl("https://api.github.com", false,"abcdef");

        val teams=remoteGitHubForUser.fetchTeams("myOrga")

        assertThat(teams).isNotEmpty
    }

    @Test
    fun shouldFetchCommits() {

        val remoteGitHubForUser = RemoteGitHubImpl("https://api.github.com", false,"someToken");

        val commits=remoteGitHubForUser.fetchCommits("myOrga/myRepo",150)

        assertThat(commits).isEmpty()
    }

}