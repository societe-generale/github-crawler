package com.societegenerale.githubcrawler.config;

import com.societegenerale.githubcrawler.FileToParseConversionService;
import com.societegenerale.githubcrawler.GitHubCrawler;
import com.societegenerale.githubcrawler.GitHubCrawlerProperties;
import com.societegenerale.githubcrawler.RepositoryEnricher;
import com.societegenerale.githubcrawler.mocks.GitHubMock;
import com.societegenerale.githubcrawler.model.Repository;
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput;
import com.societegenerale.githubcrawler.ownership.NoOpOwnershipParser;
import com.societegenerale.githubcrawler.ownership.OwnershipParser;
import com.societegenerale.githubcrawler.remote.RemoteGitHub;
import com.societegenerale.githubcrawler.remote.RemoteGitHubImpl;
import lombok.Getter;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(GitHubCrawlerProperties.class)
public class TestConfig {

    @Bean
    public ConversionService conversionService() {

        return new FileToParseConversionService();
    }

    @Bean
    public GitHubCrawler crawler(RemoteGitHub remoteGitHub,
            OwnershipParser ownershipParser,
            List<GitHubCrawlerOutput> output,
            GitHubCrawlerProperties gitHubCrawlerProperties,
            Environment environment) {

        val repositoryEnricher = new RepositoryEnricher(remoteGitHub);

        return new GitHubCrawler(remoteGitHub, ownershipParser, output, repositoryEnricher, gitHubCrawlerProperties, environment);
    }

    @Bean
    public RemoteGitHub remoteGitHub(@Value("${gitHub.url}") String gitHubUrl) {

        return new RemoteGitHubImpl(gitHubUrl);
    }

    @Bean
    public OwnershipParser dummyOwnershipParser()    {

        return new NoOpOwnershipParser();
    }

    @Bean
    public GitHubMock gitHubMock() {

        return new GitHubMock();
    }

    @Bean
    public InMemoryGitHubCrawlerOutput output() {

        InMemoryGitHubCrawlerOutput output = new InMemoryGitHubCrawlerOutput();

        return output;
    }

    public class InMemoryGitHubCrawlerOutput implements GitHubCrawlerOutput {

        @Getter
        private Map<String, Repository> analyzedRepositories = new HashMap<>();

        @Override
        public void output(Repository analyzedRepository) throws IOException {
            this.analyzedRepositories.put(analyzedRepository.getName(), analyzedRepository);
        }

        public void reset() {
            analyzedRepositories.clear();
        }

        @Override
        public void finalizeOutput() throws IOException {
            //do nothing
        }
    }

}