package com.societegenerale.githubcrawler;

import com.societegenerale.githubcrawler.config.GitHubCrawlerAutoConfiguration;
import com.societegenerale.githubcrawler.config.TestConfig;
import com.societegenerale.githubcrawler.mocks.BitbucketMock;
import com.societegenerale.githubcrawler.model.Repository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.*;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class, GitHubCrawlerAutoConfiguration.class})
@ActiveProfiles(profiles = {"bitBucketTest"})
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
public class BitbucketCrawlerIT {

  private final int MAX_TIMEOUT_FOR_CRAWLER = 50;

  @Autowired
  private GitHubCrawler crawler;

  private static BitbucketMock bitbucketMockServer;

  @Autowired
  private BitbucketMock tmpBitbucketMockServer;

  @Autowired
  private TestConfig.InMemoryGitHubCrawlerOutput output;

  private static boolean hasBitbucketMockServerStarted = false;

  private int nbRepositoriesInOrga = 4;

  @BeforeEach
  public void mockSetUp() {

    //we need a bitbucketMockServer to be static, so that we can stop it once at the end in the @AfterClass method
    //but at the same time, we need a version of it that is configured by Spring, so can't be static

    //therefore we have small hack below, where we take the reference of the BitBucketMock configured and injected by Spring,
    //and assign it to the static variable the first time, when the static reference is null

    if (bitbucketMockServer == null) {
      bitbucketMockServer = tmpBitbucketMockServer;
    }

    if (!hasBitbucketMockServerStarted) {

      bitbucketMockServer.start();

      await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
          .until(() -> assertThat(BitbucketMock.hasStarted()));

      hasBitbucketMockServerStarted = true;
    }

    bitbucketMockServer.reset();
  }

  @BeforeEach
  void resetSharedData() {
    output.reset();

    crawler.getGitHubCrawlerProperties().setRepositoriesToExclude(new ArrayList<>());
    crawler.getGitHubCrawlerProperties().setPublishExcludedRepositories(true);
    crawler.getGitHubCrawlerProperties().setCrawlAllBranches(false);
    crawler.getTasksToPerform().clear();
  }


  @AfterAll
  public static void shutDown() {
    bitbucketMockServer.stop();
  }

  @Test
  void shouldHitOrgaNextPageOfRepositoriesIfMorethanOne() throws IOException {

    int nbPages = 2;

    bitbucketMockServer.setNbPages(nbPages);

    crawler.crawl();

    Collection<Repository> processedRepositories = output.getAnalyzedRepositories().values();

    await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
        .until(() -> assertThat(processedRepositories).hasSize(nbRepositoriesInOrga));

    assertThat(bitbucketMockServer.isHasCalledNextPage()).as("next page wasn't called").isTrue();

    assertThat(bitbucketMockServer.getRepoConfigHits()).hasSizeGreaterThanOrEqualTo(nbRepositoriesInOrga);
    assertThat(bitbucketMockServer.getNbPages()).isEqualTo(2);

  }

  @Test
  void excludingRepositoriesOnServerConfigSideWithSingleRegexp() throws IOException {

    String excludedRepoName = "api-.*";
    crawler.getGitHubCrawlerProperties().setRepositoriesToExclude(List.of(excludedRepoName));
    crawler.crawl();

    assertOnlyThisRepoIsFlaggedAsExcluded("api-gateway");
  }

  @Test
  void shouldLoadIndicatorsConfig() {

    assertThat(crawler.getGitHubCrawlerProperties().getIndicatorsToFetchByFile()).hasSize(3);

    FileToParse pomXmlFile = new FileToParse("pom.xml", null);

    assertThat(crawler.getGitHubCrawlerProperties().getIndicatorsToFetchByFile()).containsKeys(pomXmlFile, new FileToParse("pom.xml", null),
        new FileToParse("Jenkinsfile", null), new FileToParse("Dockerfile", null));

    IndicatorDefinition pomXmlIndicatorDefinition1 = crawler.getGitHubCrawlerProperties().getIndicatorsToFetchByFile().get(pomXmlFile).get(0);
    assertThat(pomXmlIndicatorDefinition1).isNotNull();
    assertThat(pomXmlIndicatorDefinition1.getName()).isEqualTo("spring_boot_starter_parent_version");
    assertThat(pomXmlIndicatorDefinition1.getType()).isEqualTo("findDependencyVersionInXml");

    Map params = pomXmlIndicatorDefinition1.getParams();
    assertThat(params).hasSize(1);
    assertThat(params).containsEntry("artifactId","spring-boot-starter-parent");

  }

  @Test
  void shouldTryToFetchFilesWithIndicators() throws IOException {

    crawler.crawl();

    await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
        .until(() -> assertThat(bitbucketMockServer.getPomXmlHits()).hasSize(nbRepositoriesInOrga));

  }

  @Test
  void shouldCopyActiveProfilesAsGroupsOnRepo() throws IOException {

    crawler.crawl();

    Collection<Repository> processedRepositories = output.getAnalyzedRepositories().values();

    await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
        .until(() -> assertThat(processedRepositories).hasSize(nbRepositoriesInOrga));

    processedRepositories.stream().forEach(repo -> {
      assertThat(repo.getGroups()).containsExactlyInAnyOrder("bitBucketTest");
    });

  }

  @Test
  void shouldNotFetchFilesWithIndicatorsIfRepoIsExcluded() throws IOException {

    String excludedRepoName = "api-gateway";
    bitbucketMockServer.addRepoSideConfig(excludedRepoName, BitbucketMock.REPO_EXCLUDED_CONFIG);

    crawler.crawl();

    assertThat(bitbucketMockServer.getPomXmlHits())
        .hasSize(nbRepositoriesInOrga - 1)
        .doesNotContain(excludedRepoName);
  }

  @Test
  void shouldParseFileWithIndicatorsWhenItExists() throws IOException {

    String repoWithPomXml = "api-gateway";
    bitbucketMockServer.addReposWithPomXMl(Arrays.asList(repoWithPomXml));

    crawler.crawl();

    await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
        .until(() -> assertThat(output.getAnalyzedRepositories().values()).hasSize(nbRepositoriesInOrga));

    Collection<Repository> processedRepositories = output.getAnalyzedRepositories().values();

    String defaultBranch = "master";

    for (Repository processedRepository : processedRepositories) {

      Map<String, Object> parsedIndicators = processedRepository.getIndicatorsForBranch(defaultBranch);

      if (processedRepository.getName().equals(repoWithPomXml)) {

        assertThat(parsedIndicators).as("no indicators found for branch with name '" + defaultBranch + "' on repo " + processedRepository.getName())
            .isNotEmpty();

        assertThat(parsedIndicators).as("for repo " + processedRepository.getName())
            .containsEntry("spring_boot_starter_parent_version","1.5.9.RELEASE");

      } else {
        assertThat(parsedIndicators.get("spring_boot_starter_parent_version")).as("for repo " + processedRepository.getName()).isNull();
      }
    }
  }

  @Test
  void shouldParseFile_withRedirection_WhenItExists() throws IOException {

    String redirection = "moduleWhereDockerFileIs/Dockerfile";

    String repoConfig = "filesToParse:\n" +
        "  - name: \"Dockerfile\"\n" +
        "    redirectTo: \"" + redirection + "\"";

    String repoWithConfig = "api-gateway";
    bitbucketMockServer.addRepoSideConfig(repoWithConfig, repoConfig);

    bitbucketMockServer.addExistingResource(repoWithConfig, redirection, "sample_Dockerfile");

    crawler.crawl();

    Collection<Repository> processedRepositories = output.getAnalyzedRepositories().values();
    assertThat(processedRepositories).hasSize(nbRepositoriesInOrga);

    String indicatorName = "docker_image_used";

    Optional<Object> indicatorValueFoundInFilePostRedirection = processedRepositories.stream().filter(repo -> repo.getName().equals(repoWithConfig))
        .map(repo -> repo.getIndicatorsForBranch("master"))
        .filter(indicators -> indicators.containsKey(indicatorName))
        .map(indicators -> indicators.get(indicatorName))
        .findAny();

    assertThat(indicatorValueFoundInFilePostRedirection).contains("someImageName:20171206-162536-e136a81");

  }

  @Test
  void shouldParseAllBranchesWhenConfiguredAccordingly() throws IOException {

    String repoWithPomXml = "api-gateway";
    bitbucketMockServer.addReposWithPomXMl(Arrays.asList(repoWithPomXml));

    crawler.getGitHubCrawlerProperties().setCrawlAllBranches(true);

    crawler.crawl();

    List<String> existingBranches = Arrays.asList("debug", "uat", "hom", "int", "production");

    Collection<Repository> processedRepositories = output.getAnalyzedRepositories().values();

    await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
        .until(() -> assertThat(processedRepositories).hasSize(nbRepositoriesInOrga));

    for (Repository processedRepository : processedRepositories) {

      for (String branchName : existingBranches) {

        Map<String, Object> parsedIndicators = processedRepository.getIndicatorsForBranch(branchName);

        if (processedRepository.getName().equals(repoWithPomXml)) {

          assertThat(parsedIndicators).as("no indicators found for branch with name '" + branchName + "' on repo " + processedRepository.getName())
              .isNotEmpty();

          assertThat(parsedIndicators).as("for repo " + processedRepository.getName())
              .containsEntry("spring_boot_starter_parent_version","1.5.9.RELEASE");

        } else {
          assertThat(parsedIndicators.get("spring_boot_starter_parent_version")).as("for repo " + processedRepository.getName()).isNull();
        }
      }
    }
  }

  @Test
  void processNormallyIfNoConfigFileOnRepoSide() throws IOException {

    bitbucketMockServer.addReposWithNoConfig(Arrays.asList("api-gateway"));

    crawler.crawl();

    Collection<Repository> actualRepositories = output.getAnalyzedRepositories().values();
    await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
        .until(() -> assertThat(actualRepositories).hasSize(nbRepositoriesInOrga));

    assertThat(bitbucketMockServer.getRepoConfigHits()).hasSize(nbRepositoriesInOrga);

    assertThat(actualRepositories.stream().map(Repository::getExcluded).collect(toSet())).containsOnly(false);

    Set allReasons = actualRepositories.stream().map(Repository::getReason).collect(toSet());

    assertThat(allReasons).hasSize(1);
    assertThat(allReasons).containsNull();
  }

  @Test
  void outputResultsShouldHaveACrawlerRunId() throws IOException {

    crawler.crawl();

    Collection<Repository> actualRepositories = output.getAnalyzedRepositories().values();
    await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
        .until(() -> assertThat(actualRepositories).hasSize(nbRepositoriesInOrga));

    assertThat(actualRepositories.stream().filter(r -> !r.getCrawlerRunId().equals(GitHubCrawler.NO_CRAWLER_RUN_ID_DEFINED))
        .count())
        .isEqualTo(nbRepositoriesInOrga);
  }

  private void assertOnlyThisRepoIsFlaggedAsExcluded(String excludedRepoName) {
    SoftAssertions softly = new SoftAssertions();

    Map<String, Repository> processedRepositories = output.getAnalyzedRepositories();
    softly.assertThat(processedRepositories.get(excludedRepoName).getExcluded()).as("repo " + excludedRepoName + " should be excluded, but is not").isTrue();

    List<Repository> expectedNonExcludedRepo = processedRepositories.values().stream().filter(r -> !r.getName().equals(excludedRepoName)).collect(toList());
    softly.assertThat(expectedNonExcludedRepo.stream().map(Repository::getExcluded).collect(toSet())).containsOnly(false);

    softly.assertAll();
  }

}