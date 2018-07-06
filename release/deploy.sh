#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'makeRelease' ] ; then
    mvn release:prepare release:perform --settings release/mvnsettings.xml
fi