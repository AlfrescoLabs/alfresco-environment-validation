#!/bin/sh
VERSION=1.01
NAME=Alfresco4.0-evt-${VERSION}

# Build (afresh)
mvn clean package

# Package
#TODO: Move this into Maven build script
mkdir -p target/package/${NAME}/
cp -R src/main/lib/ target/package/${NAME}/lib/
cp target/*.jar target/package/${NAME}/lib/
cp src/main/scripts/* target/package/${NAME}/
cp src/main/docs/* target/package/${NAME}/
pushd target/package/
zip -9 -r ../${NAME}.zip ${NAME}
popd > /dev/null
echo "\nEVT distributable package may be found at 'target/${NAME}.zip'\n"