package com.societegenerale.githubcrawler.config;

import com.societegenerale.githubcrawler.GitHubCrawlerProperties;
import com.societegenerale.githubcrawler.mocks.GitHubMock;
import com.societegenerale.githubcrawler.model.Repository;
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(GitHubCrawlerProperties.class)
public class TestConfig {

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
        public void output(Repository analyzedRepository) {
            this.analyzedRepositories.put(analyzedRepository.getName(), analyzedRepository);
        }

        public void reset() {
            analyzedRepositories.clear();
        }

        @Override
        public void finalizeOutput() {
            //do nothing
        }
    }

}
