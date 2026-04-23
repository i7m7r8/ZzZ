#!/usr/bin/env python3
import struct, zlib, os

def png_chunk(tag, data):
    crc = zlib.crc32(tag + data) & 0xFFFFFFFF
    return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', crc)

def make_icon(size=192):
    rows = []
    cx = cy = size // 2
    r = size // 2 - 4
    for y in range(size):
        row = b'\x00'
        for x in range(size):
            d = ((x-cx)**2 + (y-cy)**2)**0.5
            if d < r - 8:
                row += b'\x00\xe5\xff'   # cyan
            elif d < r:
                row += b'\x00\xb8\xd4'  # ring
            else:
                row += b'\x0a\x0e\x1a'  # dark bg
        rows.append(row)
    raw = b''.join(rows)
    ihdr = struct.pack('>IIBBBBB', size, size, 8, 2, 0, 0, 0)
    return (b'\x89PNG\r\n\x1a\n'
            + png_chunk(b'IHDR', ihdr)
            + png_chunk(b'IDAT', zlib.compress(raw, 9))
            + png_chunk(b'IEND', b''))

sizes = {
    'mipmap-mdpi':    48,
    'mipmap-hdpi':    72,
    'mipmap-xhdpi':   96,
    'mipmap-xxhdpi':  144,
    'mipmap-xxxhdpi': 192,
}
base = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res')
for folder, sz in sizes.items():
    d = os.path.join(base, folder)
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, 'ic_launcher.png'), 'wb') as f:
        f.write(make_icon(sz))
    with open(os.path.join(d, 'ic_launcher_round.png'), 'wb') as f:
        f.write(make_icon(sz))
    print(f"  {folder}/ic_launcher.png ({sz}px)")
print("Icons generated.")
