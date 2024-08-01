// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2001-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

/**
 * Port From:   ICU4C v1.8.1 : format : DateFormatTest
 * Source File: $ICU4CRoot/source/test/intltest/dtfmttst.cpp
 **/

package com.ibm.icu.dev.test.format;

import java.time.LocalDate;
import java.time.Month;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.JavaTimeConverters;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;

@RunWith(JUnit4.class)
public class JavaTimeFormatCvtTest extends CoreTestFmwk {

    private final static int[] DATE_ONLY_FIELDS = {
            Calendar.DAY_OF_MONTH, Calendar.MONTH, Calendar.YEAR
    };
    private final static int[] DATE_TIME_FIELDS = {
            Calendar.DAY_OF_MONTH, Calendar.MONTH, Calendar.YEAR, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND
    };

    /**
     * Very early draft.
     * Just dumping the results to see that everything works, not proper tests.
     */
    @Test
    public void TestTemporal() {
        Calendar calExected = new GregorianCalendar(2024, Calendar.SEPTEMBER, 23, 21, 42, 57);
//        calExected.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));

        LocalDate ld = LocalDate.of(2024, Month.SEPTEMBER, 23);

        Calendar calendar = JavaTimeConverters.temporalToCalendar(ld);

        assertCalendarsEquals(calExected, calendar, DATE_ONLY_FIELDS);
//        assertCalendarsEquals(calExected, calendar, DATE_TIME_FIELDS);
    }

    private static void assertCalendarsEquals(Calendar calExected, Calendar calendar, int[] fieldsToCheck) {
        for (int field : fieldsToCheck) {
            int fieldExpected = calExected.get(field);
            int fieldActual = calendar.get(field);
            assertEquals("bad conversion", fieldExpected, fieldActual);
        }
    }
}
