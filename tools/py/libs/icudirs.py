# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Get the paths for various directories, checking for some files / folders."""

import os

def _get_save_dir(key:str, defalt_value:str | None = None,
                  dirs_to_check:list[str] | None = None,
                  files_to_check:list[str] | None = None) -> str:
  result = os.environ.get(key, defalt_value)
  # Variable not set, or set to empty string.
  if not result:
    print(f'Environment variable {key} is not set.')
    exit(1)
  # Optios: eithe complain if it is not an absolute path,
  # or make it absolute and set the environment.
  # result = os.path.abspath(result)
  # os.environ[key] = result
  result = os.path.normpath(result)
  # Does not point to a directory.
  if not os.path.isdir(result):
    print(f'Environment variable {key} is set to {result}.\n'
          + 'But it does not point to a directory.')
    exit(1)
  # Does not contain the expected subdirectories.
  if dirs_to_check:
    for chk_dir in dirs_to_check:
      if not os.path.isdir(os.path.join(result, chk_dir)):
        print(f'Environment variable {key} is set to {result}.\n'
              + 'But it does not seem to point to a valid directory.\n'
              + f'Missing subdirectory: {chk_dir}')
        exit(1)
  # Does not contain the expected files.
  if files_to_check:
    for chk_file in files_to_check:
      if not os.path.isfile(os.path.join(result, chk_file)):
        print(f'Environment variable {key} is set to {result}.\n'
              + 'But it does not seem to point to a valid directory.\n'
              + f'Missing file: {chk_file}')
        exit(1)
  return result


def icu_dir() -> str:
  rel_dir = os.path.abspath(os.path.dirname(__file__)).rsplit(os.sep, 3)[0]
  return _get_save_dir(
    'ICU_DIR', rel_dir,
    ['icu4c', 'icu4j', 'tools'],
    ['pom.xml', 'README.md', 'CONTRIBUTING.md'])


def cldr_dir() -> str:
  return _get_save_dir(
    'CLDR_DIR', None,
    ['common', 'keyboards', 'specs', 'tools'],
    ['pom.xml', 'README.md', 'CONTRIBUTING.md'])


def cldr_prod_dir() -> str:
  return _get_save_dir(
    'CLDR_PROD_DIR', None,
    ['births', 'docs/charts', 'production/common/main'],
    ['pom.xml', 'README-common.md', 'README-keyboards.md'])


def unicode_dir() -> str:
  return _get_save_dir(
    'UNICODE_DIR', None,
    ['16.0.0', 'emoji', 'idna', 'math', 'PROGRAMS', 'UCA', 'UCD'],
    [])


if __name__ == '__main__':
  print(f'__file__: {__file__}')
  print('ICU_DIR:', icu_dir())
  print('CLDR_DIR:', cldr_dir())
  print('CLDR_PROD_DIR:', cldr_prod_dir())
  print('UNICODE_DIR:', unicode_dir())
