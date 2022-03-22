package com.societegenerale.githubcrawler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.societegenerale.githubcrawler.config.GitHubCrawlerAutoConfiguration;
import com.societegenerale.githubcrawler.config.TestConfig;
import com.societegenerale.githubcrawler.remote.RemoteGitHub;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.ResourceUtils;


@SpringBootTest(classes = {TestConfig.class, GitHubCrawlerAutoConfiguration.class})
@ActiveProfiles(profiles = {"azureDevopsTest"})
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class AzureDevopsCrawlerIT {

  Logger log = LoggerFactory.getLogger(this.getClass().toString());

  private final static String API_VERSION = "api-version=7.1-preview.1";

  private final static int AZUREDEVOPS_SERVER_PORT_FOR_TESTS=9901;

  @Autowired
  ApplicationContext context;

  @Autowired
  private GitHubCrawler crawler;

  private final WireMockServer wm = new WireMockServer(
      options()
          .port(AZUREDEVOPS_SERVER_PORT_FOR_TESTS)
          .usingFilesUnderDirectory("src/test/resources/azureDevops"));

  @BeforeEach
  void mockSetUp() throws IOException {

    wm.start();
    configureFor(AZUREDEVOPS_SERVER_PORT_FOR_TESTS);

    stubFor(WireMock.get(urlEqualTo("/platform/platform-projects/_apis/git/repositories?"+API_VERSION))
        .willReturn(aResponse()
            .withBodyFile("repositories.json")
            .withStatus(200)));

    stubFor(WireMock.get(urlEqualTo("/platform/platform-projects/_apis/git/repositories/my-helm-chart/items?path=.azureDevopsCrawler&"+API_VERSION))
        .willReturn(aResponse().withStatus(404)));

    stubFor(WireMock.get(urlEqualTo("/platform/platform-projects/_apis/git/repositories/vendor-portal-ui/items?path=.azureDevopsCrawler&"+API_VERSION))
        .willReturn(aResponse().withStatus(404)));

    stubFor(WireMock.get(urlEqualTo("/platform/platform-projects/_apis/git/repositories/my-helm-chart/items?path=pom.xml&versionDescriptor.versionType=branch&versionDescriptor.version=main&"+API_VERSION))
        .willReturn((aResponse()
            .withBody(FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_pom.xml"), "UTF-8"))
            .withStatus(200))));

    stubFor(WireMock.get(urlEqualTo("/platform/platform-projects/_apis/git/repositories/vendor-portal-ui/items?path=pom.xml&versionDescriptor.versionType=branch&versionDescriptor.version=main&"+API_VERSION))
        .willReturn(aResponse().withStatus(404)));
  }


  @Test
  void shouldCrawl() throws IOException {

    var crawlers=context.getBeanNamesForType(GitHubCrawler.class);
    log.info("found "+crawlers.length+" crawlers in context..");

    var remoteGitHubs=context.getBeanNamesForType(RemoteGitHub.class);
    log.info("found "+remoteGitHubs.length+" remoteGitHubs in context..");


    log.info("about to start crawling...");

    log.info("URL config used : "+crawler.getGitHubCrawlerProperties().getGithubConfig().getApiUrl());

    log.info("nb misc tasks : "+crawler.getGitHubCrawlerProperties().getMiscRepositoryTasks().size());


    try {
      crawler.crawl();
    } catch (IOException e) {
      log.error("problem while crawling",e);
      throw e;
    }


    //TODO have an in-memory output and assert content
  }


}
