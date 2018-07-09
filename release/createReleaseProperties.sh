#!/bin/bash

echo "scm:git:git@github.com:societe-generale/github-crawler.git" >> ../release.properties
echo "scm.tag=$TRAVIS_TAG" >> ../release.properties
