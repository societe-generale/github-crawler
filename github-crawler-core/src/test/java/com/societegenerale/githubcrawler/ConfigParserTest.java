package com.societegenerale.githubcrawler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;


class ConfigParserTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    void canParseSimpleYamlConfig() throws IOException {

        String configToParse = "excluded: true";

        RepositoryConfig parsedRepositoryConfig = mapper.readValue(configToParse, RepositoryConfig.class);

        assertThat(parsedRepositoryConfig).isNotNull();
    }

    @Test
    void canParseMoreComplexYamlConfig() throws IOException {

        InputStream is = getClass().getClassLoader().getResourceAsStream("sampleRepoConfig.yaml");
        String configToParse = StreamUtils.copyToString(is, Charset.forName("UTF-8"));

        mapper.registerModule(new KotlinModule());

        RepositoryConfig parsedRepositoryConfig = mapper.readValue(configToParse, RepositoryConfig.class);

        assertThat(parsedRepositoryConfig).isNotNull();
        assertThat(parsedRepositoryConfig.getExcluded()).isFalse();
        assertThat(parsedRepositoryConfig.getFilesToParse()).hasSize(1);

        FileToParse firstFile = parsedRepositoryConfig.getFilesToParse().get(0);
        assertThat(firstFile.getRedirectTo()).isEqualTo("moduleWhereDockerFileIs/Dockerfile");
        assertThat(firstFile.getName()).isEqualTo("Dockerfile");
    }

    @Test
    void canParseSimpleJsonConfig() throws IOException {

        String configToParse = "{\"excluded\": true}";

        ObjectMapper mapper = new ObjectMapper();

        RepositoryConfig parsedRepositoryConfig = mapper.readValue(configToParse, RepositoryConfig.class);

        assertThat(parsedRepositoryConfig).isNotNull();
    }

}
