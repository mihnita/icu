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
import java.util.Date;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class MFCustomDeepDateTest extends TestFmwk {

    /*
     * Example: works in nested messages
     * Showing that it works "deep", inside a <code>select</code> or <code>plural</code>
     * If I use {today, date, ::yMd} then
     *   DateFormat df = SimpleDateFormat.getInstanceForSkeleton("yMMMd", locale)
     *   setFormatByArgumentName("today", new DateFormat)
     * then the parameter outside the select if affected, but the ones inside are not.
     */

    static class DeepDateFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;
        private final SimpleDateFormat df;

        static class Builder implements MessageFormat.CustomFormatBuilder {
            @Override
            public Format build(ULocale locale, String argName, String style) {
                return new DeepDateFormat(locale, argName, style);
            }
        }

        public DeepDateFormat(ULocale ulocale, String argName, String style) {
            super(ulocale, argName, style);
            df = style.startsWith("::")
                ? (SimpleDateFormat) SimpleDateFormat.getInstanceForSkeleton(style.substring(2), ULocale.US)
                : new SimpleDateFormat(style, ULocale.US);
        }

        @Override
        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            toAppendTo.append("<<");
            df.format(obj, toAppendTo, pos);
            toAppendTo.append(">>");
            return toAppendTo;
        }
    }

    static class FirstLastNameFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;

        static class Builder implements MessageFormat.CustomFormatBuilder {
            @Override
            public Format build(ULocale locale, String argName, String style) {
                return new FirstLastNameFormat(locale, argName, style);
            }
        }

        public FirstLastNameFormat(ULocale ulocale, String argName, String style) {
            super(ulocale, argName, style);
        }

        @Override
        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj instanceof Name) {
                Name name = (Name) obj;
                if ("last".equals(style)) {
                    toAppendTo.append(name.getLastName());
                } else {
                    toAppendTo.append(name.getFirstName());
                }
            } else {
                toAppendTo.append("" + obj);
            }
            return toAppendTo;
        }
    }

    // Using a custom format (firstLastName, deepDateFormat) inside a standard ICU select
    @Test
    public void testThatDeepNesting() {
        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("firstLastName", new FirstLastNameFormat.Builder());
        formatterMap.put("deepDateFormat", new DeepDateFormat.Builder());

        ULocale locale = ULocale.US;

        Calendar cal = new GregorianCalendar(2019, Calendar.OCTOBER, 25);
        Date today = cal.getTime();
        MessageFormat mf = new MessageFormat(
            "{gender, select,"
            + " female {Hello Ms. {name,firstLastName,last}, today is {today, deepDateFormat, ::yMd}}"
            + "   male {Hello Mr. {name,firstLastName,last}, today is {today, deepDateFormat, ::yMd}}"
            + "  other {Hello {name,firstLastName,first}, today is {today, deepDateFormat, ::yMd}}"
            + "} or {today, deepDateFormat,::yMd}!",
            locale, formatterMap);

        HashMap<String, Object> args = new HashMap<>();

        args.put("today", today);

        args.put("name", new Name("Steve", "Johnson"));
        args.put("gender", "male");
        assertEquals("recursive",
            "Hello Mr. Johnson, today is <<10/25/2019>> or <<10/25/2019>>!",
            mf.format(args));

        args.put("name", new Name("Jane", "Johnson"));
        args.put("gender", "female");
        assertEquals("recursive",
            "Hello Ms. Johnson, today is <<10/25/2019>> or <<10/25/2019>>!",
            mf.format(args));

        args.put("name", new Name("Anonymous", "Johnson"));
        args.put("gender", "other");
        assertEquals("recursive",
            "Hello Anonymous, today is <<10/25/2019>> or <<10/25/2019>>!",
            mf.format(args));
    }
}
