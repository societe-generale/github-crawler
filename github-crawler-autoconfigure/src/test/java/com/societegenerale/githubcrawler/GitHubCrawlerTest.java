package com.societegenerale.githubcrawler;

import com.societegenerale.githubcrawler.config.TestConfig;
import com.societegenerale.githubcrawler.mocks.GitHubMock;
import com.societegenerale.githubcrawler.model.Repository;
import lombok.val;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.*;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TestConfig.class, GitHubCrawlerAutoConfiguration.class })
@ActiveProfiles(profiles={"test","profilesAreAWayOfGrouping"})
public class GitHubCrawlerTest {

	private final int MAX_TIMEOUT_FOR_CRAWLER=10;

	@Autowired
	private GitHubCrawler crawler;

	@Autowired
	private GitHubMock githubMockServer;

	@Autowired
	private TestConfig.InMemoryGitHubCrawlerOutput output;

	private static boolean hasGitHubMockServerStarted=false;

	private int nbRepositoriesInOrga=7;

	@Before
	public void mockSetUp(){

		if(!hasGitHubMockServerStarted) {

			githubMockServer.start();

			await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
					.until(() -> assertThat(GitHubMock.hasStarted()));

			hasGitHubMockServerStarted = true;
		}
	}

	@Before
	public void setup(){
		output.reset();
		githubMockServer.reset();
		crawler.getGitHubCrawlerProperties().setRepositoriesToExclude(new ArrayList<>());
		crawler.getGitHubCrawlerProperties().setPublishExcludedRepositories(true);
		crawler.getGitHubCrawlerProperties().setCrawlAllBranches(false);
	}



	@Test
	public void gitHubOrganisationPollerWorks() throws IOException {

		crawler.crawl();

		Collection<Repository> processedRepositories=output.getAnalyzedRepositories().values();

		assertThat(processedRepositories).hasSize(nbRepositoriesInOrga);

		assertThat(processedRepositories.stream().map(Repository::getName).collect(toSet())).hasSize(nbRepositoriesInOrga);
		assertThat(processedRepositories.stream().map(Repository::getDefaultBranch).collect(toSet())).containsOnly("master");

	}

	@Test
	public void shouldHitOrgaNextPageOfRepositoriesIfMorethanOne() throws IOException {

		int nbPages = 2;

		githubMockServer.setNbPages(nbPages);

		crawler.crawl();


        Collection<Repository> processedRepositories = output.getAnalyzedRepositories().values();

        await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
                .until(() -> assertThat(processedRepositories).hasSize(nbRepositoriesInOrga));


		assertThat(githubMockServer.isHasCalledNextPage()).as("next page wasn't called").isTrue();

		assertThat(githubMockServer.getRepoConfigHits().size()).isGreaterThanOrEqualTo(nbRepositoriesInOrga);
		assertThat(githubMockServer.getNbPages()).isEqualTo(2);

	}

	@Test
	public void excludedRepositoriesOnRepoConfigSideAreFlaggedAsExcluded() throws IOException {

		String excludedRepoName="cwf-mobile";
		githubMockServer.addRepoSideConfig(excludedRepoName, GitHubMock.REPO_EXCLUDED_CONFIG);

		crawler.crawl();

		assertOnlyThisRepoIsFlaggedAsExcluded(excludedRepoName);
	}


	@Test
	public void shouldNotPublishExcludedRepoWhenConfiguredAccordingly() throws IOException {

		crawler.getGitHubCrawlerProperties().setPublishExcludedRepositories(false);

		String excludedRepoName = "cwf-mobile";
		githubMockServer.addRepoSideConfig(excludedRepoName, GitHubMock.REPO_EXCLUDED_CONFIG);

		crawler.crawl();

		Map<String, Repository> processedRepositories = output.getAnalyzedRepositories();

		await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
				.until(() -> assertThat(processedRepositories).hasSize(nbRepositoriesInOrga-1));

		assertThat(processedRepositories).doesNotContainKeys(excludedRepoName);
	}

	@Test
	public void excludingRepositoriesOnServerConfigSideWithSingleRegexp() throws IOException {

		String excludedRepoName = "api-.*";
		crawler.getGitHubCrawlerProperties().setRepositoriesToExclude(Arrays.asList(excludedRepoName));
		crawler.crawl();

		assertOnlyThisRepoIsFlaggedAsExcluded("api-gateway");
	}

	@Test
	public void excludingRepositoriesOnServerConfigSideWithMultipleRegexp() throws IOException {

		crawler.getGitHubCrawlerProperties().setRepositoriesToExclude(Arrays.asList(".*-documentation$",
				"^(?!financing-platform-.*$).*"));

		crawler.crawl();

		SoftAssertions softly = new SoftAssertions();

		List<String> excludedRepositories = output.getAnalyzedRepositories().keySet().stream()
				.filter(repoName -> output.getAnalyzedRepositories().get(repoName).getExcluded())
				.collect(toList());


		softly.assertThat(excludedRepositories).contains("cwf-mobile", "welcome-pack", "api-gateway", "initial-load", "financing-platform-documentation");
		softly.assertThat(excludedRepositories).doesNotContain("financing-platform-deal", "financing-platform-web");

		softly.assertAll();
	}


	@Test
	public void fetchingRepoConfigForNonExcludedRepos() throws IOException {

		String excludedRepoName="api-gateway";
		crawler.getGitHubCrawlerProperties().setRepositoriesToExclude(Arrays.asList(excludedRepoName));

		crawler.crawl();

		assertThat(githubMockServer.getRepoConfigHits()).hasSize(nbRepositoriesInOrga - 1);
	}




	@Test
	public void shouldLoadIndicatorsConfig() {

		assertThat(crawler.getGitHubCrawlerProperties().getIndicatorsToFetchByFile()).hasSize(3);

		FileToParse pomXmlFile = new FileToParse("pom.xml", null);

		assertThat(crawler.getGitHubCrawlerProperties().getIndicatorsToFetchByFile()).containsKeys(pomXmlFile, new FileToParse("pom.xml", null), new FileToParse("Jenkinsfile", null), new FileToParse("Dockerfile", null));

		IndicatorDefinition pomXmlIndicatorDefinition1 = crawler.getGitHubCrawlerProperties().getIndicatorsToFetchByFile().get(pomXmlFile).get(0);
		assertThat(pomXmlIndicatorDefinition1).isNotNull();
		assertThat(pomXmlIndicatorDefinition1.getName()).isEqualTo("spring_boot_starter_parent_version");
		assertThat(pomXmlIndicatorDefinition1.getMethod()).isEqualTo("findDependencyVersionInXml");

		Map params = pomXmlIndicatorDefinition1.getParams();
		assertThat(params).hasSize(1);
		assertThat(params.get("artifactId")).isEqualTo("spring-boot-starter-parent");

		assertThat(crawler.getGitHubCrawlerProperties().getSearchesPerRepo()).hasSize(1);
	}

	@Test
	public void shouldTryToFetchFilesWithIndicators() throws IOException {

		crawler.crawl();

		await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
				.until(() -> assertThat(githubMockServer.getPomXmlHits()).hasSize(nbRepositoriesInOrga));

	}

	@Test
    public void shouldCopyTagsFromRepoConfigOnRepoResult() throws IOException {

		String repoWithConfig = "api-gateway";

		String repoConfig = "tags:\n" +
				"  - \"someTag1\"\n" +
				"  - \"someOtherTag\"";

		githubMockServer.addRepoSideConfig(repoWithConfig, repoConfig);

		crawler.crawl();

		Collection<Repository> processedRepositories = output.getAnalyzedRepositories().values();

		await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
				.until(() -> assertThat(processedRepositories).hasSize(nbRepositoriesInOrga));

		processedRepositories.stream().filter(repo -> repo.getName().equals(repoWithConfig)).forEach(repo -> {
			assertThat(repo.getTags()).containsExactlyInAnyOrder("someTag1", "someOtherTag");
		});

		assertThat(processedRepositories.stream().filter(repo -> repo.getName().equals(repoWithConfig)).count()).isEqualTo(1);
	}

	@Test
	public void shouldCopyActiveProfilesAsGroupsOnRepo() throws IOException {

		crawler.crawl();

        Collection<Repository> processedRepositories = output.getAnalyzedRepositories().values();

        await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
                .until(() -> assertThat(processedRepositories).hasSize(nbRepositoriesInOrga));

		processedRepositories.stream().forEach(repo -> {
			assertThat(repo.getGroups()).containsExactlyInAnyOrder("test","profilesAreAWayOfGrouping");
		});

	}


	@Test
	public void shouldNotFetchFilesWithIndicatorsIfRepoIsExcluded() throws IOException {

		String excludedRepoName = "api-gateway";
		githubMockServer.addRepoSideConfig(excludedRepoName, GitHubMock.REPO_EXCLUDED_CONFIG);

		crawler.crawl();

		assertThat(githubMockServer.getPomXmlHits()).hasSize(nbRepositoriesInOrga - 1);
		assertThat(githubMockServer.getPomXmlHits()).doesNotContain(excludedRepoName);
	}

	@Test
    public void shouldParseFileWithIndicatorsWhenItExists() throws IOException {

		String repoWithPomXml = "api-gateway";
		githubMockServer.addReposWithPomXMl(Arrays.asList(repoWithPomXml));

		crawler.crawl();

		await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
				.until(() -> assertThat(output.getAnalyzedRepositories().values()).hasSize(nbRepositoriesInOrga));

		Collection<Repository> processedRepositories = output.getAnalyzedRepositories().values();


		String defaultBranch = "master";

        for (Repository processedRepository : processedRepositories) {

			Map<String, String> parsedIndicators = processedRepository.getIndicatorsForBranch(defaultBranch);

            if (processedRepository.getName().equals(repoWithPomXml)) {

				assertThat(parsedIndicators).as("no indicators found for branch with name '" + defaultBranch + "' on repo " + processedRepository.getName()).isNotEmpty();

				assertThat(parsedIndicators.get("spring_boot_starter_parent_version")).as("for repo " + processedRepository.getName())
						.isEqualTo("1.5.9.RELEASE");

            } else {
				assertThat(parsedIndicators.get("spring_boot_starter_parent_version")).as("for repo " + processedRepository.getName()).isNull();
			}
        }
    }

	@Test
	public void shouldParseFile_withRedirection_WhenItExists() throws IOException {

		String redirection = "moduleWhereDockerFileIs/Dockerfile";

		String repoConfig = "filesToParse:\n" +
				"  - name: \"Dockerfile\"\n" +
				"    redirectTo: \"" + redirection + "\"";

		String repoWithConfig = "api-gateway";
		githubMockServer.addRepoSideConfig(repoWithConfig, repoConfig);

        githubMockServer.addExistingResource(repoWithConfig, redirection, "sample_Dockerfile");

		crawler.crawl();

		Collection<Repository> processedRepositories = output.getAnalyzedRepositories().values();
		assertThat(processedRepositories).hasSize(nbRepositoriesInOrga);

		String indicatorName = "docker_image_used";

		val indicatorValueFoundInFilePostRedirection = processedRepositories.stream().filter(repo -> repo.getName().equals(repoWithConfig))
				.map(repo -> repo.getIndicatorsForBranch("master"))
				.filter(indicators -> indicators.containsKey(indicatorName))
				.map(indicators -> indicators.get(indicatorName))
				.findAny();

		assertThat(indicatorValueFoundInFilePostRedirection.isPresent()).isTrue();
		assertThat(indicatorValueFoundInFilePostRedirection.get()).isEqualTo("someImageName:20171206-162536-e136a81");

	}

	@Test
	public void shouldParseAllBranchesWhenConfiguredAccordingly() throws IOException {

		String repoWithPomXml = "api-gateway";
		githubMockServer.addReposWithPomXMl(Arrays.asList(repoWithPomXml));

		crawler.getGitHubCrawlerProperties().setCrawlAllBranches(true);

		crawler.crawl();

		List<String> existingBranches = Arrays.asList("debug", "uat", "hom", "int", "production");

		Collection<Repository> processedRepositories = output.getAnalyzedRepositories().values();

		await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
				.until(() -> assertThat(processedRepositories).hasSize(nbRepositoriesInOrga));

		for (Repository processedRepository : processedRepositories) {

			for (String branchName : existingBranches) {

				Map<String, String> parsedIndicators = processedRepository.getIndicatorsForBranch(branchName);

				if (processedRepository.getName().equals(repoWithPomXml)) {

					assertThat(parsedIndicators).as("no indicators found for branch with name '" + branchName + "' on repo " + processedRepository.getName()).isNotEmpty();

					assertThat(parsedIndicators.get("spring_boot_starter_parent_version")).as("for repo " + processedRepository.getName())
							.isEqualTo("1.5.9.RELEASE");

				} else {
					assertThat(parsedIndicators.get("spring_boot_starter_parent_version")).as("for repo " + processedRepository.getName()).isNull();
				}
			}
		}
	}

	@Test
	public void processNormallyIfNoConfigFileOnRepoSide() throws IOException {

		githubMockServer.addReposWithNoConfig(Arrays.asList("api-gateway"));

		crawler.crawl();

		Collection<Repository> actualRepositories= output.getAnalyzedRepositories().values();
		await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
				.until(() -> assertThat(actualRepositories).hasSize(nbRepositoriesInOrga));


		assertThat(githubMockServer.getRepoConfigHits()).hasSize(nbRepositoriesInOrga);


		assertThat(actualRepositories.stream().map(Repository::getExcluded).collect(toSet())).containsOnly(false);

		Set allReasons = actualRepositories.stream().map(Repository::getReason).collect(toSet());

		assertThat(allReasons).hasSize(1);
		assertThat(allReasons).containsNull();
	}

	@Test
	public void shouldPerformSearchOnAllRepos() throws IOException {

		crawler.crawl();

		Collection<Repository> actualRepositories= output.getAnalyzedRepositories().values();
		await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
				.until(() -> assertThat(actualRepositories).hasSize(nbRepositoriesInOrga));

		assertThat(githubMockServer.getSearchHitsCount()).isEqualTo(nbRepositoriesInOrga);

		actualRepositories.stream().forEach( repo -> {

			assertThat(repo.getSearchResults()).hasSize(1);
			assertThat(repo.getSearchResults().get("nbOfMetricsInPomXml")).isEqualTo("2");

		});


	}


	@Test
	public void outputResultsShouldHaveACrawlerRunId() throws IOException {

		crawler.crawl();

		Collection<Repository> actualRepositories= output.getAnalyzedRepositories().values();
		await().atMost(MAX_TIMEOUT_FOR_CRAWLER, SECONDS)
				.until(() -> assertThat(actualRepositories).hasSize(nbRepositoriesInOrga));

		assertThat(actualRepositories.stream().filter(r -> !r.getCrawlerRunId().equals(GitHubCrawler.NO_CRAWLER_RUN_ID_DEFINED))
											  .count())
				.isEqualTo(nbRepositoriesInOrga);
	}

	private void assertOnlyThisRepoIsFlaggedAsExcluded(String excludedRepoName) {
		SoftAssertions softly = new SoftAssertions();

		Map<String,Repository> processedRepositories=output.getAnalyzedRepositories();
		softly.assertThat(processedRepositories.get(excludedRepoName).getExcluded()).as("repo " + excludedRepoName + " should be excluded, but is not").isTrue();

		List<Repository> expectedNonExcludedRepo=processedRepositories.values().stream().filter(r -> !r.getName().equals(excludedRepoName)).collect(toList());
		softly.assertThat(expectedNonExcludedRepo.stream().map(Repository::getExcluded).collect(toSet())).containsOnly(false);

		softly.assertAll();
	}

}
