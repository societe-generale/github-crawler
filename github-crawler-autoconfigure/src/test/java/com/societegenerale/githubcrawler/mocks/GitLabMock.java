package com.societegenerale.githubcrawler.mocks;

import net.codestory.http.WebServer;
import net.codestory.http.payload.Payload;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class GitLabMock implements RemoteServiceMock {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GitLabMock.class);

    private static boolean hasStarted = false;

    private WebServer gitLabWebServer;

    private List<String> repoIdsForWhichFileHasBeenFetched = new ArrayList();

    public static boolean hasStarted() {
        return hasStarted;
    }

    public List<String> getRepoIdsForWhichFileHasBeenFetched() {
        return repoIdsForWhichFileHasBeenFetched;
    }

    @Override
    public boolean start() {

        gitLabWebServer = new WebServer();
        gitLabWebServer.configure(
                routes -> {

                    routes.get("/api/v4/groups?search=:groupName", (context,groupName) -> getGroups(groupName));

                    routes.get("/api/v4/groups/:groupId/projects", (context, groupId) -> getRepositories(groupId));
                    routes.get("/api/v4/projects/:repoId/repository/files/:filePath/raw?ref=:branchName", (context, repoId, filePath,branchName) -> getFileContent(repoId, filePath, branchName));

                    routes.get("/api/v4/projects/:repoId/search?scope=blobs&:searchString", (context, repoId, searchString) -> getRepoSearchResults(repoId, searchString));


                }
        ).start(GITLAB_MOCK_PORT);

        hasStarted = true;

        return true;
    }

    private Object getRepoSearchResults(String repoId, String searchedString) throws IOException {

        log.info("performing a search for repoId {} with query string {}",repoId,searchedString);

        String searchResult = FileUtils.readFileToString(ResourceUtils.getFile("classpath:gitLabRepoSearchResults.json"), "UTF-8");

        return new Payload("application/json", searchResult);

    }

    private Payload getFileContent(String repoId, String filePath, String branchName) throws IOException {

        repoIdsForWhichFileHasBeenFetched.add(repoId);

        String dummyFile="sample_Dockerfile";

        log.info("fetching file {} from repo {} on branch",filePath,repoId,branchName);
        log.info("returning content of {}",dummyFile);

        return new Payload("text/plain; charset=utf-8", FileUtils.readFileToString(ResourceUtils.getFile("classpath:"+dummyFile), "UTF-8"));
    }

    private Payload getGroups(String byGroupName) throws IOException {

        log.info("fetching groups for name {}",byGroupName);

        String groupListWIthOneElem = FileUtils.readFileToString(ResourceUtils.getFile("classpath:gitLabGroups.json"), "UTF-8");

        return new Payload("application/json", groupListWIthOneElem);

    }

    private Payload getRepositories(String groupId) throws IOException {

        log.info("get repositories for group {}",groupId);

        String repositoriesForGroup = FileUtils.readFileToString(ResourceUtils.getFile("classpath:gitLabRepositories.json"), "UTF-8");

        return new Payload("application/json", repositoriesForGroup);
    }

    @Override
    public void stop() {
        gitLabWebServer.stop();
    }

    @Override
    public void reset() {
        repoIdsForWhichFileHasBeenFetched.clear();
    }

}
