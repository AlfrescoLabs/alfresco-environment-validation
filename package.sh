#!/bin/sh
VERSION=4.0-SNAPSHOT-v1

# Build (afresh)
mvn clean package

# Package
#TODO: Move this into Maven build script
mkdir -p target/package/evt-${VERSION}/
cp -R src/main/lib/ target/package/evt-${VERSION}/lib/
cp target/*.jar target/package/evt-${VERSION}/lib/
cp src/main/scripts/* target/package/evt-${VERSION}/
cp src/main/docs/* target/package/evt-${VERSION}/
pushd target/package/
zip -9 -r ../evt-${VERSION}.zip evt-${VERSION}
popd > /dev/null
echo "\nEVT distributable package may be found at 'target/evt-${VERSION}.zip'\n"