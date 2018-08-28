package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.mocks.GitHubMock
import com.societegenerale.githubcrawler.remote.RemoteGitHubImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ActiveProfiles(profiles = arrayOf("test"))
class RemoteGitHubImplTest {

    val gitHubMock : GitHubMock= GitHubMock();

    @Test
    fun shouldCallUsersOrOrgsDependingOnConfig() {

        gitHubMock.start();

        while(!GitHubMock.hasStarted()){
            Thread.sleep(100)
        }

        val remoteGitHubForUser = RemoteGitHubImpl("http://localhost:9900/api/v3", true);
        assertThat(gitHubMock.getNbHitsOnUserRepos()).isEqualTo(0);
        remoteGitHubForUser.validateRemoteConfig("someUser");
        assertThat(gitHubMock.getNbHitsOnUserRepos()).isEqualTo(1);

        gitHubMock.reset()

        val remoteGitHubForOrg = RemoteGitHubImpl("http://localhost:9900/api/v3", false);
        remoteGitHubForOrg.validateRemoteConfig("MyOrganization");
        assertThat(gitHubMock.getNbHitsOnUserRepos()).isEqualTo(0);



    }
}