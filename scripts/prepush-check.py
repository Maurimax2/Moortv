#!/usr/bin/env python3
"""The checks this environment can run without an Android SDK.

There is no Kotlin compiler and no aapt here — GitHub Actions is the only thing
that compiles this project, so a mistake costs a full CI round trip. These are
the ones that have actually cost builds: a duplicate import, a symbol used
without importing it, malformed XML, unbalanced braces, and a string that
exists in one language but not the other.
"""
import collections
import glob
import re
import sys
import xml.etree.ElementTree as ET

NEEDS_IMPORT = {
    'awaitAll': 'kotlinx.coroutines.awaitAll',
    'coroutineScope': 'kotlinx.coroutines.coroutineScope',
    'flowOn': 'kotlinx.coroutines.flow.flowOn',
    'emitAll': 'kotlinx.coroutines.flow.emitAll',
    'delay': 'kotlinx.coroutines.delay',
    # Lists and grids each have their own itemsIndexed; either satisfies it.
    'itemsIndexed': ('androidx.compose.foundation.lazy.itemsIndexed',
                     'androidx.compose.foundation.lazy.grid.itemsIndexed'),
    'painterResource': 'androidx.compose.ui.res.painterResource',
    'stringResource': 'androidx.compose.ui.res.stringResource',
    'LaunchedEffect': 'androidx.compose.runtime.LaunchedEffect',
    'Crossfade': 'androidx.compose.animation.Crossfade',
    'tween': 'androidx.compose.animation.core.tween',
    'ContentScale': 'androidx.compose.ui.layout.ContentScale',
    'ColorFilter': 'androidx.compose.ui.graphics.ColorFilter',
    'SolidColor': 'androidx.compose.ui.graphics.SolidColor',
    'AsyncImage': 'coil.compose.AsyncImage',
    'ColumnScope': 'androidx.compose.foundation.layout.ColumnScope',
    'Spacer': 'androidx.compose.foundation.layout.Spacer',
    'RoundedCornerShape': 'androidx.compose.foundation.shape.RoundedCornerShape',
    'TextOverflow': 'androidx.compose.ui.text.style.TextOverflow',
    'TextAlign': 'androidx.compose.ui.text.style.TextAlign',
}

problems = 0

for path in glob.glob('**/src/**/*.kt', recursive=True):
    source = open(path).read()
    lines = source.split('\n')

    for name, count in collections.Counter(l for l in lines if l.startswith('import ')).items():
        if count > 1:
            print(f'DUPLICATE {name.strip()} -> {path}')
            problems += 1

    body = '\n'.join(l for l in lines if not l.startswith('import '))
    package = re.search(r'^package (\S+)', source, flags=re.M)
    package = package.group(1) if package else ''

    for symbol, needed in NEEDS_IMPORT.items():
        options = needed if isinstance(needed, tuple) else (needed,)
        if any(o.rsplit('.', 1)[0] == package for o in options):
            continue
        used = re.search(r'(?<![\w.])' + symbol + r'\s*[({.<]', body)
        if used and not any(f'import {o}' in source for o in options):
            print(f'MISSING import {options[0]} -> {path}')
            problems += 1

    # Braces inside a string are not structure. A literal holding JSON, or a
    # brace character on its own, used to be counted as a real one and reported
    # a balanced file as broken — so the literals go before the counting does.
    counted = re.sub(r'\"\"\".*?\"\"\"', '""', body, flags=re.S)
    counted = re.sub(r'\"(?:\\.|[^\"\\\n])*\"', '""', counted)
    counted = re.sub(r"'(?:\\.|[^'\\\n])'", "''", counted)

    depth = 0
    for line in counted.split('\n'):
        depth += line.split('//')[0].count('{') - line.split('//')[0].count('}')
    if depth:
        print(f'UNBALANCED braces ({depth}) -> {path}')
        problems += 1

for path in glob.glob('**/src/**/res/**/*.xml', recursive=True):
    try:
        ET.parse(path)
    except Exception as error:
        print(f'BROKEN XML {path}: {error}')
        problems += 1

# An apostrophe is a formatting character to aapt, not a letter. One unescaped
# in a French string failed resource compilation for the whole TV app, and the
# error names an internal class rather than the string — so it is worth its own
# check. A value wrapped in double quotes is allowed to contain them.
for path in glob.glob('**/src/**/res/**/strings.xml', recursive=True):
    for line in open(path, encoding='utf-8'):
        found = re.search(r'<string name="([^"]+)">(.*)</string>', line)
        if not found:
            continue
        name, text = found.groups()
        quoted = text.startswith('"') and text.endswith('"')
        if re.search(r"(?<!\\)'", text) and not quoted:
            print(f"UNESCAPED apostrophe in {name} -> {path}")
            problems += 1

for module in ('feature/home', 'feature/auth', 'core/data'):
    try:
        defined = set(re.findall(r'<string name="([^"]+)"',
                                 open(f'{module}/src/main/res/values/strings.xml').read()))
    except FileNotFoundError:
        continue
    try:
        french = set(re.findall(r'<string name="([^"]+)"',
                                open(f'{module}/src/main/res/values-fr/strings.xml').read()))
    except FileNotFoundError:
        french = set()

    used = set()
    for path in glob.glob(f'{module}/src/main/**/*.kt', recursive=True):
        used |= set(re.findall(r'R\.string\.(\w+)', open(path).read()))

    for name in sorted(used - defined):
        print(f'MISSING string {name} -> {module}')
        problems += 1
    for name in sorted(defined - french):
        print(f'NO FRENCH {name} -> {module}')
        problems += 1

print(f'{problems} problem(s)')
sys.exit(1 if problems else 0)
