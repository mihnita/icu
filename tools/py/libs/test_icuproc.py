#!/usr/bin/env python3

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
    result: subprocess.CompletedProcess[str] = icuproc.run_with_logging('grep --help', ok_result=[2])
    self.assertEqual(result.returncode, 2)
    # WARNING: not portable
    self.assertRegex(result.stdout, 'usage')
    self.assertRegex(result.stdout, 'color')
    self.assertRegex(result.stdout, 'pattern')

  def test_run_cmd_bad_flag(self):
    # Test valid command with invalid flag
    with self.assertRaises(SystemExit) as cm:
      icuproc.run_with_logging('grep -badxyz .')

  def test_run_cmd_bad_arg(self):
    # Test valid command with invalid argument
    with self.assertRaises(SystemExit) as cm:
      icuproc.run_with_logging('grep xyz bad')

  def test_run_bad_cmd(self):
    with self.assertRaises(SystemExit) as cm:
      icuproc.run_with_logging('no_such_cmd')

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
