package com.societegenerale.githubcrawler.config;

import com.societegenerale.githubcrawler.GitHubCrawlerProperties;
import com.societegenerale.githubcrawler.model.Repository;
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GitHubCrawlerProperties.class)
public class TestConfig {

    @Bean
    public GitHubCrawlerOutput output() {

        return new InMemoryGitHubCrawlerOutput();
    }

    public static class InMemoryGitHubCrawlerOutput implements GitHubCrawlerOutput {

        private final Map<String, Repository> analyzedRepositories = new ConcurrentHashMap<>();

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
