#!/bin/sh
VERSION=0.9-SNAPSHOT

# Build
mvn package

# Package
#TODO: Move this into Maven build script
mkdir -p target/package/evt-${VERSION}
cp -R lib/ target/package/evt-${VERSION}/lib/
cp target/environment-validation-*.jar target/package/evt-${VERSION}/lib/
cp evt.sh target/package/evt-${VERSION}/
cp evt.cmd target/package/evt-${VERSION}/
cp README.txt target/package/evt-${VERSION}/
pushd target/package
zip -9 -r ../../evt-${VERSION}.zip evt-${VERSION}
popd
