#!/usr/bin/env python3 -B
#
# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Automates ICU version updates across C/C++, Java, data, and Maven files.

Scans repository files for `@icu-version-update: <line_template>` comment
markers and updates the corresponding lines, then invokes Maven to update
all `pom.xml` files.
"""

import argparse
import dataclasses
import os
import re
import sys

# Ensure `<icu_root>/tools/py` is on sys.path so `from libs import ...` works
# even if PYTHONPATH was not explicitly exported by the caller.
_TOOLS_PY_DIR = os.path.abspath(
    os.path.join(os.path.dirname(__file__), '..', 'py')
)
if _TOOLS_PY_DIR not in sys.path:
  sys.path.insert(0, _TOOLS_PY_DIR)

try:
  from libs import icudirs
  from libs import icufs
  from libs import iculog
  from libs import icuproc
except (ModuleNotFoundError, ImportError):
  print('Make sure you define PYTHONPATH pointing to the ICU modules:')
  print('  export PYTHONPATH=<icu_root>/tools/py')
  print('On Windows:')
  print('  set PYTHONPATH=<icu_root>\\tools\\py')
  sys.exit(1)

MARKER_TAG = '@icu-version-update:'
MARKER_BYTES = MARKER_TAG.encode('utf-8')

# Matches a comment line containing `@icu-version-update: <template>`.
# Supports //, #, /* ... */, <!-- ... -->, ;, and REM comments.
MARKER_LINE_RE = re.compile(
    r'^\s*(?://+|#+|/\*+|<!--+|;+|REM\b)\s*'
    + re.escape(MARKER_TAG)
    + r'\s*(.+?)(?:\s*(?:\*+/|-->+))?\s*$'
)

# Regex patterns for each supported template placeholder variable.
VAR_PATTERNS: dict[str, str] = {
    'major': r'\d+',
    'prev_major': r'\d+',
    'minor': r'\d+',
    'patch': r'\d+',
    'build': r'\d+',
    'ver_short': r'\d+(?:\.\d+){1,2}',
    'ver_4part': r'\d+(?:\.\d+){3}',
    'maven_ver': r'\d+(?:\.\d+){1,2}(?:-SNAPSHOT)?',
    'gh_rel_ver': r'\d+(?:\.\d+){1,2}(?:rc\d*)?',
}

# Maximum number of lines after a marker to look ahead for its target line
# (allows placing markers above `cat <<_ACEOF` heredocs in `configure`).
MAX_LOOKAHEAD = 8

EXCLUDED_DIRS: set[str] = {
    '.git',
    '.idea',
    '.settings',
    '.vscode',
    '__pycache__',
    'build',
    'docs',
    'node_modules',
    'out',
    'target',
}

EXCLUDED_FILES: set[str] = {
    'pom.xml',
    'icu_version_update.py',
    'test_icu_version_update.py',
    'summary.md',
    'TASK.md',
}

EXCLUDED_EXTENSIONS: set[str] = {
    '.a',
    '.bin',
    '.brk',
    '.bz2',
    '.cfu',
    '.class',
    '.cnv',
    '.dat',
    '.dll',
    '.dylib',
    '.exe',
    '.gif',
    '.gz',
    '.ico',
    '.icu',
    '.jar',
    '.jpeg',
    '.jpg',
    '.lib',
    '.md',
    '.nrm',
    '.o',
    '.obj',
    '.pdf',
    '.png',
    '.res',
    '.ser',
    '.so',
    '.spp',
    '.svg',
    '.tar',
    '.tgz',
    '.tri2',
    '.ucm',
    '.xz',
    '.zip',
}


@dataclasses.dataclass(frozen=True)
class VersionContext:
  """Holds parsed version numbers and derived version strings for a phase."""

  phase: str
  major: int
  minor: int
  patch: int
  build: int = 0

  @property
  def prev_major(self) -> int:
    return self.major - 1

  @property
  def ver_short(self) -> str:
    if self.patch > 0:
      return f'{self.major}.{self.minor}.{self.patch}'
    return f'{self.major}.{self.minor}'

  @property
  def ver_4part(self) -> str:
    return f'{self.major}.{self.minor}.{self.patch}.{self.build}'

  @property
  def maven_ver(self) -> str:
    if self.phase in ('frontload', 'rc'):
      return f'{self.ver_short}-SNAPSHOT'
    return self.ver_short

  @property
  def gh_rel_ver(self) -> str:
    if self.phase == 'rc':
      return f'{self.ver_short}rc'
    return self.ver_short

  def template_vars(self) -> dict[str, str]:
    """Returns the dictionary of placeholder values for marker templates."""
    return {
        'major': str(self.major),
        'prev_major': str(self.prev_major),
        'minor': str(self.minor),
        'patch': str(self.patch),
        'build': str(self.build),
        'ver_short': self.ver_short,
        'ver_4part': self.ver_4part,
        'maven_ver': self.maven_ver,
        'gh_rel_ver': self.gh_rel_ver,
    }


def parse_version_context(phase: str, version_str: str) -> VersionContext:
  """Parses a version string and release phase into a VersionContext.

  Args:
    phase: One of 'frontload', 'rc', or 'ga'.
    version_str: Version string such as '79', '79.1', '78.3', or '79.0.1'.

  Returns:
    The computed VersionContext.

  Raises:
    ValueError: If phase or version_str is invalid.
  """
  if phase not in ('frontload', 'rc', 'ga'):
    raise ValueError(f'Invalid phase: {phase}')

  match = re.match(r'^(\d+)(?:\.(\d+)(?:\.(\d+))?)?$', version_str.strip())
  if not match:
    raise ValueError(
        f'Invalid version "{version_str}". Expected <major>[.<minor>[.<patch>]]'
        ' (e.g. 79, 79.1, 78.3, or 79.0.1).'
    )

  major = int(match.group(1))
  minor_str = match.group(2)
  patch_str = match.group(3)

  if phase == 'frontload':
    if minor_str is None:
      minor = 0
      patch = 1
    elif patch_str is None:
      target_minor = int(minor_str)
      minor = max(0, target_minor - 1)
      patch = 1
    else:
      minor = int(minor_str)
      patch = int(patch_str)
  else:
    if minor_str is None:
      minor = 1
      patch = 0
    elif patch_str is None:
      minor = int(minor_str)
      patch = 0
    else:
      minor = int(minor_str)
      patch = int(patch_str)

  return VersionContext(phase=phase, major=major, minor=minor, patch=patch)


TOKEN_RE = re.compile(r'\{([a-z0-9_]+)\}')


def build_template_regex(template: str) -> re.Pattern[str]:
  """Builds a regex that matches a target line conforming to `template`."""
  pos = 0
  pattern_parts: list[str] = [r'^(.*?)']
  found_var = False

  for match in TOKEN_RE.finditer(template):
    literal = template[pos : match.start()]
    if literal:
      pattern_parts.append(re.escape(literal))
    var_name = match.group(1)
    if var_name not in VAR_PATTERNS:
      valid = ', '.join(sorted(VAR_PATTERNS.keys()))
      raise ValueError(
          f'Unknown placeholder "{{{var_name}}}" in marker template:'
          f' "{template}". Valid placeholders: {valid}'
      )
    pattern_parts.append(f'(?:{VAR_PATTERNS[var_name]})')
    found_var = True
    pos = match.end()

  if not found_var:
    raise ValueError(
        f'Marker template "{template}" does not contain any {{...}}'
        ' placeholders.'
    )

  trailing = template[pos:]
  if trailing:
    pattern_parts.append(re.escape(trailing))
  pattern_parts.append(r'(\r?\n?)$')
  return re.compile(''.join(pattern_parts))


def render_template(template: str, vars_dict: dict[str, str]) -> str:
  """Substitutes `{var_name}` placeholders in `template` using `vars_dict`."""
  return TOKEN_RE.sub(lambda m: vars_dict[m.group(1)], template)


def update_content_with_markers(
    content: str,
    ctx: VersionContext,
    file_label: str = '<memory>',
) -> tuple[str, int, int]:
  """Updates all marker-targeted lines in `content`.

  Args:
    content: The text content of a file.
    ctx: The target VersionContext.
    file_label: File path/name used for error reporting.

  Returns:
    A tuple of (updated_content, markers_found, lines_changed).

  Raises:
    ValueError: If a marker template is invalid or no matching line is found
      within MAX_LOOKAHEAD lines after the marker.
  """
  lines = content.splitlines(keepends=True)
  vars_dict = ctx.template_vars()
  markers_found = 0
  lines_changed = 0
  claimed_indices: set[int] = set()

  for idx, line in enumerate(lines):
    line_stripped = line.rstrip('\r\n')
    marker_match = MARKER_LINE_RE.match(line_stripped)
    if not marker_match:
      continue

    markers_found += 1
    template = marker_match.group(1).strip()
    target_re = build_template_regex(template)
    rendered_body = render_template(template, vars_dict)

    matched_target_idx: int | None = None
    max_idx = min(len(lines), idx + 1 + MAX_LOOKAHEAD)
    for candidate_idx in range(idx + 1, max_idx):
      if candidate_idx in claimed_indices:
        continue
      candidate_line = lines[candidate_idx]
      if MARKER_LINE_RE.match(candidate_line.rstrip('\r\n')):
        continue
      target_match = target_re.match(candidate_line)
      if target_match:
        matched_target_idx = candidate_idx
        line_prefix = target_match.group(1)
        line_ending = target_match.group(2)
        new_line = f'{line_prefix}{rendered_body}{line_ending}'
        if new_line != candidate_line:
          lines[candidate_idx] = new_line
          lines_changed += 1
        claimed_indices.add(candidate_idx)
        break

    if matched_target_idx is None:
      raise ValueError(
          f'{file_label}:{idx + 1}: No line within {MAX_LOOKAHEAD} lines'
          f' matched marker template: {template}'
      )

  return ''.join(lines), markers_found, lines_changed


def should_scan_file(file_name: str) -> bool:
  """Returns True if `file_name` should be scanned for version markers."""
  if file_name in EXCLUDED_FILES:
    return False
  _, ext = os.path.splitext(file_name)
  if ext.lower() in EXCLUDED_EXTENSIONS:
    return False
  return True


def scan_and_update_files(
    root_dir: str,
    ctx: VersionContext,
    dry_run: bool = False,
) -> tuple[int, int]:
  """Scans `root_dir` for files with version markers and updates them.

  Args:
    root_dir: Repository root directory.
    ctx: Target VersionContext.
    dry_run: If True, do not write changes to disk.

  Returns:
    Tuple of (files_with_markers, files_modified).
  """
  files_with_markers = 0
  files_modified = 0

  for current_root, dirs, files in os.walk(root_dir):
    dirs[:] = sorted(d for d in dirs if d not in EXCLUDED_DIRS)
    for file_name in sorted(files):
      if not should_scan_file(file_name):
        continue
      abs_path = os.path.join(current_root, file_name)
      if os.path.islink(abs_path):
        continue
      try:
        with open(abs_path, 'rb') as f:
          raw_bytes = f.read()
      except OSError as ex:
        iculog.warning(f'Could not read {abs_path}: {ex}')
        continue

      if MARKER_BYTES not in raw_bytes:
        continue

      rel_path = os.path.relpath(abs_path, root_dir)
      has_bom = raw_bytes.startswith(b'\xef\xbb\xbf')
      text = raw_bytes.decode('utf-8-sig')

      updated_text, markers_count, changed_count = update_content_with_markers(
          text, ctx, file_label=rel_path
      )
      if markers_count == 0:
        continue

      files_with_markers += 1
      if changed_count > 0:
        files_modified += 1
        action = '[dry-run] Would update' if dry_run else 'Updated'
        iculog.info(
            f'{action} {rel_path} ({changed_count}/{markers_count} marker(s)'
            ' changed)'
        )
        if not dry_run:
          out_bytes = updated_text.encode('utf-8')
          if has_bom:
            out_bytes = b'\xef\xbb\xbf' + out_bytes
          with open(abs_path, 'wb') as f:
            f.write(out_bytes)
      else:
        iculog.info(
            f'Unchanged {rel_path} ({markers_count} marker(s) already up to'
            ' date)'
        )

  return files_with_markers, files_modified


def update_maven_poms(
    root_dir: str,
    ctx: VersionContext,
    dry_run: bool = False,
) -> None:
  """Updates all Maven pom.xml files by invoking `mvn` commands."""
  standalone_poms = [
      'tools/cldr/cldr-to-icu/pom.xml',
      'tools/release/java/pom.xml',
  ]
  commands: list[tuple[str, str]] = []
  for pom_path in standalone_poms:
    log_slug = pom_path.replace('/', '_')
    cmd = (
        'mvn versions:update-parent'
        f' -DparentVersion={ctx.maven_ver}'
        ' -DskipResolution=true'
        ' -DgenerateBackupPoms=false'
        f' -f {pom_path}'
    )
    commands.append((cmd, f'mvn_update_parent_{log_slug}.log'))

  commands.append((
      f'mvn versions:set -DnewVersion={ctx.maven_ver}'
      ' -DgenerateBackupPoms=false',
      'mvn_versions_set.log',
  ))
  commands.append((
      'mvn versions:set-property -Dproperty=icu.major.version'
      f' -DnewVersion={ctx.major} -DgenerateBackupPoms=false',
      'mvn_versions_set_major.log',
  ))

  old_dir = icufs.pushd(root_dir)
  try:
    for cmd, logfile in commands:
      if dry_run:
        iculog.info(f'[dry-run] Would execute: {cmd}')
      else:
        icuproc.run_with_logging(cmd, logfile=logfile, root_dir=root_dir)
  finally:
    icufs.popd(old_dir)


def main(argv: list[str] | None = None) -> int:
  iculog.init_logging()
  parser = argparse.ArgumentParser(
      description=(
          'Update ICU version numbers across marker-annotated files and Maven'
          ' pom.xml files.'
      )
  )
  phase_group = parser.add_mutually_exclusive_group(required=True)
  phase_group.add_argument(
      '--frontload',
      metavar='VERSION',
      help=(
          'Front-load pre-release version update (e.g. "80" -> 80.0.1 /'
          ' 80.0.1-SNAPSHOT, or "78.3" -> 78.2.1 / 78.2.1-SNAPSHOT).'
      ),
  )
  phase_group.add_argument(
      '--rc',
      metavar='VERSION',
      help=(
          'Release Candidate version update (e.g. "79" or "79.1" -> 79.1 /'
          ' 79.1-SNAPSHOT / 79.1rc, or "78.3" -> 78.3 / 78.3-SNAPSHOT /'
          ' 78.3rc).'
      ),
  )
  phase_group.add_argument(
      '--ga',
      metavar='VERSION',
      help=(
          'General Availability version update (e.g. "79" or "79.1" -> 79.1,'
          ' or "78.3" -> 78.3).'
      ),
  )
  parser.add_argument(
      '--dry-run',
      action='store_true',
      help='Preview changes and Maven commands without modifying any files.',
  )
  parser.add_argument(
      '--skip-maven',
      action='store_true',
      help='Only update marker-annotated files; skip running Maven commands.',
  )

  args = parser.parse_args(argv)
  if args.frontload:
    phase, version_str = 'frontload', args.frontload
  elif args.rc:
    phase, version_str = 'rc', args.rc
  else:
    phase, version_str = 'ga', args.ga

  try:
    ctx = parse_version_context(phase, version_str)
  except ValueError as ex:
    iculog.failure(str(ex))
    return 1

  root_dir = icudirs.icu_dir()
  iculog.title(
      f'ICU Version Update: phase={ctx.phase}, version={ctx.ver_short},'
      f' maven={ctx.maven_ver}, gh_rel={ctx.gh_rel_ver}'
  )

  iculog.subtitle('Scanning and updating marker-annotated files')
  try:
    files_with_markers, files_modified = scan_and_update_files(
        root_dir, ctx, dry_run=args.dry_run
    )
  except ValueError as ex:
    iculog.failure(str(ex))
    return 1

  iculog.info(
      f'Marker scan complete: {files_with_markers} file(s) with markers,'
      f' {files_modified} file(s) {"would be " if args.dry_run else ""}modified.'
  )

  if not args.skip_maven:
    iculog.subtitle('Updating Maven pom.xml files')
    update_maven_poms(root_dir, ctx, dry_run=args.dry_run)

  iculog.title('ICU Version Update Completed Successfully')
  return 0


if __name__ == '__main__':
  sys.exit(main())
