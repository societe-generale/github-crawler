package com.societegenerale.githubcrawler.parsers;

import com.societegenerale.githubcrawler.IndicatorDefinition;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.societegenerale.githubcrawler.TestUtils.readFromInputStream;
import static com.societegenerale.githubcrawler.parsers.YamlParserForPropertyValue.FIND_PROPERTY_VALUE_IN_YAML;
import static com.societegenerale.githubcrawler.parsers.YamlParserForPropertyValue.PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class YamlParserForPropertyValueTest {

    final String indicatorName = "someIndicatorName";
    String yamlFileSnippet;
    Map<String, String> params = new HashMap<>();
    IndicatorDefinition pomXmlDependencyVersion = new IndicatorDefinition();

    YamlParserForPropertyValue fileContentParser = new YamlParserForPropertyValue();

    @Before
    public void setup() throws IOException {

        yamlFileSnippet= readFromInputStream(getClass().getResourceAsStream("/sample_yamlfile.yml"));

        pomXmlDependencyVersion.setName(indicatorName);
        pomXmlDependencyVersion.setMethod(FIND_PROPERTY_VALUE_IN_YAML);
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

    private Map<String, String> parseSampleYamlForProperty(String propertyName) {
        params.put(PROPERTY_NAME, propertyName);
        pomXmlDependencyVersion.setParams(params);

        return fileContentParser.parseFileContentForIndicator(yamlFileSnippet,StringUtils.EMPTY, pomXmlDependencyVersion);

    }



}
