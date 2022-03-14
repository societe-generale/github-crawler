package com.societegenerale.githubcrawler

import com.jayway.awaitility.Awaitility.await
import com.societegenerale.githubcrawler.mocks.GitHubMock
import com.societegenerale.githubcrawler.remote.RemoteGitHubImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.concurrent.TimeUnit.SECONDS

@ExtendWith(SpringExtension::class)
@ActiveProfiles(profiles = arrayOf("test"))
class RemoteGitHubImplTest {

    companion object {

        val githubMockServer: GitHubMock = GitHubMock();

        @BeforeAll
        @JvmStatic
        fun startMockServer() {

            githubMockServer.start()

            await().atMost(5, SECONDS)
                    .until{ assertThat(GitHubMock.hasStarted()) }
        }

        @AfterAll
        @JvmStatic
        fun shutDownMockGithub() {
            githubMockServer.stop()
        }
    }


    @BeforeEach
    fun resetMockServer() {
        githubMockServer.reset()
    }


    @Test
    fun shouldCallUsersOrOrgsDependingOnConfig() {

        val remoteGitHubForUser = RemoteGitHubImpl("http://localhost:9900/api/v3", true, "someToken");
        assertThat(githubMockServer.getNbHitsOnUserRepos()).isEqualTo(0);
        remoteGitHubForUser.validateRemoteConfig("someUser");
        assertThat(githubMockServer.getNbHitsOnUserRepos()).isEqualTo(1);

        githubMockServer.reset()

        val remoteGitHubForOrg = RemoteGitHubImpl("http://localhost:9900/api/v3", false, "someToken");
        remoteGitHubForOrg.validateRemoteConfig("MyOrganization");
        assertThat(githubMockServer.getNbHitsOnUserRepos()).isEqualTo(0);
    }

    @Test
    fun shouldReturnEmptySet_whenNoCommitsInrepo() {

        val remoteGitHubForUser = RemoteGitHubImpl("http://localhost:9900/api/v3", true, "someToken");
        githubMockServer.setReturnError409OnFetchCommits(true)

        var commits=remoteGitHubForUser.fetchCommits("MyOrganization/myRepo",150)

        assertThat(commits.isEmpty())
    }


}