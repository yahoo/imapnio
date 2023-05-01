#!/usr/bin/env bash
    mkdir ci/deploy

    openssl aes-256-cbc -pass pass:$GPG_ENCPHRASE -in ci/pubkeys.asc.enc -out ci/deploy/pubkeys.asc -pbkdf2 -d
    openssl aes-256-cbc -pass pass:$GPG_ENCPHRASE -in ci/prikeys.asc.enc -out ci/deploy/prikeys.asc -pbkdf2 -d
    gpg --batch --fast-import ci/deploy/pubkeys.asc
    gpg --batch --fast-import ci/deploy/prikeys.asc

    gpg --version
    gpg --list-keys
    gpg --list-secret-keys
    mvn deploy -P ossrh --settings ci/mvnsettings.xml
    # delete decrypted keys
    rm -rf ci/deploy
