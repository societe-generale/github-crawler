package com.societegenerale.githubcrawler.parsers;

import com.societegenerale.githubcrawler.IndicatorDefinition;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.societegenerale.githubcrawler.parsers.PomXmlParserForDependencyVersion.ARTIFACT_ID;
import static com.societegenerale.githubcrawler.parsers.PomXmlParserForDependencyVersion.FIND_DEPENDENCY_VERSION_IN_XML_METHOD;
import static org.assertj.core.api.Assertions.assertThat;

public class PomXmlParserForDependencyVersionTest {

    final String indicatorName = "someIndicatorName";
    String pomXmlSnippet;
    Map<String, String> params = new HashMap<>();
    IndicatorDefinition pomXmlDependencyVersion = new IndicatorDefinition(indicatorName,FIND_DEPENDENCY_VERSION_IN_XML_METHOD,params);
    PomXmlParserForDependencyVersion fileContentParser = new PomXmlParserForDependencyVersion();

    @Before
    public void setup() throws IOException {

        pomXmlSnippet =  FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_pom.xml"),"UTF-8");

    }

    @Test
    public void canFindAdependencyVersionInPomXml() {

        Map<String, String> indicatorsFound = parseSamplePomXmlForArtifactId("spring-boot-starter-parent");

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("1.5.9.RELEASE");
    }

    @Test
    public void shouldReport_notFound_WhenArtifactIsNotFound() {

        Map<String, String> indicatorsFound = parseSamplePomXmlForArtifactId("spring-not-found-parent");

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("not found");
    }

    @Test
    public void shouldReport_versionNotFound_WhenArtifactIsFoundButNoeTheVersion() throws Exception {

        Map<String, String> indicatorsFound = parseSamplePomXmlForArtifactId("weird-artifact-with-version-declared-before");

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("artifact found, but not the version");
    }

    @Test
    public void shouldReportVersionEvenWhenDefinedAsProperty() throws Exception {

        Map<String, String> indicatorsFound = parseSamplePomXmlForArtifactId("jackson-dataformat-yaml");

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("2.9.2");
    }

    private Map<String, String> parseSamplePomXmlForArtifactId(String artifactId) {
        params.put(ARTIFACT_ID, artifactId);
        pomXmlDependencyVersion.setParams(params);

        return fileContentParser.parseFileContentForIndicator(pomXmlSnippet, StringUtils.EMPTY, pomXmlDependencyVersion);

    }

}
