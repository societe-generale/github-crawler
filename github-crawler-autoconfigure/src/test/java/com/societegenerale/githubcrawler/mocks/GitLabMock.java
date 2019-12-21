package com.societegenerale.githubcrawler.mocks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.codestory.http.Context;
import net.codestory.http.WebServer;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.errors.HttpException;
import net.codestory.http.errors.NotFoundException;
import net.codestory.http.payload.Payload;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

import static com.societegenerale.githubcrawler.remote.RemoteGitHubImpl.CONFIG_VALIDATION_REQUEST_HEADER;

@Component
public class GitLabMock implements RemoteServiceMock {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GitLabMock.class);

    private static boolean hasStarted = false;

    public static boolean hasStarted() {
        return hasStarted;
    }

    private WebServer gitLabWebServer;

    @Override
    public boolean start() {

        gitLabWebServer = new WebServer();
        gitLabWebServer.configure(
                routes -> {

                    routes.get("/api/v4/groups?search=:groupName", (context,groupName) -> getGroups(groupName));

                    routes.get("/groups/:groupId/projects", (context, groupId) -> getRepositories(groupId));
                    routes.get("/projects/:repoId/repository/files/:file_path", (context, repoId, filePath) -> getFileContent(repoId, filePath));

                }
        ).start(GITLAB_MOCK_PORT);

        hasStarted = true;

        return true;
    }

    private String getFileContent(String repoId, String filePath) {
        return null;
    }

    private Payload getGroups(String byGroupName) throws IOException {

        String groupListWIthOneElem = FileUtils.readFileToString(ResourceUtils.getFile("classpath:gitLabGroups.json"), "UTF-8");

        return new Payload("application/json", groupListWIthOneElem);

    }

    private List getRepositories(String groupId) {

        return null;
    }

    @Override
    public void stop() {
        gitLabWebServer.stop();
    }

    @Override
    public void reset() {

    }

}
