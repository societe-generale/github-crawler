package com.societegenerale.githubcrawler.mocks;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.codestory.http.WebServer;
import net.codestory.http.errors.NotFoundException;
import net.codestory.http.payload.Payload;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
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
@Slf4j
public class GitHubMock implements RemoteServiceMock {

    private String organisationResponse = "organisation.json";

    public final static String REPO_EXCLUDED_CONFIG = "excluded: true";

    private Map<String, String> repoConfigPerRepo = new HashMap<>();

    private static boolean hasStarted = false;

    private Map<String, String> existingResources = new HashMap<>();

    @Getter
    private List<String> repoConfigHits = new ArrayList<>();

    @Getter
    private List<String> pomXmlHits = new ArrayList<>();

    @Setter
    @Getter
    private int nbPages = 1;

    private int currentPage = 1;

    @Getter
    private boolean hasCalledNextPage = false;

    private List<String> reposWithNoConfig = new ArrayList<>();

    private List<String> reposWithPomXml = new ArrayList<>();

    @Getter
    private int searchHitsCount = 0;

    public static boolean hasStarted() {
        return hasStarted;
    }

    @Override
    public boolean start() {

        WebServer gitHubWebServer = new WebServer();
        gitHubWebServer.configure(
                routes -> {

                    routes.get("/api/v3/repos/MyOrganization/:repo/contents/.githubCrawler", (context, repo) -> getRepoConfigFileOnRepo(repo));
                    routes.get("/raw/MyOrganization/:repo/master/.githubCrawler", (context, repo) -> getActualRepoConfig(repo));

                    routes.get("/api/v3/orgs/MyOrganization/repos", context -> getOrganisationContent());
                    routes.get("/api/v3/organizations/1114/repos", context -> getOrganisationContentForNextPage());

                    routes.get("/api/v3/repos/MyOrganization/:repo/contents/pom.xml?ref=master", (context, repo) -> getPomXmlFileOnRepo(repo));
                    routes.get("/raw/MyOrganization/:repo/:branchName/pom.xml", (context, repo, branchName) -> getActualPomXML(repo, branchName));

                    //for other resources than pom.xml..
                    //hack for resources that are not at the root of the repository, so that we don't have to hardcode too many things
                    routes.get("/api/v3/repos/MyOrganization/:repo/contents/:aSubDirectory/:resource?ref=:branchName",
                            (context, repo, aSubDirectory, resource, branchName) -> getResourceFileOnRepo(repo, resource, aSubDirectory, branchName));
                    routes.get("/raw/MyOrganization/:repo/:branchName/:aSubDirectory/:resource",
                            (context, repo, branchName, aSubDirectory, resource) -> getResource(repo, branchName, aSubDirectory, resource));

                    routes.get("/api/v3/repos/MyOrganization/:repo/contents/:resource?ref=:branchName",
                            (context, repo, resource, branchName) -> getResourceFileOnRepo(repo, resource, null, branchName));
                    routes.get("/raw/MyOrganization/:repo/:branchName/:resource",
                            (context, repo, branchName, resource) -> getResource(repo, branchName, null, resource));

                    routes.get("/api/v3/repos/MyOrganization/:repo/branches", (context, repo) -> getBranches(repo));

                    routes.get("/api/v3/search/code?q=:searchQuery", (context, searchQuery) -> getSearchResult(searchQuery));

                    routes.get("/api/v3/orgs/MyOrganization/teams", this::getTeams);
                    routes.get("/api/v3/teams/:team/members", (context, team) -> getTeamsMembers(team));

                    routes.get("/api/v3/repos/MyOrganization/:repo/commits?per_page=150", (context, repo) -> getCommits(repo));
                    routes.get("/api/v3/repos/MyOrganization/:repo/commits/:commit", (context, repo, commit) -> getCommit(repo, commit));

                }
        ).start(GITHUB_MOCK_PORT);

        hasStarted = true;

        return true;
    }

    private Object getResourceFileOnRepo(String repo, String resource, String aSubDirectory, String branchName) throws IOException {

        String fileOnRepoTemplate;


        if (aSubDirectory != null) {

            fileOnRepoTemplate = FileUtils.readFileToString(ResourceUtils.getFile("classpath:template_FileOnRepo_withSubDir.json"),"UTF-8");
            fileOnRepoTemplate = fileOnRepoTemplate.replaceFirst("\\$\\{SUB_DIRECTORY}", aSubDirectory);
        }
        else{
            fileOnRepoTemplate = FileUtils.readFileToString(ResourceUtils.getFile("classpath:template_FileOnRepo.json"),"UTF-8");
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

            String pomXMlTemplate = FileUtils.readFileToString(ResourceUtils.getFile("classpath:pomXmlFileOnRepo.json"),"UTF-8");


            return new Payload("application/json", pomXMlTemplate.replaceFirst("\\$\\{REPO}", repo));
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

            String repoConfigTemplate =FileUtils.readFileToString(ResourceUtils.getFile("classpath:dummyFileOnRepo.json"),"UTF-8");



            return new Payload("application/json", repoConfigTemplate.replaceFirst("\\$\\{REPO}", repo));

        } else {
            log.info("\t .githubCrawler NOT FOUND");
            throw new NotFoundException();
        }

    }

    private Object getCommit(String repo, String commit) throws IOException {
        log.debug("Getting Github commit {} on repo {}...",commit,repo);
        return new Payload("application/json", FileUtils.readFileToString(ResourceUtils.getFile("classpath:commit.json"),"UTF-8"));

    }

    private Payload getCommits(String repo) throws IOException {
        log.debug("Getting Github commits on repo {}...",repo);
        return new Payload("application/json", FileUtils.readFileToString(ResourceUtils.getFile("classpath:commits.json"),"UTF-8"));
    }

    private Payload getTeamsMembers(String team) throws IOException {
        log.debug("Getting Github team members...");

        String teamFileName=String.format("team_members_%s.json", team.hashCode() % 2 == 0 ? "A" : "B");

        return new Payload("application/json", FileUtils.readFileToString(ResourceUtils.getFile("classpath:"+teamFileName),"UTF-8"));

    }

    private Payload getTeams() throws IOException {
        log.debug("Getting Github teams...");
        return new Payload("application/json", FileUtils.readFileToString(ResourceUtils.getFile("classpath:teams.json"),"UTF-8"));
    }

    private Payload getSearchResult(String searchQuery) throws IOException {

        log.debug("received a search query {}", searchQuery);

        searchHitsCount++;

        return new Payload("application/json", FileUtils.readFileToString(ResourceUtils.getFile("classpath:searchResult.json"),"UTF-8"));

    }

    private Object getResource(String repo, String branchName, String aSubDirectory, String resource) throws IOException {

        String pathToResource = aSubDirectory != null ? aSubDirectory + "/" + resource : resource;

        log.debug("retrieving file {} from repo {} on branch {}", pathToResource, repo, branchName);

        String fileNameToReturn= existingResources.get(buildResourceKey(repo, pathToResource));

        log.debug("actual file to return : {}", fileNameToReturn);

        if (fileNameToReturn != null) {

            log.info("\t {} found on repo {} and branch {}", pathToResource, repo, branchName);

            File fileToReturn=ResourceUtils.getFile("classpath:"+fileNameToReturn);

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

            return FileUtils.readFileToString(fileToReturn,"UTF-8");

        } else {
            log.info("\t {} NOT found on repo {} and branch {}", pathToResource, repo, branchName);
            throw new NotFoundException();
        }
    }

    private String getActualPomXML(String repo, String branchName) throws IOException {

        log.info("received a request to get pom.xml for rep {} on branch {} ..", repo, branchName);

        if (reposWithPomXml.contains(repo)) {
            log.info("\t returning something..");
            return FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_pom.xml"),"UTF-8");
        } else {
            log.info("\t pom.xml NOT FOUND");
            throw new NotFoundException();
        }
    }

    @NotNull
    private String buildResourceKey(String repo, String pathToResource) {
        return repo + "-" + pathToResource;
    }

    private Payload getBranches(String repoName) throws IOException {

        log.debug("received a branches request for repo {}..", repoName);

        InputStream is = getClass().getClassLoader().getResourceAsStream("branches.json");
        String jsonString = StreamUtils.copyToString(is, Charset.forName("UTF-8"));

        return new Payload("application/json", jsonString);
    }

    private Payload getOrganisationContentForNextPage() throws IOException {

        hasCalledNextPage = true;

        return getOrganisationContent();
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

    private Payload getOrganisationContent() throws IOException {

        log.info("fetching content of organisation...");

        InputStream is = getClass().getClassLoader().getResourceAsStream(organisationResponse);
        String jsonString = StreamUtils.copyToString(is, Charset.forName("UTF-8"));

        Payload response = new Payload("application/json", jsonString);

        if (currentPage < nbPages) {
            Map headers = response.headers();
            headers.put("link", "<http://localhost:" + GITHUB_MOCK_PORT + "/api/v3/organizations/1114/repos?page=" + currentPage + 1 +
                    ">; rel=\"next\", <http://localhost:" + GITHUB_MOCK_PORT + "/api/v3/organizations/1114/repos?page=8>; rel=\"last\"");

            currentPage++;
        }

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

}
