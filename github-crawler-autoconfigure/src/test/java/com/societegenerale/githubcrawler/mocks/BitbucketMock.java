package com.societegenerale.githubcrawler.mocks;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;


@Component
public class BitbucketMock implements RemoteServiceMock {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BitbucketMock.class);

    private String organisationResponse = "bitbucket/organisation.json";
    private String organisationSecondResponse = "bitbucket/organisation_second.json";

    public final static String REPO_EXCLUDED_CONFIG = "excluded: true";

    private Map<String, String> repoConfigPerRepo = new HashMap<>();

    private static boolean hasStarted = false;

    private Map<String, String> existingResources = new HashMap<>();

    public List<String> getRepoConfigHits() {
        return repoConfigHits;
    }

    public List<String> getPomXmlHits() {
        return pomXmlHits;
    }

    public int getNbPages() {
        return nbPages;
    }

    public boolean isHasCalledNextPage() {
        return hasCalledNextPage;
    }

    private List<String> repoConfigHits = new ArrayList<>();

    private List<String> pomXmlHits = new ArrayList<>();

    private int nbPages = 1;

    private int nbHitsOnUserRepos = 0;

    private int currentPage = 1;


    private boolean hasCalledNextPage = false;

    private List<String> reposWithNoConfig = new ArrayList<>();

    private List<String> reposWithPomXml = new ArrayList<>();

    private int searchHitsCount = 0;

    private boolean shouldReturnError409OnFetchCommits = false;

    public static boolean hasStarted() {
        return hasStarted;
    }

    private WebServer bitbucketWebServer;

    @Override
    public boolean start() {

        bitbucketWebServer = new WebServer();
        bitbucketWebServer.configure(
                routes -> {
                    routes.get("/projects/myProject/repos/:repo/raw/.BitBucketCrawler?at=master", (context, repo) -> getRepoConfigFileOnRepo(repo));
                    routes.get("/projects/myProject/repos/:repo/raw/pom.xml?at=master", (context, repo) -> getPomXmlFileOnRepo(repo));
                    routes.get("/projects/myProject/repos?start=:start", (context, start) -> getOrganisationContent(Integer.parseInt(start)));

                    //for other resources than pom.xml..
                    //hack for resources that are not at the root of the repository, so that we don't have to hardcode too many things
                    routes.get("/raw/myProject/:repo/:branchName/:aSubDirectory/:resource",
                            (context, repo, branchName, aSubDirectory, resource) -> getResource(repo, branchName, aSubDirectory, resource));

                    routes.get("/projects/myProject/repos/:repo/contents/:resource?ref=:branchName",
                            (context, repo, resource, branchName) -> getResourceFileOnRepo(repo, resource, null, branchName));
                    routes.get("/raw/myProject/:repo/:branchName/:resource",
                            (context, repo, branchName, resource) -> getResource(repo, branchName, null, resource));

                    routes.get("/projects/myProject/repos/:repo/branches", (context, repo) -> getBranches(repo));

                    routes.get("/admin/groups", this::getTeams);
                    routes.get("/api/v3/teams/:team/members", (context, team) -> getTeamsMembers(team));

                    routes.get("/projects/myProject/repos/:repo/commits?limit=1", (context, repo) -> getCommits(repo));
                    routes.get("/projects/myProject/repos/:repo/commits/:commit", (context, repo, commit) -> getCommit(repo, commit));

                }
        ).start(BITBUCKET_MOCK_PORT);

        hasStarted = true;

        return true;
    }

    @Override
    public void stop() {
        bitbucketWebServer.stop();
    }

    private Payload geUserReposContent() throws IOException {
        nbHitsOnUserRepos++;

        return buildListOfRepositories(organisationSecondResponse);
    }

    private Payload buildListOfRepositories(String organisationResponse) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(organisationResponse);
        String jsonString = StreamUtils.copyToString(is, Charset.forName("UTF-8"));

        return new Payload("application/json", jsonString);
    }

    public int getNbHitsOnUserRepos() {
        return nbHitsOnUserRepos;
    }

    private Object getResourceFileOnRepo(String repo, String resource, String aSubDirectory, String branchName) throws IOException {

        String fileOnRepoTemplate;

        if (aSubDirectory != null) {

            fileOnRepoTemplate = FileUtils.readFileToString(ResourceUtils.getFile("classpath:github/template_FileOnRepo_withSubDir.json"), "UTF-8");
            fileOnRepoTemplate = fileOnRepoTemplate.replaceFirst("\\$\\{SUB_DIRECTORY}", aSubDirectory);
        } else {
            fileOnRepoTemplate = FileUtils.readFileToString(ResourceUtils.getFile("classpath:github/template_FileOnRepo.json"), "UTF-8");
        }

        fileOnRepoTemplate = fileOnRepoTemplate.replaceFirst("\\$\\{REPO}", repo);
        fileOnRepoTemplate = fileOnRepoTemplate.replaceFirst("\\$\\{BRANCH}", branchName);
        fileOnRepoTemplate = fileOnRepoTemplate.replaceFirst("\\$\\{RESOURCE}", resource);

        return new Payload("application/json", fileOnRepoTemplate);

    }

    private Object getPomXmlFileOnRepo(String repo) throws IOException {

        log.debug("Getting pomXMl on repo...");

        pomXmlHits.add(repo);

        if (reposWithPomXml.contains(repo)) {
            String fileToString = FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_pom.xml"), "UTF-8");

            return new Payload("application/json", fileToString);
        } else {
            log.debug("... not found");

            throw new NotFoundException();
        }

    }

    private Object getRepoConfigFileOnRepo(String repo) throws IOException {

        repoConfigHits.add(repo);

        log.debug("Getting config file on repo...");

        if (repoConfigPerRepo.containsKey(repo)) {
            log.info("\t returning something..");

            return "excluded: true";

        } else {
            log.info("\t .bitbucketCrawler NOT FOUND");
            throw new NotFoundException();
        }

    }

    private Object getCommit(String repo, String commit) throws IOException {
        log.debug("Getting Github commit {} on repo {}...", commit, repo);
        return new Payload("application/json", FileUtils.readFileToString(ResourceUtils.getFile("classpath:bitbucket/commit.json"), "UTF-8"));

    }

    private Payload getCommits(String repo) throws IOException {
        log.debug("Getting Github commits on repo {}...", repo);

        if(shouldReturnError409OnFetchCommits){
            throw new ConflictException();
        }

        return new Payload("application/json", FileUtils.readFileToString(ResourceUtils.getFile("classpath:bitbucket/commits.json"), "UTF-8"));
    }

    private Payload getTeamsMembers(String team) throws IOException {
        log.debug("Getting Github team members...");

        String teamFileName = String.format("team_members_%s.json", team.hashCode() % 2 == 0 ? "A" : "B");

        return new Payload("application/json", FileUtils.readFileToString(ResourceUtils.getFile("classpath:" + teamFileName), "UTF-8"));

    }

    private Payload getTeams() throws IOException {
        log.debug("Getting Github teams...");
        return new Payload("application/json", FileUtils.readFileToString(ResourceUtils.getFile("classpath:teams.json"), "UTF-8"));
    }

    private Object getResource(String repo, String branchName, String aSubDirectory, String resource) throws IOException {

        String pathToResource = aSubDirectory != null ? aSubDirectory + "/" + resource : resource;

        log.debug("retrieving file {} from repo {} on branch {}", pathToResource, repo, branchName);

        String fileNameToReturn = existingResources.get(buildResourceKey(repo, pathToResource));

        log.debug("actual file to return : {}", fileNameToReturn);

        if (fileNameToReturn != null) {

            log.info("\t {} found on repo {} and branch {}", pathToResource, repo, branchName);

            File fileToReturn = ResourceUtils.getFile("classpath:" + fileNameToReturn);

            if (!fileToReturn.exists()) {
                log.error("file {} on repo {} and branch {} should be found. check the test config", pathToResource, repo, branchName);

                log.error("logging all resources available :");
                PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
                Resource[] allResources = resourceResolver.getResources("classpath:**/*");
                Arrays.asList(allResources).stream().forEach(r -> {
                    try {
                        log.error("\t- canonical path : {}, filename : {}, description : {}", r.getFile().getCanonicalPath(), r.getFilename(),
                                r.getDescription());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                assert false :
                        "failing the test immediately - we should be able to return " + fileToReturn + ", but we can't get it as an InputStream";
            }

            return FileUtils.readFileToString(fileToReturn, "UTF-8");

        } else {
            log.info("\t {} NOT found on repo {} and branch {}", pathToResource, repo, branchName);
            throw new NotFoundException();
        }
    }

    @NotNull
    private String buildResourceKey(String repo, String pathToResource) {
        return repo + "-" + pathToResource;
    }

    private Payload getBranches(String repoName) throws IOException {

        log.debug("received a branches request for repo {}..", repoName);

        InputStream is = getClass().getClassLoader().getResourceAsStream("bitbucket/branches.json");
        String jsonString = StreamUtils.copyToString(is, Charset.forName("UTF-8"));

        return new Payload("application/json", jsonString);
    }

    private Payload getOrganisationContentForNextPage(String organisationResponse) throws IOException {

        hasCalledNextPage = true;
        InputStream is = getClass().getClassLoader().getResourceAsStream(organisationResponse);
        String jsonString = StreamUtils.copyToString(is, Charset.forName("UTF-8"));

        return new Payload("application/json", jsonString);
    }

    private String getActualRepoConfig(String repoName) {

        log.debug("received a repoConfig request for repo {}..", repoName);

        repoConfigHits.add(repoName);

        if (reposWithNoConfig.contains(repoName)) {
            log.debug("\t repoConfig NOT FOUND");
            throw new NotFoundException();
        }

        if (repoConfigPerRepo.keySet().contains(repoName)) {
            log.debug("\t repoConfig found and not empty");
            return repoConfigPerRepo.get(repoName);
        } else {
            log.debug("\t repoConfig found and empty");
            return "";
        }

    }

    private Payload getOrganisationContent(int start) throws IOException {

        log.info("fetching content of organisation...");
        Payload response = start == 0 ? buildListOfRepositories(organisationResponse) : getOrganisationContentForNextPage(organisationSecondResponse);
        return response;
    }

    @Override
    public void reset() {
        repoConfigHits.clear();
        reposWithNoConfig.clear();
        repoConfigPerRepo.clear();
        pomXmlHits.clear();
        reposWithPomXml.clear();
        currentPage = 1;
        nbPages = 1;
        hasCalledNextPage = false;
        existingResources.clear();
        searchHitsCount = 0;
        nbHitsOnUserRepos = 0;
        shouldReturnError409OnFetchCommits=false;
    }

    public void addRepoSideConfig(String repoName, String config) {
        repoConfigPerRepo.put(repoName, config);
    }

    public void addReposWithNoConfig(List<String> reposWithNoConfig) {
        this.reposWithNoConfig.addAll(reposWithNoConfig);
    }

    public void addExistingResource(String repoName, String pathToResource, String fileNameWithContent) {
        this.existingResources.put(buildResourceKey(repoName, pathToResource), fileNameWithContent);
    }

    public void addReposWithPomXMl(List<String> reposWithPomXMl) {
        this.reposWithPomXml.addAll(reposWithPomXMl);
    }

    public void setReturnError409OnFetchCommits(boolean shouldReturnError) {
        this.shouldReturnError409OnFetchCommits=shouldReturnError;
    }

    public void setNbPages(int nbPages) {
        this.nbPages=nbPages;
    }

    public int getSearchHitsCount() {
        return searchHitsCount;
    }

    private class ConflictException extends HttpException {
        ConflictException() {
            super(HttpStatus.CONFLICT);
        }
    }
}
