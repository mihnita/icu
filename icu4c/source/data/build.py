#!/usr/bin/env python3 -B
#
# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

import argparse
import os
import sys
import datetime
import subprocess

from libs import icufs
from libs import iculog
from libs import icuproc

basedir = '.'
cldr_tmp_dir = None
cldr_prod_dir = None
cldrtools_jar = None
cldr_tmp_dir = None

def _init():
  iculog.subtitle('init()')
  iculog.info(str(datetime.datetime.now()))


  cldr_dir = os.environ.get('CLDR_DIR')
  if not cldr_dir:
    iculog.failure('Please set the CLDR_DIR environment variable to the top level CLDR source dir (containing \'common\').')

  cldrtools_dir = os.path.join(cldr_dir, 'tools')
  print(f'cldr_dir:{cldr_dir}')
  print(f'cldrtools_dir:{cldrtools_dir}')
  if not os.path.isdir(cldrtools_dir):
    iculog.failure('Please make sure that the CLDR tools directory is checked out into CLDR_DIR')

  dir_to_check = f'{cldrtools_dir}/cldr-code/target/classes'
  if not os.path.isdir(dir_to_check):
     iculog.failure(f'Can\'t find {dir_to_check}')

  global cldrtools_jar
  cldrtools_jar = f'{cldrtools_dir}/cldr-code/target/cldr-code.jar'
  if not os.path.isfile(cldrtools_jar):
    iculog.failure(f'CLDR classes not found in {cldrtools_dir}/cldr-code/target/classes. ' \
                   'Please build cldr-code.jar.')

  global cldr_tmp_dir
  cldr_tmp_dir = f'{cldr_dir}/../cldr-aux' # Hack: see CLDRPaths
  global cldr_prod_dir
  cldr_prod_dir = f'{cldr_tmp_dir}/production/'

  subprocess.run('mvn -version', encoding='utf-8', shell=True, check=True)
  iculog.info(f'cldr tools dir: {cldrtools_dir}')
  iculog.info(f'cldr tools jar: {cldrtools_jar}')
  # iculog.info(f'cldr tools classes: {env.CLDR_CLASSES}')
  iculog.info(f'CLDR_TMP_DIR: {cldr_tmp_dir} ')
  iculog.info(f'cldr.prod_dir (production data): {cldr_prod_dir}')


def _setup():
  iculog.subtitle('setup()')
  _init()
  global cldr_tmp_dir
  if cldr_tmp_dir:
    icufs.mkdir(cldr_tmp_dir) # make sure parent dir exists


def cleanprod(): # if="cldrprod.exists"
  iculog.title('cleanprod()')
  _setup()
  icufs.rmdir(f'{cldr_prod_dir}/common')
  icufs.rmdir(f'{cldr_prod_dir}/keyboards')


def proddata(): # unless="cldrprod.exists">
  iculog.title('proddata()')
  _setup()
  iculog.info(f'Rebuilding {cldr_prod_dir} - takes a while!')
  notes_dir = os.environ.get('NOTES')
  if not notes_dir:
    notes_dir = '.'
  # setup prod data
  icuproc.run_with_logging(
    'java' \
    f' -cp {cldrtools_jar}' \
    # change to short alias 'proddata' or similar when annotated
    ' org.unicode.cldr.tool.GenerateProductionData' \
    ' -v',
    logfile=os.path.join(notes_dir, 'cldr-newData-proddataLog.txt')
  )
  # TODO: for now, we just let the default source/target paths used.
  # Could set '-s' / '-d' for explicit source/dest


def main():
  parser = argparse.ArgumentParser()
  parser.add_argument('-c', '--cleanprod', help='remove all build targets', action='store_true')
  parser.add_argument('-p', '--proddata', help='olddir TODO', action='store_true')
  cmd = parser.parse_args()

  if cmd.cleanprod:
    cleanprod()
  elif cmd.proddata:
    proddata()
  else:
    parser.print_help()

  return 0


if __name__ == '__main__':
  sys.exit(main())
