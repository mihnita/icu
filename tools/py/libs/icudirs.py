# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Get the paths for various directories, checking for some files / folders."""

import os
from libs import iculog


def _get_save_dir(
    key: str,
    defalt_value: str | None = None,
    dirs_to_check: list[str] | None = None,
    files_to_check: list[str] | None = None,
) -> str | None:
  result = os.environ.get(key)
  # Variable not set, or set to empty string.
  if not result:
    if not defalt_value:
      iculog.failure(f'Environment variable {key} is not set.')
    iculog.warning(
        f'Environment variable {key} is not set.\n'
        + f'  Using the default value: "{defalt_value}".'
    )
    result = defalt_value
  # Optios: either complain if it is not an absolute path,
  # or make it absolute and set the environment.
  # result = os.path.abspath(result)
  # os.environ[key] = result
  result = os.path.normpath(result)
  # Does not point to a directory.
  if not os.path.isdir(result):
    iculog.failure(
        f'Environment variable {key} is set to {result}.\n'
        + '  But it does not point to a directory.'
    )
  # Does not contain the expected subdirectories.
  if dirs_to_check:
    for chk_dir in dirs_to_check:
      if not os.path.isdir(os.path.join(result, chk_dir)):
        iculog.failure(
            f'Environment variable {key} is set to {result}.\n'
            + '  But it does not seem to point to a valid directory.\n'
            + f'  Missing subdirectory: {chk_dir}/'
        )
  # Does not contain the expected files.
  if files_to_check:
    for chk_file in files_to_check:
      if not os.path.isfile(os.path.join(result, chk_file)):
        iculog.failure(
            f'Environment variable {key} is set to {result}.\n'
            + '  But it does not seem to point to a valid directory.\n'
            + f'  Missing file: {chk_file}'
        )
  return result


def icu_dir() -> str:
  rel_dir = os.path.abspath(os.path.dirname(__file__)).rsplit(os.sep, 3)[0]
  return _get_save_dir(
      'ICU_DIR',
      rel_dir,
      ['icu4c', 'icu4j', 'tools'],
      ['pom.xml', 'README.md', 'CONTRIBUTING.md'],
  )


def cldr_dir() -> str:
  return _get_save_dir(
      'CLDR_DIR',
      None,
      ['common', 'keyboards', 'specs', 'tools'],
      ['pom.xml', 'README.md', 'CONTRIBUTING.md'],
  )


def cldr_prod_dir() -> str:
  return _get_save_dir(
      'CLDR_PROD_DIR',
      None,
      ['births', 'docs/charts', 'production/common/main'],
      ['pom.xml', 'README-common.md', 'README-keyboards.md'],
  )


def report_dirs():
  iculog.debug(f'__file__(): {__file__}')
  try:
    iculog.info(f'icu_dir(): {icu_dir()}')
  except SystemExit as e:
    pass
  try:
    iculog.info(f'cldr_dir(): {cldr_dir()}')
  except SystemExit as e:
    pass
  try:
    iculog.info(f'cldr_prod_dir(): {cldr_prod_dir()}')
  except SystemExit as e:
    pass


if __name__ == '__main__':
  report_dirs()
