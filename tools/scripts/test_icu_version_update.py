#!/usr/bin/env python3 -B
#
# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Unit tests for tools/scripts/icu_version_update.py."""

import os
import sys
import tempfile
import unittest

_SCRIPTS_DIR = os.path.abspath(os.path.dirname(__file__))
if _SCRIPTS_DIR not in sys.path:
  sys.path.insert(0, _SCRIPTS_DIR)

import icu_version_update


class TestIcuVersionUpdate(unittest.TestCase):
  """Tests for version parsing, marker matching, and file scanning."""

  def test_parse_frontload(self):
    ctx = icu_version_update.parse_version_context('frontload', '80')
    self.assertEqual(ctx.major, 80)
    self.assertEqual(ctx.prev_major, 79)
    self.assertEqual(ctx.minor, 0)
    self.assertEqual(ctx.patch, 1)
    self.assertEqual(ctx.build, 0)
    self.assertEqual(ctx.ver_short, '80.0.1')
    self.assertEqual(ctx.ver_4part, '80.0.1.0')
    self.assertEqual(ctx.maven_ver, '80.0.1-SNAPSHOT')
    self.assertEqual(ctx.gh_rel_ver, '80.0.1')

  def test_parse_frontload_point_release(self):
    ctx = icu_version_update.parse_version_context('frontload', '78.3')
    self.assertEqual(ctx.major, 78)
    self.assertEqual(ctx.prev_major, 77)
    self.assertEqual(ctx.minor, 2)
    self.assertEqual(ctx.patch, 1)
    self.assertEqual(ctx.ver_short, '78.2.1')
    self.assertEqual(ctx.ver_4part, '78.2.1.0')
    self.assertEqual(ctx.maven_ver, '78.2.1-SNAPSHOT')
    self.assertEqual(ctx.gh_rel_ver, '78.2.1')

  def test_parse_frontload_explicit_3part(self):
    ctx = icu_version_update.parse_version_context('frontload', '80.0.1')
    self.assertEqual(ctx.ver_short, '80.0.1')
    self.assertEqual(ctx.maven_ver, '80.0.1-SNAPSHOT')

  def test_parse_rc(self):
    for ver_arg in ('79', '79.1'):
      ctx = icu_version_update.parse_version_context('rc', ver_arg)
      self.assertEqual(ctx.major, 79)
      self.assertEqual(ctx.prev_major, 78)
      self.assertEqual(ctx.minor, 1)
      self.assertEqual(ctx.patch, 0)
      self.assertEqual(ctx.ver_short, '79.1')
      self.assertEqual(ctx.ver_4part, '79.1.0.0')
      self.assertEqual(ctx.maven_ver, '79.1-SNAPSHOT')
      self.assertEqual(ctx.gh_rel_ver, '79.1rc')

  def test_parse_rc_point_release(self):
    ctx = icu_version_update.parse_version_context('rc', '78.3')
    self.assertEqual(ctx.major, 78)
    self.assertEqual(ctx.minor, 3)
    self.assertEqual(ctx.patch, 0)
    self.assertEqual(ctx.ver_short, '78.3')
    self.assertEqual(ctx.ver_4part, '78.3.0.0')
    self.assertEqual(ctx.maven_ver, '78.3-SNAPSHOT')
    self.assertEqual(ctx.gh_rel_ver, '78.3rc')

  def test_parse_ga(self):
    for ver_arg in ('79', '79.1'):
      ctx = icu_version_update.parse_version_context('ga', ver_arg)
      self.assertEqual(ctx.major, 79)
      self.assertEqual(ctx.minor, 1)
      self.assertEqual(ctx.patch, 0)
      self.assertEqual(ctx.ver_short, '79.1')
      self.assertEqual(ctx.ver_4part, '79.1.0.0')
      self.assertEqual(ctx.maven_ver, '79.1')
      self.assertEqual(ctx.gh_rel_ver, '79.1')

  def test_parse_ga_point_release(self):
    ctx = icu_version_update.parse_version_context('ga', '78.3')
    self.assertEqual(ctx.major, 78)
    self.assertEqual(ctx.minor, 3)
    self.assertEqual(ctx.patch, 0)
    self.assertEqual(ctx.ver_short, '78.3')
    self.assertEqual(ctx.ver_4part, '78.3.0.0')
    self.assertEqual(ctx.maven_ver, '78.3')
    self.assertEqual(ctx.gh_rel_ver, '78.3')

  def test_parse_invalid(self):
    with self.assertRaises(ValueError):
      icu_version_update.parse_version_context('invalid_phase', '79')
    with self.assertRaises(ValueError):
      icu_version_update.parse_version_context('ga', '79.1rc')
    with self.assertRaises(ValueError):
      icu_version_update.parse_version_context('ga', 'abc')

  def test_update_content_various_syntaxes(self):
    sample = (
        '// @icu-version-update: #define U_ICU_VERSION "{ver_short}"\n'
        '#define U_ICU_VERSION "79.0.1"\n'
        '<!-- @icu-version-update: <IcuMajorVersion>{major}</IcuMajorVersion>'
        ' -->\n'
        '  <IcuMajorVersion>79</IcuMajorVersion>\n'
        '    // @icu-version-update: DataVersion{"{ver_4part}"}\n'
        '    DataVersion{"79.0.1.0"}\n'
        '    // @icu-version-update: ICU_VERSION = getInstance({major},'
        ' {minor}, {patch}, {build});\n'
        '    ICU_VERSION = getInstance(79, 0, 1, 0);\n'
        "# @icu-version-update: export artifact_version='{maven_ver}'\n"
        "export artifact_version='79.0.1-SNAPSHOT'\n"
        "# @icu-version-update: export github_rel_version='{gh_rel_ver}'\n"
        "export github_rel_version='79.0.1'\n"
        '# @icu-version-update: export api_report_prev_version='
        "'{prev_major}'\n"
        "export api_report_prev_version='78'\n"
    )
    ctx = icu_version_update.parse_version_context('rc', '80')
    updated, markers, changed = icu_version_update.update_content_with_markers(
        sample, ctx
    )
    self.assertEqual(markers, 7)
    self.assertEqual(changed, 7)
    self.assertIn('#define U_ICU_VERSION "80.1"\n', updated)
    self.assertIn('  <IcuMajorVersion>80</IcuMajorVersion>\n', updated)
    self.assertIn('    DataVersion{"80.1.0.0"}\n', updated)
    self.assertIn('    ICU_VERSION = getInstance(80, 1, 0, 0);\n', updated)
    self.assertIn("export artifact_version='80.1-SNAPSHOT'\n", updated)
    self.assertIn("export github_rel_version='80.1rc'\n", updated)
    self.assertIn("export api_report_prev_version='79'\n", updated)

  def test_heredoc_lookahead_and_crlf(self):
    sample = (
        '# @icu-version-update: ICU configure {ver_short}\r\n'
        'if $ac_init_version; then\r\n'
        '  cat <<\\_ACEOF\r\n'
        'ICU configure 79.0.1\r\n'
        '_ACEOF\r\n'
    )
    ctx = icu_version_update.parse_version_context('ga', '79.1')
    updated, markers, changed = icu_version_update.update_content_with_markers(
        sample, ctx
    )
    self.assertEqual(markers, 1)
    self.assertEqual(changed, 1)
    self.assertIn('ICU configure 79.1\r\n', updated)

  def test_unmatched_marker_raises(self):
    bad_sample = (
        '// @icu-version-update: #define U_ICU_VERSION "{ver_short}"\n'
        'int something_else = 42;\n'
    )
    ctx = icu_version_update.parse_version_context('ga', '79.1')
    with self.assertRaises(ValueError):
      icu_version_update.update_content_with_markers(bad_sample, ctx)

  def test_unknown_placeholder_raises(self):
    bad_sample = (
        '// @icu-version-update: #define FOO "{no_such_var}"\n'
        '#define FOO "79"\n'
    )
    ctx = icu_version_update.parse_version_context('ga', '79.1')
    with self.assertRaises(ValueError):
      icu_version_update.update_content_with_markers(bad_sample, ctx)

  def test_scan_and_update_files_preserves_bom_and_skips_excluded(self):
    with tempfile.TemporaryDirectory(prefix='icu_ver_test_') as tmp_dir:
      # File with UTF-8 BOM and marker
      bom_file = os.path.join(tmp_dir, 'icuver.txt')
      with open(bom_file, 'wb') as f:
        f.write(
            b'\xef\xbb\xbf'
            b'// @icu-version-update: ICUVersion{"{ver_4part}"}\n'
            b'ICUVersion{"79.0.1.0"}\n'
        )

      # Excluded file (pom.xml) should not be touched even if it has marker text
      pom_file = os.path.join(tmp_dir, 'pom.xml')
      with open(pom_file, 'wb') as f:
        f.write(b'<!-- @icu-version-update: {major} -->\n')

      ctx = icu_version_update.parse_version_context('ga', '80.1')
      # Dry run should not modify the file
      files_found, files_mod = icu_version_update.scan_and_update_files(
          tmp_dir, ctx, dry_run=True
      )
      self.assertEqual(files_found, 1)
      self.assertEqual(files_mod, 1)
      with open(bom_file, 'rb') as f:
        self.assertIn(b'79.0.1.0', f.read())

      # Actual run should update and preserve UTF-8 BOM
      files_found, files_mod = icu_version_update.scan_and_update_files(
          tmp_dir, ctx, dry_run=False
      )
      self.assertEqual(files_found, 1)
      self.assertEqual(files_mod, 1)
      with open(bom_file, 'rb') as f:
        data = f.read()
      self.assertTrue(data.startswith(b'\xef\xbb\xbf'))
      self.assertIn(b'ICUVersion{"80.1.0.0"}\n', data)


if __name__ == '__main__':
  unittest.main()
