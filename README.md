# GitHub crawler

[![Build Status](https://travis-ci.org/societe-generale/github-crawler.svg?branch=master)](https://travis-ci.org/societe-generale/github-crawler)

## Why can it be useful ?

With the current move to microservices, it's not rare that a team who previously had a couple of repositories, now has several dozens. 
Keeping a minimum of consistency between the repositories becomes a challenge which may cause risks : 
- have we updated all our repositories so that they use the latest Docker image ?
- have we set up the proper security config in all our repositories ?
- which versions of the library X are we using across ?
- are we using a library that we are not supposed to use anymore ?
- do we use hardcoded string in unexpected places in our code ?
- which team is owner of a repository ? 

These are all simple questions that sometimes take hours to answer, with always the risk of missing one repository in the analysis, making the answer inaccurate.

Github crawler aims at automating the information gathering, by crawling an organization's repositories through GitHub API. **Even if your organization has hundreds of repositories,
Github crawler will be able to report very useful information in few seconds !** 

## Getting started 

If you want to provide your own configuration without any code customisation, then you can simply : 
- download the latest github-crawler-starter -exec jar from [Maven](http://repo1.maven.org/maven2/com/societegenerale/github-crawler/github-crawler-starter/)
- place your config file (say _application.yml_) next to the jar - see [below](README.md#Configuration-on-crawler-side)
- run from command line :

```bash
java -jar github-crawler-exec.jar --spring.config.location=./
 ```

(more examples are available in sections below, ie how to run from IDE and how to extend github crawler, and in this [repository](https://github.com/vincent-fuchs/my-custom-github-crawler/))

## How does it work ?

Github crawler is a Spring Boot command line application, written in Kotlin.

Following a simple configuration, it will use Github API starting from a given organization level, then for each repository, will look for patterns in specified files or perform other actions. 

You can easily exclude repositories from the analysis, configure the files and patterns you're interested in. If you have several types of repositories (front-end, back-end, config repositories for instance), you can have separate configuration files so that the information retrieved is relevant to each scope of analysis.

Several output types are available in [this package](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/output/) :
- console is the default and will be used if no output is configured
- a simple "raw" file output
- HTTP output, which enables you to POST the results to an endpoint like ElasticSearch, for easy analysis in Kibana
- some specific "CI-droid oriented" outputs, to easily "pipe" the crawler output to CI-droid


## Configuration on crawler side 

Below configuration shows how outputs, indicators and actions are configured under the ```github-crawler``` prefix.

```yaml
github-crawler:
    
    githubConfig:    
        # the base GitHub URL for your Github enterprise instance to crawl
        # or if it's github.com...
        # gitHub.url: https://api.github.com
        apiUrl: https://my.githubEnterprise/api/v3
        oauthToken: "YOUR_TOKEN"
        # the name of the GitHub organization to crawl. To fetch the repositories, the crawler will hit 
        # https://${gitHub.url}/api/v3/orgs/${organizationName}/repos
        organizationName: MyOrganization
        # default is false - API URL is slightly different depending on whether you're crawling an organization (most common case) or a user's repositories
        crawlUsersRepoInsteadOfOrgasRepos: false
     
    #repositories matching one of the configured regexp will be excluded
    repositoriesToExclude:
      # exclude the ones that start with "financing-platform-" and end with "-run"
      - "^financing-platform-.*-run$"
      # exclude the ones that DON'T start with "financing-platform-" 
      - "^(?!financing-platform-.*$).*"
    
    # do you want the excluded repositories to be written in output ? (default is false)
    # even if they won't have any indicators attached, it can be useful to output excluded repositories, 
    # especially at beginning, to make sure you're not missing any
    publishExcludedRepositories: true
    
    # by default, we'll crawl only the repositories' default branch. But in some cases, you may want to crawl all branches
    crawlAllBranches: true
    
    #by default, we'll crawl repositories in parallel. However, especially when facing an issue, crawling sequentially can help identifying the issue faster.
    #therefore, providing the option to switch between parallel and sequential processing
    crawl-in-parallel: true
    
    # default output is console - it will be configured automatically if no output is defined
    # the crawler takes a list of output, so you can configure several
    outputs:
      file:
      # we'll output one repository branch per line, in a file named ${filenamePrefix}_yyyyMMdd_hhmmss.txt
       filenamePrefix: "orgaCheckupOutput"
      http:
        # we'll POST one repository branch individually to ${targetUrl}
        targetUrl: "http://someElasticSearchServer:9201/technologymap/MyOrganization"
     
    # list the files to crawl for, and the patterns to look for in each file         
    indicatorsToFetchByFile:
    # use syntax with "[....]" to escape the dot in the file name (configuration can't be parsed otherwise, as "." is a meaningful character in yaml files)
      "[pom.xml]":
        # name of the indicator that will be reported for that repository in the output
        - name: spring_boot_starter_parent_version
          # name of the method to find the value in the file, pointing to one of the implementation classes of FileContentParser
          type: findDependencyVersionInXml
          # the parameters to the method, specific to each method type
          params:
            # findDependencyVersionInXml needs an artifactId as a parameter : it will find the version for that Maven artifact by doing a SAX parsing, even if the version is a ${variable} defined in <properties> section
            artifactId: spring-boot-starter-parent
        - name: spring_boot_dependencies_version
          type: findDependencyVersionInXml
          params:
            artifactId: spring-boot-dependencies
      #another file to parse..
      Dockerfile:
        - name: docker_image_used
            # findFirstValueWithRegexpCapture needs a pattern as a parameter. The pattern needs to contain a group capture (see https://regexone.com/lesson/capturing_groups) 
            # the first match will be returned as the value for this indicator             
          type: findFirstValueWithRegexpCapture
          params:
            pattern: ".*\\/(.*)\\s?"
    
      "[src/main/resources/application.yml]":
          - name: spring_application_name
            type: findPropertyValueInYamlFile
            params:
              propertyName: "spring.application.name"
              
# We can also define a list of miscellaneous actions to perform : this includes things like various searches, ownership computation

    misc-repository-tasks:
       - name: "nbOfMetricsInPomXml"
       #will return the number of hits returned by a search using queryString, for each repo
         type: "countHitsOnRepoSearch"
         params:
           queryString: "q=metrics+extension:xml"
       - name: "pathsWhere_ConsulCatalogWatch_IsFound"
       #will return the paths for each hit on th search using queryString, for each repo
         type: "pathsForHitsOnRepoSearch"
         params:
           queryString: "q=ConsulCatalogWatch"          
```

## Configuration on repository side 

While the global configuration is defined along with github crawler, we have the possibility to override it at the repository level. 
Repository level config is stored in a **.githubCrawler** file, at the root of the repository in the default branch

- **Exclusion** 

if a repository should be excluded, we can define it in the repository itself. if ```.githubCrawler``` contains :

```yaml
    excluded: true  
```

Then the crawler will consider the repository as excluded, even if it doesn't match any of the exclusion pattern in the crawler config 

- **Redirecting to a specific file to parse**

Sometimes, the file we're interested in parsing is not in a standard location like the root of the repository.

What we can do in this case is define the file in the crawler config, and override the path in the repository config, with the redirectTo attribute, here for a DockerFile :

```yaml
    filesToParse: 
      - 
        name: Dockerfile
        redirectTo: routing/Dockerfile 
```

With above config, when the crawler tries to fetch Dockerfile at the root of the repository, it will actually try to parse *routing/Dockerfile*

- **Tagging a repo**

You may want to "tag" some repos, to be able to filter easily on them when browsing the results.
GitHub provides "topics" that are very easy to edit, which are actually similar to "tags".
GithubCrawler crawls through repository and attaches tags information with all the repositories for which topics have been configured.

## Gitlab support

From v1.1.4 onward, basic support for gitLab is available ! It all boils down to implementing a GitLab specific version of ```RemoteGitHub``` interface. Primary support is still for Github though, so not all the naming and config is aligned for GitLab (at least for now).

### Running the crawler for Gitlab

- run the application with proper Spring Boot profile, ie with ```-Dspring.profiles.active=gitLab``` on the command line
- Since the crawler is still primarily for GitHub, the config properties haven't been adapted (yet ?) for Gitlab. So your config would look like : 

```
    github-crawler:
      githubConfig:
        apiUrl: https://gitlab.com/api/v4/

        # your Gitlab personal access token
        oauthToken: "5yL4_Y9hyC_YX9urZN_G"

        # your Gitlab "group"
        organizationName: myJavaProjects
```

Not all methods defined in [RemoteGitHub](https://github.com/societe-generale/github-crawler/blob/b86c7483e4f361211750f454d70ccdec135ad655/github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/remote/RemoteGitHub.kt) interface may have been implemented for Gitlab : ```NotImplementedError``` would be thrown in that case. If you need them, you can implement them in ```RemoteGitLabImpl``` (and contribute them back through a pull request ?).

Similarly, we may have added methods in the interface for some of our Gitlab specific use-cases : in that case, these methods may not have been implemented in the [Github version of the interface](https://github.com/societe-generale/github-crawler/blob/master/github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/remote/RemoteGitHubImpl.kt) 
 
## File content parsers

Some parsers are provided [here](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/parsers). As of v1.1.1, available parser types out of the box are :

- [countMatchingXmlElements](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/parsers/CountXmlElementsParser.kt)
- [findFirstValueWithRegexpCapture](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/parsers/FirstMatchingRegexpParser.kt)
- [findDependencyVersionInXml](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/parsers/PomXmlParserForDependencyVersion.kt)
- [findValueForJsonPath](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/parsers/JsonPathParser.kt)
- [findFilePath](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/parsers/SimpleFilePathParser.kt) 

see javadoc in each class for details

## Miscellaneous tasks to perform 

We sometimes need to get information on repositories, that is not found in the files it contains : we need to perform a "task" on each repository. As of v1.1.0, these are the task types available out of the box  :

- [countHitsOnRepoSearch](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/repoTaskToPerform/CountHitsOnRepoSearch.kt) 
- [pathsForHitsOnRepoSearch](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/repoTaskToPerform/PathsForHitsOnRepoSearch.kt)
- [repositoryOwnershipComputation](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/repoTaskToPerform/ownership/RepoOwnershipComputer.kt)

see javadoc in each class for details


## Outputs

Available default outputs are available in this [package](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/output). 

Each of them can be enabled at startup time through configuration. Have a look at [GitHubCrawlerOutputConfig](https://github.com/societe-generale/github-crawler/blob/master/github-crawler-autoconfigure/src/main/kotlin/com/societegenerale/githubcrawler/config/GitHubCrawlerOutputConfig.kt) to see which property activates which output : we use Spring ```@ConditionalOnProperty``` to decide which output to instantiate, depending on what we've configured under ```github-crawler.outputs```

As of v1.1.0, there are 2 "general purpose" outputs available : 
- [FileOutput](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/output/FileOutput.kt)
- [HttpOutput](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/output/HttpOutput.kt)

there are 3 "specific purpose" outputs available (see javadoc for more infos):
- [CIdroidReadyCsvFileOutput](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/output/CIdroidReadyCsvFileOutput.kt)
- [CIdroidReadyJsonFileOutput](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/output/CIdroidReadyJsonFileOutput.kt)
- [SearchPatternInCodeCsvFileOutput](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/output/SearchPatternInCodeCsvFileOutput.kt)


default output is [ConsoleOutput](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/output/ConsoleOutput.kt) 

#### example using HTTP output, pointing to ElasticSearch with Kibana on top

when running the crawler with HTTP output to push indicators in ElasticSearch, this is the kind of data you'll get 

- different values for the same indicator, fetched with ```findFirstValueWithRegexpCapture``` parser: 

![](/docs/images/kibanaOutput1.png)

- different values for the same indicator, fetched with ```findDependencyVersionInXml``` parser : 

![](/docs/images/kibanaOutput2.png)

(when there's no value, it means the file was not found. when the value is "not found", it means the file exists, but the value was not found in it)

- when using _crawlAllBranches: true_ property , branch name is shown : 

![](/docs/images/kibanaOutput_severalBranches.png)

Once you have this data, you can quickly do any dashboard you want, like here, with the split of ```spring-boot-starter-parent``` version across our services : 

![](/docs/images/kibana_visualization.png) 


## Packaging

At build time, we produce several jars :

- a _starter-exec_ jar, bigger because self-contained. If you don't need to extend it, just take this jar and run it from command line with your config
- much smaller _regular_ jars (following [Spring Boot recommendations](https://github.com/spring-projects/spring-boot/wiki/Building-On-Spring-Boot), that contains just the compiled code : this is the jar you need to declare as a dependency if you want to extend Github crawler on your side. 


## Running the crawler from your IDE

We leverage on Spring Boot profiles to manage several configurations. Since we consider that each profile will represent a logical grouping of repositories, the Spring profile(s) will be copied on a "groups" attribute for each repository in output. 

Assuming you have a property file as defined above, all you need to do in your IDE is :

1. check out this repository
2. create your own property file in src/main/resources, and name it _application-myOwn.yml_ : myOwn is the Spring Boot profile you'll use
3. run GitHubCrawlerApplication, passing _myOwn_ as profile 

![](/docs/images/runningFromIDE.png)


## Extending the crawler (and contributing to it ?)

A starter project is available, allowing you to create your own GitHub crawler application, leveraging on everything that exists in the library.
This is the perfect way to test your own output or parser class on your side.. before maybe contributing it back to the project ? ;-) 

A simple example is available here : https://github.com/vincent-fuchs/my-custom-github-crawler/  

- import the gitHubCrawler starter as a [dependency](https://github.com/vincent-fuchs/my-custom-github-crawler/blob/0e48dd7961b6b625802a2e1eb6b2fc4f8c4d5cdb/pom.xml#L19-L23) in your project
- create a Spring Boot starter class, and inject the GitHubCrawler instantiated by the starter's autoconfig :

```java
@SpringBootApplication
public class PersonalGitHubCrawlerApplication implements CommandLineRunner {

    @Autowired
    private GitHubCrawler crawler;

    public static void main(String[] args) {

        SpringApplication.run(PersonalGitHubCrawlerApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        crawler.crawl();
    }
}
```

- add your own config or classes, the Spring Boot way : if you add your own, implementing the recognized interfaces for output or parsing, then Spring Boot will use them ! 
see [here](https://github.com/vincent-fuchs/my-custom-github-crawler/blob/0e48dd7961b6b625802a2e1eb6b2fc4f8c4d5cdb/src/main/java/com/github/vincent_fuchs/output/CustomOutput.java) or [here](https://github.com/vincent-fuchs/my-custom-github-crawler/blob/ec7ed9a74f91b31794b8a0afb1196553434b1567/src/main/java/com/github/vincent_fuchs/parsers/MyOwnParser.java) for examples

- see the javadoc in [FileContentParser](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/parsers/FileContentParser.kt) , [RepoTaskToPerform](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/repoTaskToPerform/RepoTaskToPerform.kt), [GitHubCrawlerOutput](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/output/GitHubCrawlerOutput.kt) which are the main extension points.
