#!/usr/bin/env python3
"""Make the prebuilt libnfsu2_android.so read the game files from the working directory.

The prebuilt library hardcodes its data folder as "/storage/emulated/0/nfsu2" (one literal, used as
root + "/SPEED2.EXE" etc.). We overwrite it with "." padded with slashes to the same length
(".////…" is the same path as "."), so the length baked into the machine code stays valid. The
launcher then chdir()s into the folder the player picked before the game starts (StorageFix).

Usage: patch_game_root.py <libnfsu2_android.so>
"""
import sys

OLD = b"/storage/emulated/0/nfsu2\x00"
NEW = b"." + b"/" * (len(OLD) - 2) + b"\x00"
assert len(OLD) == len(NEW)


def main(path):
    data = open(path, "rb").read()
    if data.count(OLD) == 0 and data.count(NEW) == 1:
        print("already patched:", path)
        return 0
    if data.count(OLD) != 1:
        print("expected exactly one game root literal, found %d" % data.count(OLD), file=sys.stderr)
        return 1
    open(path, "wb").write(data.replace(OLD, NEW))
    print("patched game root literal in", path)
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(2)
    sys.exit(main(sys.argv[1]))
