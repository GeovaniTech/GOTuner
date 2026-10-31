#!/usr/bin/env python3
"""Check ELF PT_LOAD alignment in an APK/AAB; also check uncompressed APK ZIP alignment."""
import struct
import sys
import zipfile

path = sys.argv[1]
failures = []
count = 0
with zipfile.ZipFile(path) as archive, open(path, 'rb') as raw:
    for entry in archive.infolist():
        if not entry.filename.endswith('.so'):
            continue
        data = archive.read(entry)
        if data[:4] != b'\x7fELF':
            failures.append(entry.filename + ': invalid ELF')
            continue
        endian = '<' if data[5] == 1 else '>'
        is64 = data[4] == 2
        phoff = struct.unpack_from(endian + ('Q' if is64 else 'I'), data, 32 if is64 else 28)[0]
        entsize, number = struct.unpack_from(endian + 'HH', data, 54 if is64 else 42)
        alignments = []
        for i in range(number):
            offset = phoff + i * entsize
            if struct.unpack_from(endian + 'I', data, offset)[0] == 1:
                alignment = struct.unpack_from(endian + ('Q' if is64 else 'I'), data, offset + (48 if is64 else 28))[0]
                alignments.append(alignment)
                # Google Play's 16 KB requirement concerns 64-bit devices.
                if is64 and alignment < 16384:
                    failures.append(entry.filename + ': PT_LOAD alignment below 16 KB')
        if path.endswith('.apk') and entry.compress_type == zipfile.ZIP_STORED:
            raw.seek(entry.header_offset + 26)
            name_len, extra_len = struct.unpack('<HH', raw.read(4))
            if (entry.header_offset + 30 + name_len + extra_len) % 16384:
                failures.append(entry.filename + ': uncompressed ZIP data not 16 KB aligned')
        print(entry.filename, 'PT_LOAD alignments:', alignments)
        count += 1
print('Libraries checked:', count)
for failure in failures:
    print('FAIL:', failure)
raise SystemExit(bool(failures))
