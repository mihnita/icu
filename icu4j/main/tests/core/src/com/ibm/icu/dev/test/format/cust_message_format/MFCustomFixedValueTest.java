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
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class MFCustomFixedValueTest extends TestFmwk {

    /*
     * Example: syntactic sugar to use "fixed values" (not parameters)
     * Same as hard-coding the value ("expires in 30 days"), but still formatted
     * at runtime. Reasons: we translate into ar/es/fr/en, but there are 40+ countries
     * using French, 20+ for Arabic & Spanish, > 100 for English.
     * There are also systems (Windows, Android) where the digit types is a user preference.
     * Same for calendars, time formats, etc.
     */

    static class UseTheNameFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;

        static class Builder implements MessageFormat.CustomFormatBuilder {
            @Override
            public Format build(ULocale locale, String argName, String style) {
                return new UseTheNameFormat(locale, argName, style);
            }
        }

        public UseTheNameFormat(ULocale ulocale, String argName, String style) {
            super(ulocale, argName, style);
        }

        @Override
        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (argName.startsWith("N")) { // number
                Double val = Double.valueOf(argName.substring(1).replace("_", "."));
                NumberFormat nf = NumberFormat.getInstance(ulocale);
                toAppendTo.append(nf.format(val));
            } else if (argName.startsWith("D")) { // very primitive date parsing, iso
                String [] parts = argName.substring(1).split("T", 2);
                String date = "";
                String time = "";
                if (parts.length == 2) {
                    date = parts[0];
                    time = parts[1];
                } else {
                    if (parts[0].length() == 8) { // date
                        date = parts[0];
                    } else if (parts[0].length() == 6) { // time
                        time = parts[0];
                    }
                }
                Date now = new Date();
                if (!date.isEmpty()) {
                    int year = Integer.valueOf(date.substring(0, 4));
                    int month = Integer.valueOf(date.substring(4, 6));
                    int day = Integer.valueOf(date.substring(6, 8));
                    now.setYear(year - 1900);
                    now.setMonth(month - 1);
                    now.setDate(day);
                }
                if (!time.isEmpty()) {
                    int hour = Integer.valueOf(date.substring(0, 2));
                    int minute = Integer.valueOf(date.substring(2, 4));
                    int second = Integer.valueOf(date.substring(4, 6));
                    now.setHours(hour);
                    now.setMinutes(minute);
                    now.setSeconds(second);
                }
                DateFormat df = DateFormat.getInstanceForSkeleton(style, ulocale);
                toAppendTo.append(df.format(now));
            }
            return toAppendTo;
        }
    }

    @Test
    public void TestCustomTypeNameDependent() {
        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("inline", new UseTheNameFormat.Builder());

        HashMap<String, Object> args = new HashMap<>();

        ULocale locale = ULocale.forLanguageTag("en-IN");

        MessageFormat mfXref = new MessageFormat("This expires on {D20200923T214215,inline,yMMMdjm}",
                locale, formatterMap);
        assertEquals("Cross-reference to a sting in resources",
            "This expires on 23 Sep 2020, 8:20 pm", mfXref.format(args));

        mfXref = new MessageFormat("The population is {N123456789_9876,inline,{x:'a', b:'xxrr'}}",
                locale, formatterMap);
        assertEquals("Cross-reference to a sting in resources",
                "The population is 12,34,56,789.988", mfXref.format(args));
    }
}
