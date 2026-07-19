package com.societegenerale.githubcrawler;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.module.kotlin.KotlinModule;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;


class ConfigParserTest {

    ObjectMapper mapper = YAMLMapper.builder()
        .addModule(new KotlinModule.Builder().build())
        .build();

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

        RepositoryConfig parsedRepositoryConfig = mapper.readValue(configToParse, RepositoryConfig.class);

        assertThat(parsedRepositoryConfig).isNotNull();
        assertThat(parsedRepositoryConfig.getExcluded()).isFalse();
        assertThat(parsedRepositoryConfig.getFilesToParse()).hasSize(1);

        FileToParse firstFile = parsedRepositoryConfig.getFilesToParse().getFirst();
        assertThat(firstFile.getRedirectTo()).isEqualTo("moduleWhereDockerFileIs/Dockerfile");
        assertThat(firstFile.getName()).isEqualTo("Dockerfile");
    }

    @Test
    void canParseSimpleJsonConfig() throws IOException {

        String configToParse = "{\"excluded\": true}";

        ObjectMapper mapper = new JsonMapper();

        RepositoryConfig parsedRepositoryConfig = mapper.readValue(configToParse, RepositoryConfig.class);

        assertThat(parsedRepositoryConfig).isNotNull();
    }

}
