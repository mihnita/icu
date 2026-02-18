#!/usr/bin/env python3

# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Interface for all steps, don't use directly."""

import subprocess
import tempfile
import unittest

from libs import icufs
from libs import iculog
from libs import icuproc


class TestIcuProc(unittest.TestCase):
  """Test class for the file system utilities."""

  def test_run_cmd(self):
    result: subprocess.CompletedProcess[str] = icuproc.run_with_logging('ls /')
    self.assertEqual(result.returncode, 0)
    # WARNING: not portable
    self.assertRegex(result.stdout, 'etc')
    self.assertRegex(result.stdout, 'usr')
    self.assertRegex(result.stdout, 'var')

  def test_run_cmd_bad_flag(self):
    # Test valid command with invalid flag
    with self.assertRaises(Exception) as cm:
      icuproc.run_with_logging('ls -xyz /')
    proc_error: subprocess.CalledProcessError = cm.exception  # type: ignore
    self.assertRegex(proc_error.stdout, 'invalid option')
    self.assertEqual(proc_error.returncode, 2)

  def test_run_cmd_bad_arg(self):
    # Test valid command with invalid argument
    with self.assertRaises(Exception) as cm:
      icuproc.run_with_logging('ls /xyz_bad')
    proc_error: subprocess.CalledProcessError = cm.exception  # type: ignore
    self.assertRegex(proc_error.stdout, 'No such file or directory')
    self.assertEqual(proc_error.returncode, 2)

  def test_run_bad_cmd(self):
    with self.assertRaises(Exception) as cm:
      icuproc.run_with_logging('no_such_cmd')
      proc_error: subprocess.CalledProcessError = cm.exception  # type: ignore
      self.assertEqual(proc_error.returncode, 127)

  @classmethod
  def setUpClass(cls):
    super().setUpClass()
    # Would be good to add argument `delete=True`, from Python 3.12
    cls.root_dir: tempfile.TemporaryDirectory[str] = (
        tempfile.TemporaryDirectory(prefix='icuproc_test_')
    )
    icufs.mkdir(cls.root_dir.name)

  @classmethod
  def tearDownClass(cls):
    super().tearDownClass()
    cls.root_dir.cleanup()


if __name__ == '__main__':
  iculog.init_logging()
  unittest.main()
