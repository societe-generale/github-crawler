#!/usr/bin/env bash
if [ "$TRAVIS_PULL_REQUEST_BRANCH" = 'makeRelease' ] ; then
    git checkout master
    mvn --batch-mode release:prepare release:perform --settings release/mvnsettings.xml
fi