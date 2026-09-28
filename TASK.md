The current folder is a multi-module Mavan project (in `icu4j/`), and a C/C++ one (in `icu4c/`).
There are also other "pieces" of code writen in python, shell script, powershell, perl, etc.

You have maven, java, c++ compiler and everything needed in the path.

The job is to scan the git commits in the last 3 years for the ones doing version changes.
Also scan the documentation in the `docs/` folder for info on how to do version changes.

You will find that the process is quite complex and manual.

First, come up with a strategy to automate it, and present it to me.
And the version is present in many places, sometimes full, sometimes just elements of the version (major, minor, etc.)

If approved, you will write a python script to do the actual work.
That script will not touch any `pom.xml` files. Maven can do that. The script will call maven.

If you have any doubts, ask.
