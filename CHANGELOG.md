# Changelog - see https://keepachangelog.com for conventions

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

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
