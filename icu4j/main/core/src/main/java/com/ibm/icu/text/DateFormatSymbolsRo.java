// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.text;

import com.ibm.icu.text.DateFormatSymbols.CapitalizationContextUsage;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Type;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

class DateFormatSymbolsRo implements Serializable, Cloneable {
    private static final long serialVersionUID = -221705994782673958L;

    private final DateFormatSymbols theRealDeal;

    DateFormatSymbolsRo(DateFormatSymbols s) {
        theRealDeal = s;
    }

    @Override
    public String toString() {
        return theRealDeal.toString();
    }

    String[] getEras() {
        return theRealDeal.getEras();
    }

    String[] getEraNames() {
        return theRealDeal.getEraNames();
    }

    String[] getNarrowEras() {
        return theRealDeal.getNarrowEras();
    }

    String[] getMonths() {
        return theRealDeal.getMonths();
    }

    String[] getMonths(int context, int width) {
        return theRealDeal.getMonths(context, width);
    }

    String[] getShortMonths() {
        return theRealDeal.getShortMonths();
    }

    String[] getWeekdays() {
        return theRealDeal.getWeekdays();
    }

    String[] getWeekdays(int context, int width) {
        return theRealDeal.getWeekdays(context, width);
    }

    String[] getShortWeekdays() {
        return theRealDeal.getShortWeekdays();
    }

    String[] getQuarters(int context, int width) {
        return theRealDeal.getQuarters(context, width);
    }

    String[] getYearNames(int context, int width) {
        return theRealDeal.getYearNames(context, width);
    }

    String[] getZodiacNames(int context, int width) {
        return theRealDeal.getZodiacNames(context, width);
    }

    String getLeapMonthPattern(int context, int width) {
        return theRealDeal.getLeapMonthPattern(context, width);
    }

    String[] getAmPmStrings() {
        return theRealDeal.getAmPmStrings();
    }

    String[] getAmPmStrings(int context, int width) {
        return theRealDeal.getAmPmStrings(context, width);
    }

    String getTimeSeparatorString() {
        return theRealDeal.getTimeSeparatorString();
    }

    String[][] getZoneStrings() {
        return theRealDeal.getZoneStrings();
    }

    String getLocalPatternChars() {
        return theRealDeal.getLocalPatternChars();
    }

    @Override
    public DateFormatSymbolsRo clone() {
        return this;
    }

    DateFormatSymbols getDateFormatSymbols() {
        return this.theRealDeal.clone();
    }

    @Override
    public int hashCode() {
        return theRealDeal.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DateFormatSymbolsRo) {
            DateFormatSymbolsRo other = (DateFormatSymbolsRo) obj;
            return Objects.equals(theRealDeal, other.theRealDeal);
        }
        if (obj instanceof DateFormatSymbols) {
            DateFormatSymbols other = (DateFormatSymbols) obj;
            return Objects.equals(theRealDeal, other);
        }
        return false;
    }

    final ULocale getLocale(Type type) {
        return theRealDeal.getLocale(type);
    }

    String[] getAbbreviatedDayPeriods() {
        return theRealDeal.abbreviatedDayPeriods;
    }

    String[] getStandaloneNarrowQuarters() {
        // TODO Auto-generated method stub
        return theRealDeal.standaloneNarrowQuarters;
    }

    String[] getStandaloneQuarters() {
        return theRealDeal.standaloneQuarters;
    }

    String[] getStandaloneShortQuarters() {
        return theRealDeal.standaloneShortQuarters;
    }

    String[] getDayOfMonthCardinalNames(int format, int abbreviated) {
        return theRealDeal.getDayOfMonthCardinalNames(format, abbreviated);
    }

    String[] getDayOfMonthOrdinalNames(int format, int abbreviated) {
        return theRealDeal.getDayOfMonthOrdinalNames(format, abbreviated);
    }

    String[] getStandaloneWeekdays() {
        return theRealDeal.standaloneWeekdays;
    }

    String[] getStandaloneShortWeekdays() {
        return theRealDeal.standaloneShortWeekdays;
    }

    String[] getWideDayPeriods() {
        return theRealDeal.wideDayPeriods;
    }

    String[] getNarrowDayPeriods() {
        return theRealDeal.narrowDayPeriods;
    }

    String[] getAmpmsWide() {
        return theRealDeal.ampmsWide;
    }

    String[] getAmpmsNarrow() {
        return theRealDeal.ampmsNarrow;
    }

    String[] getAmpms() {
        return theRealDeal.ampms;
    }

    String[] getStandaloneShorterWeekdays() {
        return theRealDeal.standaloneShorterWeekdays;
    }

    String[] getLeapMonthPatterns() {
        return theRealDeal.leapMonthPatterns;
    }

    String[] getNarrowQuarters() {
        return theRealDeal.narrowQuarters;
    }

    String[] getQuarters() {
        return theRealDeal.quarters;
    }

    String[] getShortQuarters() {
        return theRealDeal.shortQuarters;
    }

    String[] getNarrowWeekdays() {
        return theRealDeal.narrowWeekdays;
    }

    String[] getShorterWeekdays() {
        return theRealDeal.shorterWeekdays;
    }

    String[] getShortYearNames() {
        return theRealDeal.shortYearNames;
    }

    Map<CapitalizationContextUsage, boolean[]> getCapitalization() {
        return theRealDeal.capitalization;
    }

    String[] getNarrowMonths() {
        return theRealDeal.narrowMonths;
    }

    String[] getStandaloneNarrowMonths() {
        return theRealDeal.standaloneNarrowMonths;
    }

    String[] getStandaloneMonths() {
        return theRealDeal.standaloneMonths;
    }

    String[] getStandaloneNarrowWeekdays() {
        return theRealDeal.standaloneNarrowWeekdays;
    }

    String[] getStandaloneShortMonths() {
        return theRealDeal.standaloneShortMonths;
    }

    // Serialization
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
    }
}
