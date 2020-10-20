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
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class MFCustomPoliteNameTest extends TestFmwk {

    /*
     * Example: user defined class & formatter
     * Formatting a user defined class (<code>Name</code>) using an <code>argStyle</code>
     * (<code>formal</code> / <code>casual</code>)
     * So we can do <code>{...,politeness,formal}</code>, very similar to the way we
     * do <code>{...,date,long}</code>
     */

    static class PoliteNameFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;

        static class Builder implements MessageFormat.CustomFormatBuilder {
            @Override
            public Format build(ULocale locale, String argName, String style) {
                return new PoliteNameFormat(locale, argName, style);
            }
        }

        public PoliteNameFormat(ULocale ulocale, String argName, String style) {
            super(ulocale, argName, style);
        }

        @Override
        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj instanceof Name) {
                Name name = (Name) obj;
                if ("formal".equals(style)) {
                    // This would of course should not be hardcoded.
                    // Should come from the name, might be translated, might depend on gender,
                    // and the position / spaces are locale-sensitive (think "Tanaka San")
                    // But that is not the point here, it can be refined later.
                    toAppendTo.append("Mr. " + name.getLastName());
                } else {
                    toAppendTo.append(name.getFirstName());
                }
            } else {
                toAppendTo.append("" + obj);
            }
            return toAppendTo;
        }
    }

    @Test
    public void TestCustomTypeNames() {
        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("politeness", new PoliteNameFormat.Builder());

        HashMap<String, Object> args = new HashMap<>();

        ULocale locale = ULocale.forLanguageTag("ja");

        MessageFormat mfFormal = new MessageFormat(
            "Hello {jaName,politeness,formal}, hello {enName,politeness,formal}",
            locale, formatterMap);
        MessageFormat mfCasual = new MessageFormat(
            "Hello {jaName,politeness,casual}, hello {enName,politeness,casual}",
            locale, formatterMap);

        Name name = new Name("Taro", "Yamada");
        args.put("jaName", name);
        args.put("enName", "John"); // not a Name object

        assertEquals("polite format("+ name + ",formal)",
            "Hello Mr. Yamada, hello John", mfFormal.format(args));

        assertEquals("polite format("+ name + ",casual)",
            "Hello Taro, hello John", mfCasual.format(args));
    }
}
