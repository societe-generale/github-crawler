package com.societegenerale.githubcrawler.parsers;

import static com.societegenerale.githubcrawler.parsers.YamlParserForPropertyValue.FIND_PROPERTY_VALUE_IN_YAML;
import static com.societegenerale.githubcrawler.parsers.YamlParserForPropertyValue.PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.societegenerale.githubcrawler.IndicatorDefinition;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

class YamlParserForPropertyValueTest {

    final String indicatorName = "someIndicatorName";
    String yamlFileSnippet;
    Map<String, String> params = new HashMap<>();
    IndicatorDefinition pomXmlDependencyVersion = new IndicatorDefinition(indicatorName,FIND_PROPERTY_VALUE_IN_YAML,params);

    YamlParserForPropertyValue fileContentParser = new YamlParserForPropertyValue();

    @BeforeEach
    public void setup() throws IOException {

        yamlFileSnippet=
                FileUtils.readFileToString(ResourceUtils.getFile("classpath:sample_yamlfile.yml"), "UTF-8");
    }

    @Test
    void joinListAsStringIfValueIsList() {

        Map<String, String> indicatorsFound = parseSampleYamlForProperty("interesting.valueAsList");

        assertThat(indicatorsFound).containsEntry(indicatorName,"item1, item2");
    }

    @Test
    void canFindAsimplePropertyValue() {

        Map<String, String> indicatorsFound = parseSampleYamlForProperty("interesting.property");

        assertThat(indicatorsFound).containsEntry(indicatorName,"myExpectedValue");
    }

    @Test
    void canFindAsimpleNestedPropertyValue() {

        Map<String, String> indicatorsFound = parseSampleYamlForProperty("interesting.nested.property");

        assertThat(indicatorsFound).containsEntry(indicatorName,"myExpectedNestedValue");
    }

    @Test
    void canFindAbooleanProperty() {

        Map<String, String> indicatorsFound = parseSampleYamlForProperty("boolean.property");

        assertThat(indicatorsFound).containsEntry(indicatorName,"true");
    }

    @Test
    void canFindPropertyIfNotInFirstDocument() {

        Map<String, String> indicatorsFound = parseSampleYamlForProperty("server.ssl.key-store");

        assertThat(indicatorsFound).containsEntry(indicatorName,"hello");
    }

    @Test
    void canFindSemiNestedProperty() {

        Map<String, String> indicatorsFound = parseSampleYamlForProperty("interesting.semiNested.property");

        assertThat(indicatorsFound).containsEntry(indicatorName,"semiNestedValue");
    }

    @Test
    void yieldsIssueWhileParsingIfError() throws IOException {

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
