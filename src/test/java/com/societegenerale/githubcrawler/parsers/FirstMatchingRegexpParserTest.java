package com.societegenerale.githubcrawler.parsers;

import com.societegenerale.githubcrawler.IndicatorDefinition;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.societegenerale.githubcrawler.TestUtils.readFromInputStream;
import static com.societegenerale.githubcrawler.parsers.FirstMatchingRegexpParser.FIND_FIRST_VALUE_WITH_REGEXP_CAPTURE_METHOD;
import static com.societegenerale.githubcrawler.parsers.FirstMatchingRegexpParser.PATTERN;
import static org.assertj.core.api.Assertions.assertThat;

public class FirstMatchingRegexpParserTest {

    final String indicatorName = "someIndicatorName";
    //TODO pattern can probably be improved to match even if there are extra blank characters after the image name, but match only the image name
    private final String regexForDockerImage = "FROM\\s.*\\/(.*)\\s?.*";
    FirstMatchingRegexpParser fileContentParser = new FirstMatchingRegexpParser();
    IndicatorDefinition firstMatchingRegexpToFind = new IndicatorDefinition();
    Map<String, String> params = new HashMap<>();

    @Before
    public void setup() {

        firstMatchingRegexpToFind.setName(indicatorName);
        firstMatchingRegexpToFind.setMethod(FIND_FIRST_VALUE_WITH_REGEXP_CAPTURE_METHOD);
    }

    @Test
    public void shouldFindValueBasedOnRegexp() {

        params.put(PATTERN, "(?s).*com\\.sgcib\\.fcc\\.osd\\.([a-z]*)\\.build\\.BuildHelpers.*");

        firstMatchingRegexpToFind.setParams(params);

        String fileContent = "test\nimport static com.sgcib.fcc.osd.cap.build.BuildHelpers.*";

        Map<String, String> indicatorsFound = fileContentParser.parseFileContentForIndicator(fileContent,StringUtils.EMPTY, firstMatchingRegexpToFind);

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("cap");
    }

    @Test
    public void shouldFindValueBasedOnRegexp2() throws Exception {

        params.put(PATTERN, "(?s).*dockerNode\\(image:'(.+?(?=')).*");
        firstMatchingRegexpToFind.setParams(params);

        String fileContent = readFromInputStream(getClass().getResourceAsStream("/sample_Jenkinsfile2"));

        Map<String, String> indicatorsFound = fileContentParser.parseFileContentForIndicator(fileContent,StringUtils.EMPTY, firstMatchingRegexpToFind);

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("myDtr.com/someOrg/someImageName:1.0");
    }

    @Test
    public void canFindDockerImageInDockerFile() throws Exception {

        Map<String, String> indicatorsFound = findDockerImageIndicatorIn("/sample_Dockerfile");

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("someImageName:20171206-162536-e136a81");
    }

    @Test
    public void canFindDockerImageInMultilineDockerFile() throws Exception {

        Map<String, String> indicatorsFound = findDockerImageIndicatorIn("/sample_multilineDockerfile");

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("someImageName:7.2");
    }

    @Test
    public void canFindValueInDockerStackFile() throws Exception {

        String sampleDockerStackFile= readFromInputStream(getClass().getResourceAsStream("/sample_dockerStack"));

        params.put(PATTERN, "(?m).*ZIPKIN_BASE_URL=(.*)$");
        firstMatchingRegexpToFind.setParams(params);

        Map<String, String> indicatorsFound =fileContentParser.parseFileContentForIndicator(sampleDockerStackFile,StringUtils.EMPTY,firstMatchingRegexpToFind);

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("http://someZipkinUrl.com");
    }

    @Test
    public void shouldDefaultIfNotConfiguredProperly_noCapturingGroup() throws Exception {

        String sampleDockerStackFile= readFromInputStream(getClass().getResourceAsStream("/sample_dockerStack"));

        params.put(PATTERN, "(?m).*ZIPKIN_BASE_URL=.*$");
        firstMatchingRegexpToFind.setParams(params);

        Map<String, String> indicatorsFound =fileContentParser.parseFileContentForIndicator(sampleDockerStackFile,StringUtils.EMPTY, firstMatchingRegexpToFind);

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("issue in config, check logs");
    }

    private Map<String, String> findDockerImageIndicatorIn(String fileName) throws IOException {

        String sampleJenkinsFile = readFromInputStream(getClass().getResourceAsStream(fileName));

        params.put(PATTERN, regexForDockerImage);
        firstMatchingRegexpToFind.setParams(params);

        return fileContentParser.parseFileContentForIndicator(sampleJenkinsFile, StringUtils.EMPTY,firstMatchingRegexpToFind);
    }
}
