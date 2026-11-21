"""Syncs exported structures into the mod and assigns loot tables to chests and suspicious gravel.

Run from anywhere: python3 scripts/structure_loot.py [--no-sync]
Edit RULES when a structure with containers is added.
"""
import argparse
import gzip
import re
import shutil
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "neoforge/run/saves/Structures/generated/hollowvale/structures"
TARGET = ROOT / "common/src/main/resources/data/hollowvale/structure"

CHESTS = {"minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel"}
BRUSHABLE = {"minecraft:suspicious_gravel", "minecraft:suspicious_sand"}

# stem pattern -> loot table per container kind
RULES = {
    r"ruins_\d+": {"chest": "hollowvale:chests/ruins"},
    r"catacombs": {"chest": "hollowvale:chests/catacombs"},
    r"dead_whale_\d+": {"brushable": "hollowvale:archaeology/whale_remains"},
}

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTES, STRING, LIST, COMPOUND, INTS, LONGS = range(13)
SCALARS = {BYTE: ">b", SHORT: ">h", INT: ">i", LONG: ">q", FLOAT: ">f", DOUBLE: ">d"}


class Reader:
    def __init__(self, data):
        self.data = data
        self.pos = 0

    def take(self, fmt):
        value = struct.unpack_from(fmt, self.data, self.pos)[0]
        self.pos += struct.calcsize(fmt)
        return value

    def string(self):
        size = self.take(">H")
        text = self.data[self.pos:self.pos + size].decode("utf-8")
        self.pos += size
        return text

    def payload(self, tag):
        if tag in SCALARS:
            return self.take(SCALARS[tag])
        if tag == BYTES:
            size = self.take(">i")
            self.pos += size
            return self.data[self.pos - size:self.pos]
        if tag == STRING:
            return self.string()
        if tag == LIST:
            inner, size = self.take(">b"), self.take(">i")
            return inner, [self.payload(inner) for _ in range(size)]
        if tag == COMPOUND:
            fields = {}
            while (inner := self.take(">b")) != END:
                name = self.string()
                fields[name] = (inner, self.payload(inner))
            return fields
        if tag in (INTS, LONGS):
            fmt = ">i" if tag == INTS else ">q"
            return [self.take(fmt) for _ in range(self.take(">i"))]
        raise ValueError(f"unknown tag {tag}")


def write_string(out, text):
    raw = text.encode("utf-8")
    out += struct.pack(">H", len(raw)) + raw


def write_payload(out, tag, value):
    if tag in SCALARS:
        out += struct.pack(SCALARS[tag], value)
    elif tag == BYTES:
        out += struct.pack(">i", len(value)) + value
    elif tag == STRING:
        write_string(out, value)
    elif tag == LIST:
        inner, items = value
        out += struct.pack(">bi", inner, len(items))
        for item in items:
            write_payload(out, inner, item)
    elif tag == COMPOUND:
        for name, (inner, item) in value.items():
            out += struct.pack(">b", inner)
            write_string(out, name)
            write_payload(out, inner, item)
        out += b"\x00"
    elif tag in (INTS, LONGS):
        out += struct.pack(">i", len(value))
        for item in value:
            out += struct.pack(">i" if tag == INTS else ">q", item)
    else:
        raise ValueError(f"unknown tag {tag}")


def parse(data):
    reader = Reader(gzip.decompress(data))
    assert reader.take(">b") == COMPOUND
    reader.string()
    return reader.payload(COMPOUND)


def load(path):
    return parse(path.read_bytes())


def save(path, root):
    out = bytearray(struct.pack(">b", COMPOUND))
    write_string(out, "")
    write_payload(out, COMPOUND, root)
    path.write_bytes(gzip.compress(bytes(out), mtime=0))


def rule_for(stem):
    return next((rule for pattern, rule in RULES.items() if re.fullmatch(pattern, stem)), {})


def assign(path):
    """Assigns loot tables in one structure file and returns how many containers were handled."""
    root = load(path)
    palette = [entry["Name"][1] for entry in root["palette"][1][1]]
    rule = rule_for(path.stem)
    counts = {"chest": 0, "brushable": 0, "missing": 0}
    changed = False

    for block in root["blocks"][1][1]:
        name = palette[block["state"][1]]
        if name in CHESTS:
            kind, table = "chest", rule.get("chest")
        elif name in BRUSHABLE:
            kind, table = "brushable", rule.get("brushable")
        else:
            continue
        if table is None:
            counts["missing"] += 1
            continue

        nbt = block.get("nbt", (COMPOUND, {}))[1]
        if nbt.get("LootTable") != (STRING, table):
            nbt["LootTable"] = (STRING, table)
            nbt.pop("Items", None)
            block["nbt"] = (COMPOUND, nbt)
            changed = True
        counts[kind] += 1

    if changed:
        save(path, root)
    return counts


def sync():
    if not SOURCE.is_dir():
        return
    TARGET.mkdir(parents=True, exist_ok=True)
    for source in sorted(SOURCE.glob("*.nbt")):
        target = TARGET / source.name
        if not target.exists() or source.stat().st_mtime > target.stat().st_mtime:
            shutil.copy2(source, target)
            print(f"synced {source.name}")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--no-sync", action="store_true", help="skip copying from the dev run directory")
    args = parser.parse_args()

    if not args.no_sync:
        sync()

    failed = False
    for path in sorted(TARGET.glob("*.nbt")):
        counts = assign(path)
        if counts["missing"]:
            print(f"{path.name}: {counts['missing']} container(s) without a loot rule, add one to RULES")
            failed = True
        elif counts["chest"] or counts["brushable"]:
            print(f"{path.name}: {counts['chest']} chest(s), {counts['brushable']} brushable block(s)")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
