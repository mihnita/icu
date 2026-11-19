// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test;

import com.ibm.icu.util.ULocale;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * A base class for testing within the cross-component tests in the `common_tests` component.
 *
 * <p>This subclass of TestFmwk allows the usage of functionality from the `core` component.
 * TestFmwk is not able to depend on `core` since it is located in `framework`, while `core` tests
 * already use TestFmwk, which requires that `core` depends on `framework`. We cannot have a cycle
 * in our dependencies.
 *
 * <p>With the allowance for this test class to use functionality from components, we can do things
 * like assert whether tests are properly resetting the ICU and JDK time zones to their original
 * values at the end of their execution.
 */
public class CoreTestFmwk extends TestFmwk {

    private static final IcuTimeZoneHelper DEFAULT_ICU_TZ_HELPER =
            IcuTimeZoneHelper.forClassName("com.ibm.icu.util.TimeZone");

    private final IcuTimeZoneHelper icuTzHelper;

    private String testStartDefaultIcuTzId;

    private java.util.TimeZone testStartDefaultJdkTz;

    private com.ibm.icu.util.ULocale testStartDefaultULocale;

    private java.util.Locale testStartDefaultLocale;

    @Rule public TestName name = new TestName();

    public CoreTestFmwk() {
        this(DEFAULT_ICU_TZ_HELPER);
    }

    CoreTestFmwk(IcuTimeZoneHelper icuTzHelper) {
        this.icuTzHelper = icuTzHelper;
    }

    @Override
    public void localTestInitialize() {
        super.localTestInitialize();

        // Just like TestFmwk initializes JDK TimeZone and Locale before every test,
        // do the same for ICU TimeZone (when present on classpath) and ULocale.
        ULocale.setDefault(ULocale.forLocale(defaultLocale));
        if (icuTzHelper != null) {
            icuTzHelper.setDefaultById(defaultTimeZone.getID());
            testStartDefaultIcuTzId = icuTzHelper.getDefaultId();
        } else {
            testStartDefaultIcuTzId = null;
        }

        // Save starting JDK timezone
        testStartDefaultJdkTz = java.util.TimeZone.getDefault();

        // Save starting locales
        testStartDefaultULocale = com.ibm.icu.util.ULocale.getDefault();
        testStartDefaultLocale = java.util.Locale.getDefault();
    }

    @Override
    public void localTestTeardown() {
        String testMethodName = name.getMethodName();

        // Assert that timezones are in a good state

        java.util.TimeZone testEndDefaultJdkTz = java.util.TimeZone.getDefault();

        if (icuTzHelper != null) {
            String testEndDefaultIcuTzId = icuTzHelper.getDefaultId();

            assertEquals(
                    "In [" + testMethodName + "] Test should keep in sync ICU & JDK TZs",
                    testEndDefaultIcuTzId,
                    testEndDefaultJdkTz.getID());

            assertEquals(
                    "In [" + testMethodName + "] Test should reset ICU default TZ",
                    testStartDefaultIcuTzId,
                    testEndDefaultIcuTzId);
        }
        assertEquals(
                "In [" + testMethodName + "] Test should reset JDK default TZ",
                testStartDefaultJdkTz.getID(),
                testEndDefaultJdkTz.getID());

        // Assert that locales are in a good state

        com.ibm.icu.util.ULocale testEndDefaultULocale = com.ibm.icu.util.ULocale.getDefault();
        java.util.Locale testEndDefaultLocale = java.util.Locale.getDefault();

        assertEquals(
                "In [" + testMethodName + "] Test should reset ICU ULocale",
                testStartDefaultULocale.toLanguageTag(),
                testEndDefaultULocale.toLanguageTag());
        assertEquals(
                "In [" + testMethodName + "] Test should reset JDK Locale",
                testStartDefaultLocale.toLanguageTag(),
                testEndDefaultLocale.toLanguageTag());

        super.localTestTeardown();
    }

    /**
     * Reflective bridge to {@code com.ibm.icu.util.TimeZone} so {@link CoreTestFmwk} can be used by
     * modules that do not depend on the datetime/timezone component.
     */
    @Ignore // Prevent this helper class from being detected as a JUnit test case class
    static final class IcuTimeZoneHelper {
        private final Method getTimeZoneMethod;
        private final Method setDefaultMethod;
        private final Method getDefaultMethod;
        private final Method getIdMethod;

        private IcuTimeZoneHelper(
                Method getTimeZoneMethod,
                Method setDefaultMethod,
                Method getDefaultMethod,
                Method getIdMethod) {
            this.getTimeZoneMethod = getTimeZoneMethod;
            this.setDefaultMethod = setDefaultMethod;
            this.getDefaultMethod = getDefaultMethod;
            this.getIdMethod = getIdMethod;
        }

        static IcuTimeZoneHelper forClassName(String className) {
            try {
                Class<?> tzClass = Class.forName(className);
                Method getTimeZone = tzClass.getMethod("getTimeZone", String.class);
                Method setDefault = tzClass.getMethod("setDefault", tzClass);
                Method getDefault = tzClass.getMethod("getDefault");
                Method getId = tzClass.getMethod("getID");
                return new IcuTimeZoneHelper(getTimeZone, setDefault, getDefault, getId);
            } catch (ClassNotFoundException e) {
                return null;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unexpected ICU TimeZone API mismatch", e);
            }
        }

        void setDefaultById(String id) {
            try {
                Object tz = getTimeZoneMethod.invoke(null, id);
                setDefaultMethod.invoke(null, tz);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getTargetException());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        String getDefaultId() {
            try {
                Object tz = getDefaultMethod.invoke(null);
                return (String) getIdMethod.invoke(tz);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getTargetException());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
