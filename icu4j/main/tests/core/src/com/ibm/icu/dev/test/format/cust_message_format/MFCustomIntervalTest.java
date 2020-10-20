// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
**********************************************************************
* Author: Mihai Nita
* Created: October 20, 2020
* Since: ICU TBD (exploratory)
**********************************************************************
*/
package com.ibm.icu.dev.test.format.cust_message_format;

import java.text.FieldPosition;
import java.text.Format;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DateIntervalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.DateInterval;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class MFCustomIntervalTest extends TestFmwk {

    /*
     * Example: interval formatting
     * Shows how you can have a "function" that takes more than a parameter
     * OK, it is cheating: take an Object :-) But this is an already established ICU pattern.
     */

    static class DTIntervalFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;
        private final DateIntervalFormat dif;

        static class Builder implements MessageFormat.CustomFormatBuilder {
            @Override
            public Format build(ULocale locale, String argName, String style) {
                return new DTIntervalFormat(locale, argName, style);
            }
        }

        public DTIntervalFormat(ULocale ulocale, String argName, String style) {
            super(ulocale, argName, style);
            dif = DateIntervalFormat.getInstance(style, ulocale);
        }

        @Override
        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            // ICU axpects the obj to be DateInterval.
            // If it is not, it throws. So there is no point to test & throw here.
            dif.format(obj, toAppendTo, pos);
            return toAppendTo;
        }
    }

    @Test
    public void TestDateTimeInterval() {
        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("interval", new DTIntervalFormat.Builder());

        long from = new GregorianCalendar(2020, Calendar.SEPTEMBER, 12).getTimeInMillis();
        long toSameMonth = new GregorianCalendar(2020, Calendar.SEPTEMBER, 21).getTimeInMillis();
        long toSameYear = new GregorianCalendar(2020, Calendar.NOVEMBER, 13).getTimeInMillis();
        long toDifferentYear = new GregorianCalendar(2021, Calendar.FEBRUARY, 2).getTimeInMillis();

        ULocale locale = ULocale.forLanguageTag("en");

        MessageFormat mf = new MessageFormat("Offer valid {validDates, interval, ::dMMMMy}",
                locale, formatterMap);

        HashMap<String, Object> args = new HashMap<>();

        args.put("validDates", new DateInterval(from, from)); // same day
        assertEquals("Interval formatting",
            "Offer valid September 12, 2020", mf.format(args));

        args.put("validDates", new DateInterval(from, toSameMonth));
        assertEquals("Interval formatting",
            "Offer valid September 12 – 21, 2020", mf.format(args));

        args.put("validDates", new DateInterval(from, toSameYear));
        assertEquals("Interval formatting",
            "Offer valid September 12 – November 13, 2020", mf.format(args));

        args.put("validDates", new DateInterval(from, toDifferentYear));

        assertEquals("Interval formatting",
            "Offer valid September 12, 2020 – February 2, 2021", mf.format(args));
    }
}
