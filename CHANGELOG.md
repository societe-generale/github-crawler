# Changelog - see https://keepachangelog.com for conventions

## [Unreleased]

### Added
- ability to make a release by creating a branch named "makerelease"

### Changed

### Deprecated

### Removed
- most of default parameters in application.yml - if you were using it, put them in your own config file

### Fixed

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
