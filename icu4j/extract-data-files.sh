#!/usr/bin/env bash
# Copyright (C) 2023 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# Check that all the .jar files are present
JAR_FILE=main/shared/data/icudata.jar
if [ ! -f "$JAR_FILE" ]; then
  echo "Missing $JAR_FILE"
  exit
fi
JAR_FILE=main/shared/data/icutzdata.jar
if [ ! -f "$JAR_FILE" ]; then
  echo "Missing $JAR_FILE"
  exit
fi
JAR_FILE=main/shared/data/testdata.jar
if [ ! -f "$JAR_FILE" ]; then
  echo "Missing $JAR_FILE"
  exit
fi

# Unpack the pre-built .jar files with data only

echo "Unpacking icudata.jar"
rm -fr      main/core/src/main/resources/com/ibm/icu/impl/data/icudata
unzip -q -d main/core/src/main/resources/ main/shared/data/icudata.jar
rm -fr      main/core/src/main/resources/META-INF

echo "  Moving charset data"
rm   -fr main/charset/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/charset/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/*.cnv        main/charset/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/cnvalias.icu main/charset/src/main/resources/com/ibm/icu/impl/data/icudata/

echo "  Moving currency data"
rm   -fr main/currdata/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/currdata/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/curr         main/currdata/src/main/resources/com/ibm/icu/impl/data/icudata/curr

echo "  Moving collate data"
rm   -fr main/collate/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/collate/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/coll         main/collate/src/main/resources/com/ibm/icu/impl/data/icudata/coll

echo "  Moving langdata data"
rm   -fr main/langdata/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/langdata/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/lang         main/langdata/src/main/resources/com/ibm/icu/impl/data/icudata/lang

echo "  Moving regiondata data"
rm   -fr main/regiondata/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/regiondata/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/region       main/regiondata/src/main/resources/com/ibm/icu/impl/data/icudata/region

echo "  Moving translit data"
rm   -fr main/translit/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/translit/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/translit     main/translit/src/main/resources/com/ibm/icu/impl/data/icudata/translit

echo "  Moving idna data"
rm   -fr main/idna/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/idna/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/*.spp     main/idna/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/uts46.nrm main/idna/src/main/resources/com/ibm/icu/impl/data/icudata/

echo "  Moving spoof data"
rm   -fr main/spoof/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/spoof/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/confusables.cfu main/spoof/src/main/resources/com/ibm/icu/impl/data/icudata/

echo "  Moving segmenter_dict dictionary data"
rm   -fr main/segmenter_dict/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/segmenter_dict/src/main/resources/com/ibm/icu/impl/data/icudata/brkitr/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/brkitr/*.dict main/segmenter_dict/src/main/resources/com/ibm/icu/impl/data/icudata/brkitr/

echo "  Moving number data"
rm   -fr main/number/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/number/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/unit                    main/number/src/main/resources/com/ibm/icu/impl/data/icudata/unit
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/units.res               main/number/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/plurals.res             main/number/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/pluralRanges.res        main/number/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/numberingSystems.res    main/number/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/grammaticalFeatures.res main/number/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/currencyNumericCodes.res main/number/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/genderList.res          main/number/src/main/resources/com/ibm/icu/impl/data/icudata/

echo "  Moving rbnf data"
rm   -fr main/rbnf/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/rbnf/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/rbnf main/rbnf/src/main/resources/com/ibm/icu/impl/data/icudata/rbnf

echo "  Moving datetime data and unpacking icutzdata.jar"
rm   -fr main/datetime/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/datetime/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/zone           main/datetime/src/main/resources/com/ibm/icu/impl/data/icudata/zone
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/dayPeriods.res main/datetime/src/main/resources/com/ibm/icu/impl/data/icudata/
unzip -q -d main/datetime/src/main/resources/ main/shared/data/icutzdata.jar
rm -fr      main/datetime/src/main/resources/META-INF
# timezoneTypes.res is aliased from keyTypeData.res in core
mv       main/datetime/src/main/resources/com/ibm/icu/impl/data/icudata/timezoneTypes.res main/core/src/main/resources/com/ibm/icu/impl/data/icudata/

echo "Unpacking testdata.jar"
rm -fr      main/core/src/test/resources/com/ibm/icu/dev/data/testdata/
unzip -q -d main/core/src/test/resources/ main/shared/data/testdata.jar
rm -fr      main/core/src/test/resources/META-INF

echo "Removing jar files"
echo "  icudata.jar"
rm main/shared/data/icudata.jar
echo "  icutzdata.jar"
rm main/shared/data/icutzdata.jar
echo "  testdata.jar"
rm main/shared/data/testdata.jar

echo DONE
