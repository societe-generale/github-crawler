package com.societegenerale.githubcrawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import lombok.val;
import org.junit.Test;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;


public class ConfigParserTest {

    @Test
    public void canParseSimpleYamlConfig() throws IOException {

        String configToParse = "excluded: true";

        val mapper = new ObjectMapper(new YAMLFactory());

        RepositoryConfig parsedRepositoryConfig = mapper.readValue(configToParse, RepositoryConfig.class);

        assertThat(parsedRepositoryConfig).isNotNull();
    }

    @Test
    public void canParseMoreComplexYamlConfig() throws IOException {

        InputStream is = getClass().getClassLoader().getResourceAsStream("sampleRepoConfig.yaml");
        String configToParse = StreamUtils.copyToString(is, Charset.forName("UTF-8"));

        val mapper = new ObjectMapper(new YAMLFactory());

        mapper.registerModule(new KotlinModule());

        RepositoryConfig parsedRepositoryConfig = mapper.readValue(configToParse, RepositoryConfig.class);

        assertThat(parsedRepositoryConfig).isNotNull();
        assertThat(parsedRepositoryConfig.getExcluded()).isFalse();
        assertThat(parsedRepositoryConfig.getFilesToParse()).hasSize(1);

        val firstFile = parsedRepositoryConfig.getFilesToParse().get(0);
        assertThat(firstFile.getRedirectTo()).isEqualTo("moduleWhereDockerFileIs/Dockerfile");
        assertThat(firstFile.getName()).isEqualTo("Dockerfile");
    }

    @Test
    public void canParseSimpleJsonConfig() throws IOException {

        String configToParse = "{\"excluded\": true}";

        val mapper = new ObjectMapper();

        RepositoryConfig parsedRepositoryConfig = mapper.readValue(configToParse, RepositoryConfig.class);

        assertThat(parsedRepositoryConfig).isNotNull();
    }

}
