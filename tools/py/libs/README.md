<!---
Copyright (C) 2026 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Reusable Python utilities for ICU

## Overview

We want to write the integration scripts in a very succinct way.

Pseudo-code:

```
dir = 'someFolder/foo/bar'
mkdir(dir)
pushd(dir)
execute('some_cmd -f1 --flag2 val2 arg1 arg2')
popd(dir)
rmdir('someFolder')
```

But at the same time we want everything to be very reliable by default.

- The whole process should fail if one step fails.
- We should produce little output, mostly status and progress.
- The status reporting should be visible (using colors).
- But the complete output should be logged for debugging.

So we create here several helper modules:

- **Logging (`iculog`):** which will log progress messages to standard output using colors,
  but will log detailed info to log files.
- **File system operations (`icufs`):** creating and removing folders, changing current folder, etc.
- **Process execution (`icuproc`):** to execute external processes, with logging, good error detection, etc.

These will be the building blocks that we will use to migrate the current shell scripts to Python.

## Testing

From the current folder (`<icuroot>/tools/py/libs`) run:
```sh
python -m unittest discover .
```

