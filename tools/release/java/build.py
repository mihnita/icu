#!/usr/bin/env python3 -B
#
# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

import argparse
import os
import sys
import datetime
import subprocess

from libs import iculog
from libs import icuproc


basedir = '.'
rsrc_dir = os.path.join(basedir, 'src/main/resources')
apireport_jar_exists = False
apireport_jar = 'target/icu4c-apireport.jar'
newdir = ''
olddir = ''


def init():
  print(datetime.datetime.now())
  # icuproc.run_with_logging('mvn -version')
  subprocess.run('mvn -version', encoding='utf-8', shell=True, check=True)
  iculog.info(f'tools jar={apireport_jar}')
  iculog.info(f'basedir={basedir}')
  global apireport_jar_exists
  apireport_jar_exists = os.path.isfile(apireport_jar)


def tools():
  """compile release tools"""
  init()
  if apireport_jar_exists:
    return
  icuproc.run_with_logging('mvn package')

def clean():
  """remove all build targets"""
  init()
  icuproc.run_with_logging('mvn clean')


def apireport():
  tools()
  xslt_dir = '{rsrc_dir}/com/ibm/icu/dev/tools/docs'
  icuproc.run_with_logging('java ' \
            f' -jar {apireport_jar}' \
            f' --olddir {olddir}' \
            f' --newdir {newdir}' \
            f' --cppxslt {xslt_dir}/dumpAllCppFunc.xslt' \
            f' --cxslt {xslt_dir}/dumpAllCFunc.xslt' \
            f' --reportxslt {xslt_dir}/genReport.xslt' \
            f' --resultfile {basedir}/APIChangeReport.html')


def apireport_md():
  tools()
  xslt_dir = '{rsrc_dir}/com/ibm/icu/dev/tools/docs'
  icuproc.run_with_logging('java ' \
            f' -jar {apireport_jar}' \
            f' --olddir {olddir}' \
            f' --newdir {newdir}' \
            f" --cppxslt {xslt_dir}/dumpAllCppFunc.xslt" \
            f" --cxslt {xslt_dir}/dumpAllCFunc.xslt" \
            f" --reportxslt {xslt_dir}/genReport_md.xslt" \
            f" --resultfile {basedir}/APIChangeReport.md")


def apireport_xml():
  tools()
  xslt_dir = '{rsrc_dir}/com/ibm/icu/dev/tools/docs'
  icuproc.run_with_logging('java ' \
            f' -jar {apireport_jar}' \
            f' --olddir {olddir}' \
            f' --newdir {newdir}' \
            f'--cppxslt {xslt_dir}/dumpAllCppFunc_xml.xslt' \
            f'--cxslt {xslt_dir}/dumpAllCFunc_xml.xslt' \
            f'--reportxslt {xslt_dir}/genreport_xml.xslt' \
            f'--resultfile {basedir}/APIChangeReport.xml')


def main():
  parser = argparse.ArgumentParser()
  parser.add_argument('--clean', help='remove all build targets', action='store_true')
  parser.add_argument('--tools', help='compile release tools', action='store_true')
  parser.add_argument('--olddir', help='olddir TODO')
  parser.add_argument('--newdir', help='newdir TODO')
  parser.add_argument('--apireport', help='apireport TODO', action='store_true')
  parser.add_argument('--apireport_md', help='apireport_md TODO', action='store_true')
  parser.add_argument('--apireport_xml', help='apireport_xml TODO', action='store_true')
  cmd = parser.parse_args()

  if cmd.olddir:
    global olddir
    olddir = cmd.olddir
  if cmd.newdir:
    global newdir
    newdir = cmd.newdir

  if cmd.clean:
    clean()
  elif cmd.tools:
    tools()
  elif cmd.apireport:
    apireport()
  elif cmd.clean:
    clean()
  elif cmd.apireport_md:
    apireport_md()
  elif cmd.apireport_xml:
    apireport_xml()
  else:
    parser.print_help()
  print('DONE')
  return 0


if __name__ == '__main__':
  sys.exit(main())
