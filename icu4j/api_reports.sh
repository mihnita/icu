#!/usr/bin/env bash

rm target/icu4j77.api3
rm spy.log

# echo Clean icu
# mvn clean -q -o --batch-mode
# mvn install -q -o --batch-mode -DskipITs -DskipTests -P with_javadoc

# echo Build icu
# mvn install -q -o --batch-mode -DskipITs -DskipTests

echo Build tools
mvn install -q -o --batch-mode -DskipITs -DskipTests -f tools/build

echo Gather API
mvn site -q -o  --batch-mode -DskipITs -DskipTests -P gatherapi

echo Save results
mkdir ~/third_party/icu_work/javadoc/jdk11/$1
mv target/icu4j77.api3 ~/third_party/icu_work/javadoc/jdk11/$1
mv spy.log ~/third_party/icu_work/javadoc/jdk11/$1
ls -ali ~/third_party/icu_work/javadoc/jdk11/$1

