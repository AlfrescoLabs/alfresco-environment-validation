#!/bin/sh
VERSION=0.9-SNAPSHOT

mvn clean

if [ -e evt-${VERSION}.zip ]; then
  rm evt-${VERSION}.zip
fi
