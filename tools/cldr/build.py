#!/usr/bin/env python3 -B
#
# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""This build file is intended to become the single mechanism for working with
  CLDR code and data when building ICU data.

Eventually it will encompass:
* Building ICU data form CLDR data via cldr-to-icu.
* Building the CLDR libraries needed to support ICU data conversion.
* Copying CLDR test data for ICU regression tests.

It's not complete yet, so for now follow the instructions in:
  <icu_root>/docs/processes/cldr-icu.md
which is best viewed as
  https://unicode-org.github.io/icu/processes/cldr-icu.html
"""

import argparse
import os
import sys

from libs import icufs
from libs import iculog
from libs import icuproc

cldr_dir = str(os.getenv('CLDR_DIR'))
icu_dir = str(os.getenv('ICU_DIR'))
test_data_dir_4c = ''
test_data_dir_4j = ''


def _init_args():
  """Initialize any properties not already set on the command line."""
  # Inherit properties from environment variable unless specified. As usual
  # with Ant, this is messier than it should be. All we are saying here is:
  # "Use the property if explicitly set, otherwise use the environment variable."
  # We cannot just set the property to the environment variable, since expansion
  # fails for non existent properties, and you are left with a literal value of
  # "${env.CLDR_DIR}".
  global test_data_dir_4c
  global test_data_dir_4j
  if not icu_dir:
    iculog.failure('Set the ICU_DIR environment variable to the top level' \
    ' ICU source directory (containing `icu4c` and `icu4j`).')
  if not cldr_dir:
    iculog.failure('Set the CLDR_DIR environment variable to the top level' \
    ' CLDR source directory (containing `common`).')
  test_data_dir_4c = os.path.join(icu_dir, 'icu4c/source/test/testdata/cldr')
  test_data_dir_4j = os.path.join(icu_dir, 'icu4j/main/core/src/test/resources/com/ibm/icu/dev/data/cldr')


def _create_catalog(test_data_dir:str, contents: list[str]):
  catalog_file_name = os.path.join(test_data_dir, 'personNameTest/catalog.txt')
  icufs.copyfile(os.path.join(test_data_dir, 'personNameTest/_header.txt'),
                 catalog_file_name)
  with open(catalog_file_name, 'a', encoding='utf-8') as f:
    for line in contents:
      f.write(line)
      f.write('\n')


def copy_cldr_testdata(): # depends="init-args, clean-cldr-testdata
  """Copies CLDR test data directories, after deleting previous
    contents to prevent inconsistent state."""
  src_dir_base = os.path.join(cldr_dir, 'common/testData')
  # CLDR test data directories to be copied into ICU.
  # Add directories here to control which test data is installed. -->
  cldr_test_data = [
        'localeIdentifiers',
        'personNameTest', # Used in ExhaustivePersonNameTest
        'units/' # Used in UnitsTest tests
  ]
  for test_dir in cldr_test_data:
    src_dir = os.path.join(src_dir_base, test_dir)
    iculog.subtitle(f'Copying CLDR test data to {src_dir}')
    icufs.copycleandir(src_dir, os.path.join(test_data_dir_4c, test_dir))
    icufs.copycleandir(src_dir, os.path.join(test_data_dir_4j, test_dir))

  iculog.subtitle('Creating catalog.txt file')
  # collect the file names in the cldr/personNameTest directory
  contents = os.listdir(os.path.join(src_dir_base, 'personNameTest'))
  contents.sort()
  contents = list(filter(lambda x: not x.startswith('_'), contents))
  _create_catalog(test_data_dir_4c, contents)
  _create_catalog(test_data_dir_4j, contents)


def clean_cldr_testdata(): # depends="init-args"
  """Deletes CLDR test data"""
  iculog.title('Removing test dirs')
  icufs.rmdir(test_data_dir_4c)
  icufs.rmdir(test_data_dir_4j)


def reset_cldr_testdata(): # depends="init-args"
  """Restores CLDR test data"""
  iculog.title('Git-restore test dirs')
  icuproc.run_with_logging(f'git co -- {test_data_dir_4c}')
  icuproc.run_with_logging(f'git co -- {test_data_dir_4j}')


def main() -> int:
  parser = argparse.ArgumentParser()
  parser.add_argument('-cp', '--copy-cldr-testdata',
                      help='Copies CLDR test data directories, after deleting' \
                      ' previous contents to prevent inconsistent state.',
                      action='store_true')
  parser.add_argument('-rm', '--remove-cldr-testdata',
                      help='Deletes CLDR test data',
                      action='store_true')
  parser.add_argument('-reset', '--reset-cldr-testdata',
                      help='Restores the CLDR test data from git',
                      action='store_true')
  cmd = parser.parse_args()

  _init_args()
  if (cmd.copy_cldr_testdata):
    copy_cldr_testdata()
  elif (cmd.remove_cldr_testdata):
    clean_cldr_testdata()
  elif (cmd.reset_cldr_testdata):
    reset_cldr_testdata()
  else:
    parser.print_help()
  return 0


if __name__ == '__main__':
  sys.exit(main())
