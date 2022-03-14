package com.societegenerale.githubcrawler.parsers;

import static com.societegenerale.githubcrawler.parsers.FirstMatchingRegexpParser.FIND_FIRST_VALUE_WITH_REGEXP_CAPTURE_METHOD;
import static com.societegenerale.githubcrawler.parsers.FirstMatchingRegexpParser.PATTERN;
import static org.assertj.core.api.Assertions.assertThat;

import com.societegenerale.githubcrawler.IndicatorDefinition;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

class FirstMatchingRegexpParserTest {

    final String indicatorName = "someIndicatorName";

    //TODO pattern can probably be improved to match even if there are extra blank characters after the image name, but match only the image name
    private final String regexForDockerImage = "FROM\\s.*\\/(.*)\\s?.*";

    FirstMatchingRegexpParser fileContentParser = new FirstMatchingRegexpParser();

    Map<String, String> params = new HashMap<>();

    IndicatorDefinition firstMatchingRegexpToFind = new IndicatorDefinition(indicatorName,FIND_FIRST_VALUE_WITH_REGEXP_CAPTURE_METHOD,params);


    @Test
    void shouldFindValueBasedOnRegexp() {

        params.put(PATTERN, "(?s).*com\\.sgcib\\.fcc\\.osd\\.([a-z]*)\\.build\\.BuildHelpers.*");

        firstMatchingRegexpToFind.setParams(params);

        String fileContent = "test\nimport static com.sgcib.fcc.osd.cap.build.BuildHelpers.*";

        Map<String, String> indicatorsFound = fileContentParser
                .parseFileContentForIndicator(fileContent, StringUtils.EMPTY, firstMatchingRegexpToFind);

        assertThat(indicatorsFound).containsEntry(indicatorName,"cap");
    }

    @Test
    void shouldFindValueBasedOnRegexp_gradleVersion() throws Exception {

        params.put(PATTERN, "(?m)^distributionUrl=.*\\/([^\\/\\s]+$)$");
        firstMatchingRegexpToFind.setParams(params);

        String fileContent = FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_gradleWrapper.properties"), "UTF-8");

        Map<String, String> indicatorsFound = fileContentParser
                .parseFileContentForIndicator(fileContent, StringUtils.EMPTY, firstMatchingRegexpToFind);

        assertThat(indicatorsFound).containsEntry(indicatorName,"gradle-3.3-bin.zip");
    }

    @Test
    void shouldFindValueBasedOnRegexp2() throws Exception {

        params.put(PATTERN, "(?s).*dockerNode\\(image:'(.+?(?=')).*");
        firstMatchingRegexpToFind.setParams(params);

        String fileContent = FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_Jenkinsfile2"), "UTF-8");

        Map<String, String> indicatorsFound = fileContentParser
                .parseFileContentForIndicator(fileContent, StringUtils.EMPTY, firstMatchingRegexpToFind);

        assertThat(indicatorsFound).containsEntry(indicatorName,"myDtr.com/someOrg/someImageName:1.0");
    }


    @Test
    void canFindDockerImageInDockerFile() throws Exception {

        Map<String, String> indicatorsFound = findDockerImageIndicatorIn("sample_Dockerfile");

        assertThat(indicatorsFound).containsEntry(indicatorName,"someImageName:20171206-162536-e136a81");
    }

    @Test
    void canFindDockerImageInMultilineDockerFile() throws Exception {

        Map<String, String> indicatorsFound = findDockerImageIndicatorIn("sample_multilineDockerfile");

        assertThat(indicatorsFound).containsEntry(indicatorName,"someImageName:7.2");
    }

    @Test
    void canFindValueInDockerStackFile() throws Exception {

        String sampleDockerStackFile = FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_dockerStack"), "UTF-8");

        params.put(PATTERN, "(?m).*ZIPKIN_BASE_URL=(.*)$");
        firstMatchingRegexpToFind.setParams(params);

        Map<String, String> indicatorsFound = fileContentParser
                .parseFileContentForIndicator(sampleDockerStackFile, StringUtils.EMPTY, firstMatchingRegexpToFind);

        assertThat(indicatorsFound).containsEntry(indicatorName,"http://someZipkinUrl.com");
    }

    @Test
    void shouldDefaultIfNotConfiguredProperly_noCapturingGroup() throws Exception {

        String sampleDockerStackFile = FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_dockerStack"), "UTF-8");

        params.put(PATTERN, "(?m).*ZIPKIN_BASE_URL=.*$");
        firstMatchingRegexpToFind.setParams(params);

        Map<String, String> indicatorsFound = fileContentParser
                .parseFileContentForIndicator(sampleDockerStackFile, StringUtils.EMPTY, firstMatchingRegexpToFind);

        assertThat(indicatorsFound).containsEntry(indicatorName,"issue in config, check logs");
    }

    private Map<String, String> findDockerImageIndicatorIn(String fileName) throws IOException {

        String sampleJenkinsFile = FileUtils.readFileToString(ResourceUtils.getFile("classpath:"+fileName), "UTF-8");


        params.put(PATTERN, regexForDockerImage);
        firstMatchingRegexpToFind.setParams(params);

        return fileContentParser.parseFileContentForIndicator(sampleJenkinsFile, StringUtils.EMPTY, firstMatchingRegexpToFind);
    }
}
