package com.societegenerale.githubcrawler.config;

import com.societegenerale.githubcrawler.mocks.GitHubMock;
import com.societegenerale.githubcrawler.model.Repository;
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput;
import lombok.Getter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableAutoConfiguration
public class TestConfig {

    @Bean
    public GitHubMock gitHubMock(){

        return new GitHubMock();
    }

    @Bean
    public InMemoryGitHubCrawlerOutput output(){

        InMemoryGitHubCrawlerOutput output=new InMemoryGitHubCrawlerOutput();

        return output;
    }

    public class InMemoryGitHubCrawlerOutput implements GitHubCrawlerOutput {

        @Getter
        Map<String,Repository> analyzedRepositories=new HashMap<>();



        @Override
        public void output(Repository analyzedRepository) throws IOException {
            this.analyzedRepositories.put(analyzedRepository.getName(), analyzedRepository);
        }

        public void reset(){
            analyzedRepositories.clear();
        }

        @Override
        public void finalize() throws IOException {
            //do nothing
        }
    }


}
