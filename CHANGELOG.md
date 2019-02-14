# Changelog - see https://keepachangelog.com for conventions

## [Unreleased]

### Added
- issue #78 - now we can report the number of open PRs and branches for each repository

### Changed

### Deprecated

### Removed

### Fixed

##  [1.1.2] - 2019-01-26

### Added
- possibility to configure targetUrl with a date pattern (useful when POSTing to ElasticSearch)

### Changed
- Upgraded Jacoco to 0.8.3
- Upgraded Spring Boot to 2.1.2

##  [1.1.1] - 2018-12-13

### Added
- adding a JsonPath parser

### Fixed
- issue #72 - checking that Oauth token is not blank


## [1.1.0] - 2018-11-21 - BREAKING CHANGES

### Added
- optional _withTags_ property on ciDroidJsonReadyFile output
- properties auto-completion in IDE
- new output available : SearchPatternInCodeCsvFileOutput
- new action available : PathsForHitsOnRepoSearch

### Changed
- upgrade to Kotlin 1.3.10
- upgrade to Spring Boot 2.1.0
- BREAKING CHANGE - all properties are now under a github-crawler prefix
- BREAKING CHANGE - introducing misc-repository-tasks in config : searches need to be moved there 
- BREAKING CHANGE - issue #66 : in config, renamed property name from "method" to "type"

### Removed
- issue #62 - removing topics field from OutputIndicator -> use tags instead

### Fixed
- Kotlin compiler warnings
- issue #41 - fetching commits on a repo was failing


## [1.0.11] - 2018-10-26

### Fixed
- upgraded dom4j version to fix vulnerability CVE-2018-1000632
- issue #44 - CountXmlElementsParser makes the crawler crash when there's a parsing issue
- issue #46 - now stacktrace is logged when there's a runtime exception during processing
- YamlParser can now parse results that are lists without crashing 

## [1.0.10] - 2018-09-28

### Added
- a property (crawlInParallel) to enable/disable the parallel processing or repositories. default is true

### Fixed
- issue #39 - handling exception during yaml parsing to avoid crash

## [1.0.9] - 2018-09-20

### Added
- issue #35 - new parser : CountXmlElementsParser

### Fixed
- issue #31 - now handling invalid search results without crashing
- issue #34 - now giving details when HttpOutput POST fails

## [1.0.8] - 2018-09-05

### Changed
- issue #10 - migrated to Spring Boot 2.0.4

### Fixed
- issue #29 - not crashing anymore when fetching commits from a repository that is empty

## [1.0.7] - 2018-08-29

### Fixed
- following issue #3 - parsing of repo config shouldn't fail on unknown element

### Changed
- issue #25 - we can now parse user's repo, not only organization's
- using Github OAuth token to send authenticated requests to github.com : this way, we're not impacted by the rate-limit


## [1.0.6] - 2018-08-20

### Changed
- issue #3 : Github topics are now used for tags. 

### Deprecated
- related to issue #3 - tags in repoConfig are not accepted anymore, they need to be declared in github's topics

## [1.0.5] - 2018-08-08

### Fixed
- issue #20 - now having better error message in case configuration of URL or organization are incorrect
- issue #22 - we couldn't run the application from IDE anymore 

## [1.0.4] - 2018-07-10

### Added
- ability to make a Maven release with Travis, when pushing a tag

### Removed
- most of default parameters in application.yml - if you were using it, put them in your own config file


## [1.0.3] - 2018-06-29

### Changed
- re-enabling the build of a -exec jar that was removed in v1.0.2
- upgraded to Kotlin 1.2.50

### Fixed
- issue #14 - now it won't fail if we don't provide indicatorsToFetchByFile in the config

## [1.0.2] - 2018-06-28

### Changed
- issue #9 - changed the way application is package, in a more standard way
- Documentation update
- some Codacy issues have been fixed

## [1.0.1] - 2018-06-19

### Added
- improved documentation
- issue #1 - now crawler is compatible for both github.com and GitHub Enterprise
- outputs are now part of autoconfig, when extending the crawler in a separate app

## [1.0.0] - 2018-06-12

first version !
