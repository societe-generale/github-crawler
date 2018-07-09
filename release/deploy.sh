#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'makeRelease' ] ; then
    git checkout master
    mvn release:prepare release:perform --settings release/mvnsettings.xml
fi