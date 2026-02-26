#!/usr/bin/env python3 -B
#
# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

import argparse
import os
import sys
import urllib
import urllib.request

from libs import icufs
from libs import iculog
from libs import icuproc


basedir = '.'
out_dir = f'{basedir}/out'
src_dir = f'{basedir}/src'
classes_dir = f'{out_dir}/bin'
res_dir = f'{out_dir}/res'
xml_dir = f'{out_dir}/xml'

base_url = 'https://www.six-group.com/dam/download/financial-information/data-center/iso-currrency/lists'
current_xml = 'list-one.xml'
historic_xml = 'list-three.xml'


def build(): # depends="check, resource"
  """Verify ICU's local data and generate ISO 4217 alpha-numeric code mapping data resource"""
  iculog.subtitle('build()')
  check()
  resource()


def classes():
  """Build the Java tool"""
  iculog.subtitle('classes()')
  icufs.makecleandir(classes_dir)
  icuproc.run_with_logging('javac' \
      f' -d {classes_dir}' \
      ' --release 11 -encoding UTF-8' \
      f' {src_dir}/com/ibm/icu/dev/tool/currency/*.java')


def _check_local_xml() -> bool:
  iculog.info('_check_local_xml()')
  return os.path.exists(f'{basedir}/{current_xml}') \
      and os.path.exists(f'{basedir}/{historic_xml}')


def _local_xml() -> bool:
  iculog.info('_local_xml()')
  if _check_local_xml():
    iculog.subtitle('Using local ISO 4217 XML data files')
    icufs.copyfile(current_xml, xml_dir)
    icufs.copyfile(historic_xml, xml_dir)
    return True
  return False


def _download_xml():
  iculog.info('_download_xml()')
  if _check_local_xml():
    return
  iculog.info('Downloading ISO 4217 XML data files')
  icufs.mkdir(xml_dir)

  print('urllib.request.urlretrieve(' \
      f'"{base_url}/{current_xml}", ' \
      f'"{xml_dir}/{current_xml}")')

  opener = urllib.request.build_opener()
  opener.addheaders = [('Accept', 'application/xml')]
  urllib.request.install_opener(opener)
  urllib.request.urlretrieve(f'{base_url}/{current_xml}',
                             f'{xml_dir}/{current_xml}')
  urllib.request.urlretrieve(f'{base_url}/{historic_xml}',
                             f'{xml_dir}/{historic_xml}')


def xml_data(): # depends="_localXml, _downloadXml"
  """Prepare necessary ISO 4217 XML data files"""
  iculog.subtitle('xml_data()')
  if not _local_xml():
    _download_xml()


def check(): # depends="classes, xmlData"
  """Verify if ICU's local mapping data is synchronized with the XML data"""
  iculog.subtitle('check()')
  classes()
  xml_data()
  icuproc.run_with_logging(f'java -cp {classes_dir}'
      ' com.ibm.icu.dev.tool.currency.Main check'
      f' {xml_dir}/{current_xml}'
      f' {xml_dir}/{historic_xml}')


def resource(): # depends="classes"
  """Build ISO 4217 alpha-numeric code mapping data resource"""
  iculog.subtitle('resources()')
  classes()
  icufs.mkdir(res_dir)
  icuproc.run_with_logging('java'
            f' -cp={classes_dir}'
            ' com.ibm.icu.dev.tool.currency.Main'
            f' build {res_dir}')
  iculog.info(f'ISO 4217 numeric code mapping data was successfully created in {res_dir}')


def clean():
  """Delete build outputs"""
  iculog.subtitle('clean()')
  icufs.rmdir(out_dir)


def main():
  parser = argparse.ArgumentParser()
  parser.add_argument('--build',
                      help='Verify ICUs local data and generate ISO 4217 alpha-numeric code mapping data resource',
                      action='store_true')
  parser.add_argument('--check',
                      help='Verify if ICU\'s local mapping data is synchronized with the XML data',
                      action='store_true')
  parser.add_argument('--classes',
                      help='Build the Java tool',
                      action='store_true')
  parser.add_argument('--clean',
                      help='Delete build outputs',
                      action='store_true')
  parser.add_argument('--resource',
                      help='Build ISO 4217 alpha-numeric code mapping data resource',
                      action='store_true')
  parser.add_argument('--xmlData',
                      help='Prepare necessary ISO 4217 XML data files',
                      action='store_true')
  # Default target: build
  cmd = parser.parse_args()

  if cmd.build:
    build()
  elif cmd.check:
    check()
  elif cmd.classes:
    classes()
  elif cmd.clean:
    clean()
  elif cmd.resource:
    resource()
  elif cmd.xmlData:
    xml_data()
  else:
    parser.print_help()
  print('ALL GOOD!')
  return 0


if __name__ == '__main__':
  sys.exit(main())
