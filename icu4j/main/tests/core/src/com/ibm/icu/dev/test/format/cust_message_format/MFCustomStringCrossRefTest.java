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
import java.util.Objects;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class MFCustomStringCrossRefTest extends TestFmwk {

    /*
     * Example: referencing a different message, possibly handling grammatical case in the same time
     */

    static class XRefFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;
        private final Properties prop;

        static class Builder implements MessageFormat.CustomFormatBuilder {
            private final Properties prop;

            public Builder(Properties prop) {
                this.prop = prop;
            }

            @Override
            public Format build(ULocale locale, String argName, String style) {
                return new XRefFormat(locale, argName, style, prop);
            }
        }

        public XRefFormat(ULocale ulocale, String argName, String style, Properties prop) {
            super(ulocale, argName, style);
            this.prop = prop;
        }

        @Override
        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String argValue = (String) prop.get(argName + "." + style);
            if (argValue == null) {
                argValue = (String) prop.get(argName);
            }
            if (argValue == null) {
                String show = (obj == null) ? "{" + argName + "}" : Objects.toString(obj);
                if (show.isEmpty())
                    show = "{" + argName + "}";
                toAppendTo.append(show);
            } else {
                toAppendTo.append(argValue);
            }
            return toAppendTo;
        }
    }

    @Test
    public void TestCustomTypeXRef() {
        // Resources. This to be (or course) replaced with whatever the "resource manager"
        // is on the hosting platform. For instance Android would use Resources.getString, etc.
        Properties prop = new Properties();
        // prop.setProperty("brandName.nominative", "Konto Firefox");
        prop.setProperty("brandName", "Konto Firefox");
        prop.setProperty("brandName.genitive", "Konta Firefox");
        prop.setProperty("brandName.accusative", "Kontem Firefox");

        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("xref", new XRefFormat.Builder(prop));

        HashMap<String, Object> args = new HashMap<>();
        args.put("company", "unknown company"); // Used as "ultimate fallback

        ULocale locale = ULocale.forLanguageTag("pl");

        MessageFormat mfXref = new MessageFormat("Zaloguj do {brandName,xref,genitive}",
                locale, formatterMap);
        assertEquals("Cross-reference to a sting in resources",
            "Zaloguj do Konta Firefox", mfXref.format(args));

        mfXref = new MessageFormat("Zaloguj do {brandName,xref,dative}", // dative does not exist, fallback
                locale, formatterMap);
        assertEquals("Cross-reference to a sting in resources",
            "Zaloguj do Konto Firefox", mfXref.format(args));

        // TBD what we really want to do when we can't find the string in resources
        mfXref = new MessageFormat("Zaloguj do {company,xref,dative}", // company does not exist, has fallback
                locale, formatterMap);
        assertEquals("Cross-reference to a sting in resources",
            "Zaloguj do unknown company", mfXref.format(args));

        // The current result is not great. This is what we really want?
        mfXref = new MessageFormat("Zaloguj do {entity,xref,dative}", // entity does not exist
                locale, formatterMap);
        assertEquals("Cross-reference to a sting in resources",
            "Zaloguj do {entity}", mfXref.format(args));
    }
}
