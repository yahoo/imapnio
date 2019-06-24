#!/usr/bin/env bash
if [ "${TRAVIS_BRANCH}" == 'master' ] && [ "${TRAVIS_PULL_REQUEST}" == 'false' ]; then
    mvn deploy --settings ci/mvnsettings.xml
fi
