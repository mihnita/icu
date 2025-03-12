#!/usr/bin/env bash

rm target/icu4j77.api3.gz
rm spy.log

mvn clean -q --batch-mode
mvn install -q --batch-mode -DskipITs -DskipTests -P with_javadoc
mvn install -q --batch-mode -DskipITs -DskipTests -f tools/build
mvn site -q  --batch-mode -DskipITs -DskipTests -P gatherapi

mkdir target/icu4j77.api3.gz ~/third_party/icu_work/javadoc/jdk8/$1
mv target/icu4j77.api3.gz ~/third_party/icu_work/javadoc/jdk8/$1
mv spy.log ~/third_party/icu_work/javadoc/jdk8/$1
ls -ali ~/third_party/icu_work/javadoc/jdk8/$1

