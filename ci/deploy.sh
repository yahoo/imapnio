#!/usr/bin/env bash
if [ "${TRAVIS_BRANCH}" == 'master' ] && [ "${TRAVIS_PULL_REQUEST}" == 'false' ]; then
    mkdir ci/deploy
    openssl aes-256-cbc -pass pass:$GPG_ENCPHRASE -in ci/pubring.gpg.enc -out ci/deploy/pubring.gpg -d
    openssl aes-256-cbc -pass pass:$GPG_ENCPHRASE -in ci/secring.gpg.enc -out ci/deploy/secring.gpg -d
    gpg --fast-import ci/deploy/pubring.gpg
    gpg --fast-import ci/deploy/secring.gpg

    mvn deploy -P ossrh --settings ci/mvnsettings.xml
    # delete decrypted keys
    rm -rf ci/deploy
fi
