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

import java.text.FieldPosition;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.CalType;
import com.ibm.icu.message2.MessageFormatter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateIntervalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.Calendar;

@RunWith(JUnit4.class)
public class JavaTimeFormatTest extends CoreTestFmwk {
    /**
     * Very early draft.
     * Just dumping the results to see that everything works, not proper tests.
     */
    @Test
    public void TestTemporal() {
        Locale locale = Locale.US;
        DateFormat df;

        DateFormat dFmt = DateFormat.getDateInstance(DateFormat.LONG, locale);
        DateFormat tFmt = DateFormat.getTimeInstance(DateFormat.LONG, locale);
        DateFormat dtFmt = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);

        System.out.println("=== skeleton & style, date =======");
        df = DateFormat.getInstanceForSkeleton("EEEEyMMMMd", locale);
        LocalDate ld = LocalDate.of(1943, Month.MARCH, 16);
        System.out.println(df.format(ld));
        System.out.println(dFmt.format(ld));

        System.out.println("=== skeleton & styles, datetime =======");
        df = DateFormat.getInstanceForSkeleton("EEEEyMMMMd jmsSSS", locale);
        LocalDateTime ldt = LocalDateTime.of(1943, Month.MARCH, 16, 21, 42, 57, 1234567);
        System.out.println(df.format(ldt));
        System.out.println(dtFmt.format(ldt));
        System.out.println(tFmt.format(ldt));

        System.out.println("=== skeleton, with tz =======");
        df = DateFormat.getInstanceForSkeleton("EEEEyMMMMd jmsSSS vvvv", locale);
        ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.of("Europe/Paris"));
        System.out.println(df.format(zdt));
        OffsetDateTime odt = OffsetDateTime.of(ldt, ZoneOffset.ofHoursMinutes(5, 30));
        System.out.println(df.format(odt));

        System.out.println("=== full styles, with tz =======");
        tFmt = DateFormat.getTimeInstance(DateFormat.FULL, locale);
        dtFmt = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locale);
        System.out.println(tFmt.format(zdt));
        System.out.println(dtFmt.format(zdt));
        System.out.println(tFmt.format(odt));
        System.out.println(dtFmt.format(odt));

        System.out.println("=== non-gregorian as input =======");
        HijrahDate hd = HijrahDate.from(ld);
        JapaneseDate jd = JapaneseDate.from(ld);
        MinguoDate md = MinguoDate.from(ld);
        ThaiBuddhistDate td = ThaiBuddhistDate.from(ld);
        df = DateFormat.getInstanceForSkeleton("EEEEGGGyMMMMd", locale);
        System.out.println(df.format(hd));
        System.out.println(df.format(jd));
        System.out.println(df.format(md));
        System.out.println(df.format(td));

        System.out.println("=== non-gregorian as output (from locale) =======");
        for (CalType ct : CalType.values()) {
            String calId = ct.getId();
            locale = Locale.forLanguageTag("fr-u-ca-" + calId);
            df = DateFormat.getInstanceForSkeleton("EEEEGGGGyMMMMd", locale);
            System.out.printf("%-12s : %s%n", calId, df.format(ldt));
            System.out.printf("%-12s : %s%n", calId, df.format(ld));
            System.out.printf("%-12s : %s%n", calId, df.format(hd));
            System.out.printf("%-12s : %s%n", calId, df.format(jd));
            System.out.printf("%-12s : %s%n", calId, df.format(md));
            System.out.printf("%-12s : %s%n", calId, df.format(td));
        }

        System.out.println("=== MessageFormat =======");
        locale = Locale.FRANCE;
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("expDate", ld);

        MessageFormat mf = new MessageFormat("Your card expires on {expDate}", locale);
        System.out.println(mf.format(arguments));
        mf = new MessageFormat("Your card expires on {expDate, date}", locale);
        System.out.println(mf.format(arguments));
        mf = new MessageFormat("Your card expires on {expDate, date, FULL}", locale);
        System.out.println(mf.format(arguments));
        mf = new MessageFormat("Your card expires on {expDate, date, ::EEEEyMMMMd}", locale);
        System.out.println(mf.format(arguments));

        System.out.println("=== MessageFormatter (MF2) =======");
        MessageFormatter.Builder mf2Builder = MessageFormatter.builder().setPattern("(mf2) Your card expires on {$expDate :datetime dateStyle=long}").setLocale(locale);

        MessageFormatter mf2 = mf2Builder.build();
        System.out.println(mf2.formatToString(arguments));

        mf2 = mf2Builder.setPattern("(mf2) Your card expires on {$expDate :date icu:skeleton=EEEEyMMMMd}").build();
        System.out.println(mf2.formatToString(arguments));

        mf2 = mf2Builder.setPattern("(mf2) Your card expires on {$expDate :date}").build();
        System.out.println(mf2.formatToString(arguments));

        mf2 = mf2Builder.setPattern("(mf2) Your card expires on {$expDate}").build();
        System.out.println(mf2.formatToString(arguments));

        arguments.put("expDate", Calendar.getInstance());
        mf2 = mf2Builder.setPattern("(mf2) Your card expires on {$expDate}").build();
        System.out.println(mf2.formatToString(arguments));

        System.out.println("=== DateIntervalFormat =======");
        String intervalSkeleton = "dMMMMy";
        LocalDate from = LocalDate.of(2024, Month.SEPTEMBER, 17);
        LocalDate to = LocalDate.of(2024, Month.OCTOBER, 3);
        StringBuffer result = new StringBuffer();

        locale = Locale.forLanguageTag("fr");
        DateIntervalFormat di = DateIntervalFormat.getInstance(intervalSkeleton, locale);
        System.out.println(di.format(from, to, result, new FieldPosition(0)));

        locale = Locale.forLanguageTag("fr-u-ca-hebrew");
        di = DateIntervalFormat.getInstance(intervalSkeleton, locale);
        result.setLength(0);
        System.out.println(di.format(from, to, result, new FieldPosition(0)));

        locale = Locale.forLanguageTag("fr-u-ca-coptic");
        di = DateIntervalFormat.getInstance(intervalSkeleton, locale);
        result.setLength(0);
        System.out.println(di.format(from, to, result, new FieldPosition(0)));
    }
}
