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

import static com.societegenerale.githubcrawler.parsers.YamlParserForPropertyValue.FIND_PROPERTY_VALUE_IN_YAML;
import static com.societegenerale.githubcrawler.parsers.YamlParserForPropertyValue.PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class YamlParserForPropertyValueTest {

    final String indicatorName = "someIndicatorName";
    String yamlFileSnippet;
    Map<String, String> params = new HashMap<>();
    IndicatorDefinition pomXmlDependencyVersion = new IndicatorDefinition(indicatorName,FIND_PROPERTY_VALUE_IN_YAML,params);

    YamlParserForPropertyValue fileContentParser = new YamlParserForPropertyValue();

    @Before
    public void setup() throws IOException {

        yamlFileSnippet=
                FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_yamlfile.yml"), "UTF-8");
    }

    @Test
    public void canFindAsimplePropertyValue() {

        Map<String, String> indicatorsFound = parseSampleYamlForProperty("interesting.property");

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("myExpectedValue");
    }

    @Test
    public void canFindAsimpleNestedPropertyValue() {

        Map<String, String> indicatorsFound = parseSampleYamlForProperty("interesting.nested.property");

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("myExpectedNestedValue");
    }

    @Test
    public void canFindAbooleanProperty() {

        Map<String, String> indicatorsFound = parseSampleYamlForProperty("boolean.property");

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("true");
    }

    @Test
    public void canFindPropertyIfNotInFirstDocument() {

        Map<String, String> indicatorsFound = parseSampleYamlForProperty("server.ssl.key-store");

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("hello");
    }

    @Test
    public void canFindSemiNestedProperty() {

        Map<String, String> indicatorsFound = parseSampleYamlForProperty("interesting.semiNested.property");

        assertThat(indicatorsFound.get(indicatorName)).isEqualTo("semiNestedValue");
    }

    @Test
    public void yieldsIssueWhileParsingIfError() throws IOException {

        params.put(PROPERTY_NAME, "anything");
        pomXmlDependencyVersion.setParams(params);

        Map<String, String> indicatorsFound = fileContentParser.parseFileContentForIndicator(FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_invalid_yamlfile.yml"), "UTF-8"),
                                                                                             StringUtils.EMPTY,
                                                                                            pomXmlDependencyVersion);

        assertThat(indicatorsFound.get(indicatorName)).startsWith("issue while parsing ");
    }

    private Map<String, String> parseSampleYamlForProperty(String propertyName) {
        params.put(PROPERTY_NAME, propertyName);
        pomXmlDependencyVersion.setParams(params);

        return fileContentParser.parseFileContentForIndicator(yamlFileSnippet,StringUtils.EMPTY, pomXmlDependencyVersion);

    }



}
