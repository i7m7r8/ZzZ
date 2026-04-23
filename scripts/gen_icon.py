#!/usr/bin/env python3
"""Generate minimal PNG icon for CI when ImageMagick is unavailable."""
import struct, zlib, os

def make_png(size=192, bg=(10,14,26), fg=(0,229,255)):
    def chunk(name, data):
        c = zlib.crc32(name + data) & 0xffffffff
        return struct.pack('>I', len(data)) + name + data + struct.pack('>I', c)

    pixels = []
    cx, cy = size // 2, size // 2
    r = size // 3
    for y in range(size):
        row = b'\x00'
        for x in range(size):
            dist = ((x-cx)**2 + (y-cy)**2) ** 0.5
            if dist < r:
                row += bytes(fg)
            else:
                row += bytes(bg)
        pixels.append(row)

    raw = b''.join(pixels)
    compressed = zlib.compress(raw, 9)

    ihdr_data = struct.pack('>IIBBBBB', size, size, 8, 2, 0, 0, 0)
    png = (
        b'\x89PNG\r\n\x1a\n' +
        chunk(b'IHDR', ihdr_data) +
        chunk(b'IDAT', compressed) +
        chunk(b'IEND', b'')
    )
    return png

os.makedirs('scripts', exist_ok=True)
with open('scripts/placeholder_icon.png', 'wb') as f:
    f.write(make_png())
print("Generated scripts/placeholder_icon.png")
