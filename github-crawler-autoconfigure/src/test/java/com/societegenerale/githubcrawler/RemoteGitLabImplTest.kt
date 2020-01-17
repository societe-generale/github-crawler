package com.societegenerale.githubcrawler

import com.jayway.awaitility.Awaitility.await
import com.societegenerale.githubcrawler.mocks.GitLabMock
import com.societegenerale.githubcrawler.mocks.RemoteServiceMock.GITLAB_MOCK_PORT
import com.societegenerale.githubcrawler.remote.RemoteGitLabImpl
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
class RemoteGitLabImplTest {

    companion object {

        val gitlabMockServer: GitLabMock = GitLabMock();

        @BeforeClass
        @JvmStatic
        fun startMockServer() {

            gitlabMockServer.start()

            await().atMost(5, SECONDS)
                    .until{ assertThat(GitLabMock.hasStarted()) }
        }

        @AfterClass
        @JvmStatic
        fun shutDownMockGitLab() {
            gitlabMockServer.stop()
        }
    }


    @Before
    fun resetMockServer() {
        gitlabMockServer.reset()
    }

    val remoteGitLab = RemoteGitLabImpl("http://localhost:"+GITLAB_MOCK_PORT+"/api/v4",  "someToken");

    @Test
    fun shouldGetRepositoriesForAgroup() {

        val repositoriesForMyGroup=remoteGitLab.fetchRepositories("myGroup")

        assertThat(repositoriesForMyGroup).isNotEmpty

    }

    @Test
    fun shouldGetWantedFileContent() {

        remoteGitLab.fetchRepositories("myGroup")

        val fileContent=remoteGitLab.fetchFileContent("h5bp/html5-boilerplate","master","Dockerfile")

        assertThat(fileContent).isNotBlank()

    }

    @Test
    fun shouldQueryFileContentWithRepoIdInsteadOfRepoName() {

        remoteGitLab.fetchRepositories("myGroup")

        val fileContent=remoteGitLab.fetchFileContent("h5bp/html5-boilerplate","master","Dockerfile")

        assertThat(fileContent).isNotBlank()

        assertThat(gitlabMockServer.repoIdsForWhichFileHasBeenFetched).containsOnly("16")
    }

    @Test
    fun shouldPerformRepoSearch() {

        remoteGitLab.fetchRepositories("myGroup")

        val searchResults=remoteGitLab.fetchCodeSearchResult("h5bp/html5-boilerplate","blabla")

        assertThat(searchResults.totalCount).isEqualTo(2);
    }


}
