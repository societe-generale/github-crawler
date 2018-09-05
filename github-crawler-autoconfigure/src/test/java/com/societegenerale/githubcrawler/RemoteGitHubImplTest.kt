package com.societegenerale.githubcrawler

import com.jayway.awaitility.Awaitility.await
import com.societegenerale.githubcrawler.mocks.GitHubMock
import com.societegenerale.githubcrawler.remote.RemoteGitHubImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.util.concurrent.TimeUnit.SECONDS


@RunWith(SpringRunner::class)
@ActiveProfiles(profiles = arrayOf("test"))
class RemoteGitHubImplTest {

    companion object {

        val githubMockServer: GitHubMock = GitHubMock();

        @BeforeClass
        @JvmStatic
        fun startMockServer() {

            githubMockServer.start()

            await().atMost(5, SECONDS)
                    .until{ assertThat(GitHubMock.hasStarted()) }
        }

        @AfterClass
        @JvmStatic
        fun shutDownMockGithub() {
            githubMockServer.stop()
        }
    }


    @Before
    fun resetMockServer() {
        githubMockServer.reset()
    }


    @Test
    fun shouldCallUsersOrOrgsDependingOnConfig() {

        val remoteGitHubForUser = RemoteGitHubImpl("http://localhost:9900/api/v3", true, null);
        assertThat(githubMockServer.getNbHitsOnUserRepos()).isEqualTo(0);
        remoteGitHubForUser.validateRemoteConfig("someUser");
        assertThat(githubMockServer.getNbHitsOnUserRepos()).isEqualTo(1);

        githubMockServer.reset()

        val remoteGitHubForOrg = RemoteGitHubImpl("http://localhost:9900/api/v3", false, null);
        remoteGitHubForOrg.validateRemoteConfig("MyOrganization");
        assertThat(githubMockServer.getNbHitsOnUserRepos()).isEqualTo(0);
    }

    @Test
    fun shouldReturnEmptySet_whenNoCommitsInrepo() {

        val remoteGitHubForUser = RemoteGitHubImpl("http://localhost:9900/api/v3", true, null);
        githubMockServer.setReturnError409OnFetchCommits(true)

        var commits=remoteGitHubForUser.fetchCommits("MyOrganization","myRepo",150)

        assertThat(commits.isEmpty())
    }


}