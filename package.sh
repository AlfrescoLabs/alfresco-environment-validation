#!/bin/sh
VERSION=0.9-SNAPSHOT

# Build (afresh)
mvn clean package

# Package
#TODO: Move this into Maven build script
mkdir -p target/package/evt-${VERSION}/
cp -R lib/ target/package/evt-${VERSION}/lib/
cp target/environment-validation-*.jar target/package/evt-${VERSION}/lib/
cp src/main/scripts/* target/package/evt-${VERSION}/
cp src/main/docs/* target/package/evt-${VERSION}/
pushd target/package/
zip -9 -r ../evt-${VERSION}.zip evt-${VERSION}
popd > /dev/null
