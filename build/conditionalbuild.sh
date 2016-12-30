#!/bin/bash -e
# ref: https://raw.githubusercontent.com/cdown/travis-automerge/master/travis-automerge

if [ ! -z "$TRAVIS_TAG" ]; then 
   printf "Don't execute releases on tag builds request"
   exit 0
fi

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then 
   printf "Don't execute releases on pull request"
   exit 0
fi

if [ "snapshot" == "$TRAVIS_BRANCH" ]; then
     printf "Snapshot branch will deploy snapshot to Maven central"
     mvn -T2 -B -DSTORM_TEST_TIMEOUT_MS=60000 clean deploy
fi

if [ "master" == "$TRAVIS_BRANCH" ]; then 
    printf "Master branch will cut a release to Maven central"
    mkdir -p "/tmp/secrets"
    printf "Extracting SSH Key"
    openssl aes-256-cbc -K $encrypted_e739014d2f1e_key -iv $encrypted_e739014d2f1e_iv -in build/secrets.tar.enc -out /tmp/secrets/secrets.tar -d
    tar xf /tmp/secrets/secrets.tar -C /tmp/secrets/
    
    mkdir -p ~/.ssh
    chmod 700 ~/.ssh
    ls -lh /tmp/secrets/secrets/
    cp /tmp/secrets/secrets/id_rsa ~/.ssh/id_rsa
    chmod 400 ~/.ssh/id_rsa
    
    git remote set-url origin $REPO
    git checkout master || git checkout -b master
    git reset --hard origin/master
    
    git config --global user.name "Travis CI"
    git config --global user.email "$COMMIT_AUTHOR_EMAIL"
    
    echo "Release Build number:$TRAVIS_BUILD_NUMBER" > build/build-info.txt
    echo "Release Commit number:$TRAVIS_COMMIT" >> build/build-info.txt
    git add build/build-info.txt
    git commit -m "[ci skip] Updating build-info file"
    git push
    
    git checkout master || git checkout -b master
    git reset --hard origin/master
    
    gpg --fast-import /tmp/secrets/secrets/codesign.asc >> /dev/null
    
    mvn -T2 -B -DSTORM_TEST_TIMEOUT_MS=60000 -Darguments=-Dgpg.passphrase=$passphrase release:clean release:prepare release:perform --settings settings.xml
fi
