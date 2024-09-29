// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.io.Reader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;

@SuppressWarnings({"static-method", "javadoc"})
@RunWith(JUnit4.class)
public class CoreTest extends CoreTestFmwk {
    private static final String[] JSON_FILES = {"message2/alias-selector-annotations.json",
                                                "message2/duplicate-declarations.json",
                                                "message2/icu-parser-tests.json",
                                                "message2/icu-test-functions.json",
                                                "message2/icu-test-previous-release.json",
                                                "message2/icu-test-selectors.json",
                                                "message2/invalid-number-literals-diagnostics.json",
                                                "message2/invalid-options.json",
                                                "message2/markup.json",
                                                "message2/matches-whitespace.json",
                                                "message2/more-data-model-errors.json",
                                                "message2/more-functions.json",
                                                "message2/resolution-errors.json",
                                                "message2/runtime-errors.json",
                                                "message2/spec/data-model-errors.json",
                                                "message2/spec/syntax-errors.json",
                                                "message2/spec/syntax.json",
                                                "message2/spec/functions/date.json",
                                                "message2/spec/functions/datetime.json",
                                                "message2/spec/functions/integer.json",
                                                "message2/spec/functions/number.json",
                                                "message2/spec/functions/string.json",
                                                "message2/spec/functions/time.json",
                                                "message2/syntax-errors-diagnostics.json",
                                                "message2/syntax-errors-diagnostics-multiline.json",
                                                "message2/syntax-errors-end-of-input.json",
                                                "message2/syntax-errors-reserved.json",
                                                "message2/tricky-declarations.json",
                                                "message2/unsupported-expressions.json",
                                                "message2/unsupported-statements.json",
                                                "message2/valid-tests.json"};

    @Test
    public void test() throws Exception {
        for (String jsonFile : JSON_FILES) {
            try (Reader reader = TestUtils.jsonReader(jsonFile)) {
                MF2Test tests = TestUtils.GSON.fromJson(reader, MF2Test.class);
                for (Unit unit : tests.tests) {
                    TestUtils.runTestCase(tests.defaultTestProperties, unit);
                }
            }
        }
    }
}
