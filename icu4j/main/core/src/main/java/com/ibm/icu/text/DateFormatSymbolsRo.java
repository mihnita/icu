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
import java.util.Map;

interface DateFormatSymbolsRo {
    public Map<CapitalizationContextUsage, boolean[]> getCapitalization();
    public String getLeapMonthPattern(int context, int width);
    public String getLocalPatternChars();
    public String getTimeSeparatorString();
    public ULocale getLocale(Type type);
    public String[] getAbbreviatedDayPeriods();
    public String[] getAmPmStrings();
    public String[] getAmPmStrings(int context, int width);
    public String[] getAmpms();
    public String[] getAmpmsNarrow();
    public String[] getAmpmsWide();
    public String[] getDayOfMonthCardinalNames(int format, int abbreviated);
    public String[] getDayOfMonthOrdinalNames(int format, int abbreviated);
    public String[] getEraNames();
    public String[] getEras();
    public String[] getLeapMonthPatterns();
    public String[] getMonths();
    public String[] getMonths(int context, int width);
    public String[] getNarrowDayPeriods();
    public String[] getNarrowEras();
    public String[] getNarrowMonths();
    public String[] getNarrowQuarters();
    public String[] getNarrowWeekdays();
    public String[] getQuarters();
    public String[] getQuarters(int context, int width);
    public String[] getShortMonths();
    public String[] getShortQuarters();
    public String[] getShortWeekdays();
    public String[] getShortYearNames();
    public String[] getShorterWeekdays();
    public String[] getStandaloneMonths();
    public String[] getStandaloneNarrowMonths();
    public String[] getStandaloneNarrowQuarters();
    public String[] getStandaloneNarrowWeekdays();
    public String[] getStandaloneQuarters();
    public String[] getStandaloneShortMonths();
    public String[] getStandaloneShortQuarters();
    public String[] getStandaloneShortWeekdays();
    public String[] getStandaloneShorterWeekdays();
    public String[] getStandaloneWeekdays();
    public String[] getWeekdays();
    public String[] getWeekdays(int context, int width);
    public String[] getWideDayPeriods();
    public String[] getYearNames(int context, int width);
    public String[] getZodiacNames(int context, int width);
    public String[][] getZoneStrings();
}
