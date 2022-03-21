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
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest(classes = {TestConfig.class, GitHubCrawlerAutoConfiguration.class})
@ActiveProfiles(profiles = {"azureDevopsTest"})
class AzureDevopsCrawlerIT {

  private final static String API_VERSION = "api-version=7.1-preview.1";

  private final static int AZUREDEVOPS_SERVER_PORT_FOR_TESTS=9901;

  @Autowired
  private GitHubCrawler crawler;

  private final WireMockServer wm = new WireMockServer(
      options()
          .port(AZUREDEVOPS_SERVER_PORT_FOR_TESTS)
          .usingFilesUnderDirectory("src/test/resources/azureDevops"));

  @BeforeEach
  void mockSetUp() {

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

  }


  @Test
  void shouldCrawl() throws IOException {
    crawler.crawl();
  }


}
