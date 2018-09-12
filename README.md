# GitHub crawler

[![Build Status](https://travis-ci.org/societe-generale/github-crawler.svg?branch=master)](https://travis-ci.org/societe-generale/github-crawler) [![DepShield Badge](https://depshield.sonatype.org/badges/societe-generale/github-crawler/depshield.svg)]

## Why can it be useful ?

With the current move to microservices, it's not rare that a team who previously had a couple of repositories, now has several dozens. 
Keeping a minimum of consistency between the repositories becomes a challenge which may cause risks : 
- have we updated all our repositories so that they use the latest Docker image ?
- have we set up the proper security config in all our repositories ?
- which versions of the library X are we using across ?
- are we using a library that we are not supposed to use anymore ?
- which team is owner of a repository ? 

These are all simple questions that sometimes take hours to answer, with always the risk of missing one repository in the analysis, making the answer inaccurate.

Github crawler aims at automating the information gathering, by crawling an organization's repositories through GitHub API. **Even if your organization has hundreds of repositories,
Github crawler will be able to report very useful information in few seconds !** 

## How does it work ?

Github crawler is a Spring Boot command line application. It is written in Java and Kotlin, the target being to move as much as possible to Kotlin.

Following a simple configuration, it will use Github API starting from a given organization level, then for each public repository, will look for patterns in specified files. 

You can easily exclude repositories from the analysis, configure the files and patterns you're interested in. If you have several types of repositories (front-end, back-end, config repositories for instance), you can have separate configuration files so that the information retrieved is relevant to each scope of analysis.

Several output types are available in [this package](./github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/output/) :
- console is the default and will be used if no output is configured
- a simple "raw" file output
- a CI-droid ready CSV (or Json) file output, to run locally and copy/paste in CI-droid bulk update UI (UI implementation is in progress)
- HTTP output, which enables you to POST the results to an endpoint like ElasticSearch, for easy analysis in Kibana


## Configuration on crawler side 

Several outputs can be configured at the same time. You can define as many indicators to fetch as required. 

Below configuration shows how parsers are invoked for each indicator, thanks to the ```method``` attribute.

```yaml
    # the base GitHub URL for your Github enterprise instance to crawl
    gitHub.url: https://my.githubEnterprise/api/v3
    
    # or if it's github.com...
    # gitHub.url: https://api.github.com
    
    # the name of the GitHub organization to crawl. To fetch the repositories, the crawler will hit 
    # https://${gitHub.url}/api/v3/orgs/${organizationName}/repos
    organizationName: MyOrganization
    
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
    
    # default output is console - it will be configured automatically if no output is defined
    # the crawler takes a list of output, so you can configure several
    output:
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
          method: findDependencyVersionInXml
          # the parameters to the method, specific to each method type
          params:
            # findDependencyVersionInXml needs an artifactId as a parameter : it will find the version for that Maven artifact by doing a SAX parsing, even if the version is a ${variable} defined in <properties> section
            artifactId: spring-boot-starter-parent
        - name: spring_boot_dependencies_version
          method: findDependencyVersionInXml
          params:
            artifactId: spring-boot-dependencies
      #another file to parse..
      Dockerfile:
        - name: docker_image_used
            # findFirstValueWithRegexpCapture needs a pattern as a parameter. The pattern needs to contain a group capture (see https://regexone.com/lesson/capturing_groups) 
            # the first match will be returned as the value for this indicator             
          method: findFirstValueWithRegexpCapture
          params:
            pattern: ".*\\/(.*)\\s?"
    
      "[src/main/resources/application.yml]":
          - name: spring_application_name
            method: findPropertyValueInYamlFile
            params:
              propertyName: "spring.application.name"
```

## Link with [CI-droid](https://github.com/societe-generale/ci-droid)

[CI-droid](https://github.com/societe-generale/ci-droid) is another of our tools, that can help you perform the same actions (modifying a pom.xml, replacing a string by another) in N resources, in X repositories. When there are dozens of resources to update, it can be cumbersome to prepare the message. GitHub crawler can help here, with below config :

```yaml
    output:
      ciDroidJsonReadyFile:
        # this should be an indicator defined in indicatorsToFetchByFile section
        indicatorsToOutput: "pomFilePath" 

    indicatorsToFetchByFile:
      "[pom.xml]":
          - name: "pomFilePath"
            method: findFilePath
```

this will generate a file containing all the pom.xml found in the repositories (and in the branches also if ```crawlAllBranches``` is set to true), in the format that CI-droid expect. 

All you need to do is then to copy/paste the records you want into the CI-droid bulk action Json message.

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

## Parsers

Some parsers are provided [here](https://github.com/societe-generale/github-crawler/tree/master/github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/parsers). There are 2 types of parsers and you can 
easily add your own implementation by implementing one of the 2 interfaces (see javadoc for details) :

- [FileContentParser](https://github.com/societe-generale/github-crawler/blob/master/github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/parsers/FileContentParser.kt)
- [SearchResultParser](https://github.com/societe-generale/github-crawler/blob/master/github-crawler-core/src/main/kotlin/com/societegenerale/githubcrawler/parsers/SearchResultParser.kt)


## Outputs

when running the crawler with above config and using HTTP output to push indicators in ElasticSearch, this is the kind of data you'll get 

- different values for the same indicator, fetched by findFirstValueWithRegexpCapture method : 

![](/docs/images/kibanaOutput1.png)

- different values for the same indicator, fetched by findDependencyVersionInXml method : 

![](/docs/images/kibanaOutput2.png)

(when there's no value, it means the file was not found. when the value is "not found", it means the file exists, but the value was not found in it)

- when using _crawlAllBranches: true_ property , branch name is shown : 

![](/docs/images/kibanaOutput_severalBranches.png)

Once you have this data, you can quickly do any dashboard you want, like here, with the split of ```spring-boot-starter-parent``` version across our services : 

![](/docs/images/kibana_visualization.png) 


## Packaging

At build time, we produce several jars :

- a _starter-exec_ jar, bigger because self-contained. If you don't need to extend it, just take this jar and run it from command line with your config
- much smaller _regular_ jars (following [Spring Boot recommendations](https://github.com/spring-projects/spring-boot/wiki/Building-On-Spring-Boot), that contains just the compiled code : these are the jar you need to declare as a dependency if you want to extend Github crawler on your side. 


## Running the crawler from your IDE

We leverage on Spring Boot profiles to manage several configurations. Since we consider that each profile will represent a logical grouping of repositories, the Spring profile(s) will be copied on a "groups" attribute for each repository in output. 

Assuming you have a property file as defined above, all you need to do in your IDE is :

1. check out this repository
2. create your own property file in src/main/resources, and name it _application-myOwn.yml_ : myOwn is the Spring Boot profile you'll use
3. run GitHubCrawlerApplication, passing _myOwn_ as profile 

![](/docs/images/runningFromIDE.png)


## Running the crawler from the packaged -exec jar, from the command line :

As mentioned above, we also package the starter as a standalone -exec jar (from v1.0.3 onward), available in [Maven Central](http://repo1.maven.org/maven2/com/societegenerale/github-crawler/github-crawler-starter/), so all you have to do is to fetch it and execute it with the property file(s) that you need. 

Have a look at below very simple script :
1. get the jar from Maven central (or place the jar you've built locally)
2. place your yml config files along with the jar
3. run the jar with your config files (--spring.config.location parameter) and the proper profile (--spring.profiles.active)

--> it should work and ouput will be available according to your configuration

```bash
#!/usr/bin/env bash
crawlerVersion="1.0.3"
wget -P github-crawler-exec.jar http://repo1.maven.org/maven2/com/societegenerale/github-crawler/github-crawler-starter/${crawlerVersion}/github-crawler-starter-${crawlerVersion}-exec.jar --no-check-certificate
$JAVA_HOME/bin/java -jar github-crawler-exec.jar --spring.config.location=./ --spring.profiles.active=myOwn
```

Above script assumes that you have property file(s) in same directory as the script itself (_--spring.config.location=./_) and that one of them is declaring a _myOwn_ Spring Boot profile


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



## Test strategy

We follow a strict test driven strategy for the implementation. Contributions are welcome, but you'll need to submit decent tests along with your changes for them to be accepted. 
Browse the tests to get an idea of what level of test is expected.
