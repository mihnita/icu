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

import java.text.Format;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SelectFormat;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class MFCustomSelectorTest extends TestFmwk {

    /*
     * Example: implementing a selector (<code>complexArg</code>)
     * Showing that it is also easy to implement a selector kind of <code>argType</code>.
     * This implements a <code>{..., gender, ...}</code> syntax working similar to <code>select</code>
     */

    static class GenderFormat extends SelectFormat {
        static final long serialVersionUID = -1;

        static class Builder implements MessageFormat.CustomFormatBuilder {
            @Override
            public Format build(ULocale locale, String argName, String style) {
                return new GenderFormat(locale, argName, style);
            }
        }

        public GenderFormat(ULocale ulocale, String argName, String style) {
            super(style);
        }
    }

    @Test
    public void TestCustomTypeComplexArgument() {
        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("gender", new GenderFormat.Builder());
        ULocale locale = ULocale.US;

        MessageFormat mf = new MessageFormat(
            "{host_gender, gender,"
            + " female {She invited you}"
            + "   male {He invited you}"
            + "  other {They invited you}"
            + "}",
            locale, formatterMap);

        HashMap<String, Object> args = new HashMap<>();

        args.put("host_gender", "female");
        assertEquals("gender", "She invited you", mf.format(args));

        args.put("host_gender", "male");
        assertEquals("gender", "He invited you", mf.format(args));

        args.put("host_gender", "other");
        assertEquals("gender", "They invited you", mf.format(args));

        args.put("host_gender", "something");
        assertEquals("gender", "They invited you", mf.format(args));
    }
}
