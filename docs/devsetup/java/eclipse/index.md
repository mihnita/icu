---
layout: default
title: Eclipse Setup for Java Developers
grand_parent: Setup for Contributors
parent: Java Setup
---

<!--
© 2026 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Eclipse Setup for Java Developers Using Maven
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


ICU4J uses Maven as a build system after version 74.

## Install Eclipse

You will (of course) have to install Eclipse if you don't have it already
(from <https://www.eclipse.org/downloads/>)

Choose one of the packages targeting Java developers,
for example _"Eclipse IDE for Java Developers"_ or
_"Eclipse IDE for Enterprise Java and Web Developers"_.

## Configure Eclipse

### Force encoding to UTF-8 (Windows only)

If you are working on Windows we would highly recommend forcing
the default encoding to UTF-8. That will prevent encoding problems that might
sneak in when you edit Java sources or test files.

Although it is possible to override the default encoding in UI, the easiest way
to do it is by editing the `eclipse.ini` in the Eclipse installation folder.

Just add `-Dfile.encoding=UTF-8` at the end.

## Use a separate workspace

**If you want to use Eclipse, you should create a new clean workspace first.**

## Install recommended plugins


## Import ICU4J from the file system

(Recommended)

In <icu workspace root>/icu4j remember to run "ant init" first. You might run
"ant check" as well for good measure.

If you check out ICU4J source from the repository using an external client
(usually command-line git clone), the new instruction is not much different. You
just follow the steps below -

1.  File/Import...
2.  Select General/Existing Projects into Workspace
3.  Select root directory: Browse to <icu svn workspace root>/icu4j, which will
    show a number of projects.
4.  Deselect the following projects (i.e., do not import them). These are not
    needed for normal ICU development (and would require installing further
    prerequisite libraries to get them to build).
    *   com.ibm.\* (Eclipse plug-in)
    *   icu4j-localespi\* (more plug-in)
    *   icu4j-build-tools
    *   icu4j-packaging-tests
5.  Click Finish.
6.  Wait for Eclipse to build the projects.

## Testing & Debugging

### Run All Tests

To run all of the main tests, do the following:

**58 or later**

*   "ant check" from the command line?

### Run specific tests

#### 58 or later

*   Right click on a test package (for example `com.ibm.icu.dev.test.rbbi` in
    the **icu4j-core-tests** project), or an entire test source directory (such
    as src in the **icu4j-core-tests** project) and choose **Run As->JUnit
    Test**
*   For test coverage, install EclEmma (below) and use **Coverage As** instead
    of **Run As**.

### Test Coverage (53 or later)

*   Install EclEmma plug-in. The installation instruction is found in [the
    EclEmma site page](http://www.eclemma.org/installation.html).
*   Run all tests once as described in the above section once.
*   For the menu, select "Run" > "Coverage..." to open "Coverage Configurations"
    window.
*   Go to "Coverage" tab and uncheck all test projects (icu4j-\*-tests,
    icu4j-test-framework, icu4j-testall) to exclude test codes from coverage
    analysis.
*   Click "Coverage" to run the all tests with coverage analysis enabled. After
    the text execution, coverage report displayed in "Coverage" view.
*   After running coverage, source lines are highlighted in different colors
    depending on coverage level. Too remove the highlights, click "Remove All
    Sessions" icon below (which also delete the coverage results).

![image](Capture.png)

*   If you want to run coverage again, you can just right click on icu4j-testall
    project and select "Coverage As" > "Java Application"
