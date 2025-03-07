#!/usr/bin/env bash

rm spy.log
rm visit.log
# touch spy.log
mvn clean install -q -DskipTests -DskipITs -f tools/build/
mvn site -q  --batch-mode -DskipITs -DskipTests -P gatherapi >1 2>2
# > /dev/null
ls -l 1
ls -l 2

