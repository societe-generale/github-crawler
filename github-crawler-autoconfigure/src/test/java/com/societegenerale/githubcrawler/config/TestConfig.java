package com.societegenerale.githubcrawler.config;

import com.societegenerale.githubcrawler.*;
import com.societegenerale.githubcrawler.mocks.GitHubMock;
import com.societegenerale.githubcrawler.model.Repository;
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput;
import com.societegenerale.githubcrawler.parsers.FileContentParser;
import com.societegenerale.githubcrawler.remote.RemoteGitHub;
import com.societegenerale.githubcrawler.repoTaskToPerform.RepoTaskBuilder;
import com.societegenerale.githubcrawler.repoTaskToPerform.ownership.NoOpOwnershipParser;
import com.societegenerale.githubcrawler.repoTaskToPerform.ownership.OwnershipParser;
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
            List<GitHubCrawlerOutput> output,
            GitHubCrawlerProperties gitHubCrawlerProperties,
            Environment environment,
            String organizationName,
            ConfigValidator configValidator,
            List<FileContentParser> fileContentParsers,
            List<RepoTaskBuilder> tasksBuilders) {

        RepositoryEnricher repositoryEnricher = new RepositoryEnricher(remoteGitHub);

        return new GitHubCrawler(remoteGitHub, output, repositoryEnricher, gitHubCrawlerProperties, environment,organizationName,configValidator,fileContentParsers,tasksBuilders);
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

        private Map<String, Repository> analyzedRepositories = new HashMap<>();

        public Map<String, Repository> getAnalyzedRepositories() {
            return analyzedRepositories;
        }

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
