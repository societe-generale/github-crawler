#!/usr/bin/env bash
if [ "$TRAVIS_PULL_REQUEST_BRANCH" = 'makeRelease' ] ; then
    mvn release:prepare release:perform --settings release/mvnsettings.xml
fi