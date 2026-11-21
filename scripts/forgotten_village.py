"""Builds the forgotten village from the vanilla taiga village.

Run: python3 scripts/forgotten_village.py [minecraft-1.21.1.jar]

Copies the reachable templates and template pools into the hollowvale namespace, so the jigsaw blocks inside the
templates lead to the copies, and drops villagers and animals. Every chest gets LOOT_TABLE and every piece the decay
processors from DECAY.
"""
import json
import re
import sys
import zipfile
from pathlib import Path

import structure_loot as nbt

ROOT = Path(__file__).resolve().parent.parent
JAR = Path.home() / ".gradle/caches/fabric-loom/1.21.1/minecraft-merged.jar"
DATA = ROOT / "common/src/main/resources/data/hollowvale"
NAME = "forgotten_village"
START_POOL = "minecraft:village/taiga/town_centers"
EMPTY = "minecraft:empty"
LOOT_TABLE = "hollowvale:chests/forgotten_village"
DROPPED_POOLS = re.compile(r"villagers|animals|cats|sheep|iron_golem")


def block(name: str, **properties: str) -> dict:
    return {"Name": f"minecraft:{name}", **({"Properties": properties} if properties else {})}


def rule(name: str, output: dict, probability: float | None = None) -> dict:
    kind = "random_block_match" if probability else "block_match"
    predicate = {"predicate_type": f"minecraft:{kind}", "block": f"minecraft:{name}"}
    return {
        "input_predicate": predicate | ({"probability": probability} if probability else {}),
        "location_predicate": {"predicate_type": "minecraft:always_true"},
        "output_state": output,
    }


AIR, COBWEB = block("air"), block("cobweb")
UNLIT_CAMPFIRE = block("campfire", facing="north", lit="false", signal_fire="false", waterlogged="false")
DECAY = [
    # Skips 20% of the blocks, which leaves the terrain in their place.
    {"processor_type": "minecraft:block_rot", "integrity": 0.8},
    {"processor_type": "minecraft:rule", "rules": [
        rule("spruce_log", AIR, 0.4),
        rule("spruce_log", COBWEB, 0.15),
        rule("cobblestone", block("mossy_cobblestone"), 0.5),
        rule("cobblestone", COBWEB, 0.1),
        rule("glass_pane", COBWEB, 0.5),
        rule("torch", AIR),
        rule("wall_torch", AIR),
        rule("campfire", UNLIT_CAMPFIRE),
    ]},
]


def local(name: str) -> str:
    return f"hollowvale:{NAME}/{name.removeprefix('minecraft:village/')}"


def is_kept(pool: str) -> bool:
    return pool != EMPTY and not DROPPED_POOLS.search(pool)


def mapped_pool(pool: str) -> str:
    return local(pool) if is_kept(pool) else EMPTY


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2))


def read_json(jar: zipfile.ZipFile, name: str, kind: str) -> dict:
    namespace, path = name.split(":")
    return json.loads(jar.read(f"data/{namespace}/worldgen/{kind}/{path}.json"))


def copy_template(jar: zipfile.ZipFile, location: str) -> list[str]:
    """Saves a template with its jigsaw pools redirected and returns the vanilla pools it links to."""
    root = nbt.parse(jar.read(f"data/minecraft/structure/{location.removeprefix('minecraft:')}.nbt"))
    linked = []
    for entry in root["blocks"][1][1]:
        fields = entry.get("nbt", (nbt.COMPOUND, {}))[1]
        if "LootTable" in fields:
            fields["LootTable"] = (nbt.STRING, LOOT_TABLE)
        if "pool" in fields:
            linked.append(fields["pool"][1])
            fields["pool"] = (nbt.STRING, mapped_pool(linked[-1]))
    nbt.save(DATA / "structure" / NAME / f"{location.removeprefix('minecraft:village/')}.nbt", root)
    return linked


def convert_pool(jar: zipfile.ZipFile, pool: str) -> tuple[dict, list[str]]:
    """Returns the pool redirected to the copies and the vanilla templates it uses."""
    data = read_json(jar, pool, "template_pool")
    data["fallback"] = mapped_pool(data["fallback"])
    locations = []
    for entry in data["elements"]:
        element = entry["element"]
        if "location" not in element:
            continue
        processors = element["processors"]
        if isinstance(processors, str):
            processors = read_json(jar, processors, "processor_list")
        element["processors"] = {"processors": processors["processors"] + DECAY}
        locations.append(element["location"])
        element["location"] = local(element["location"])
    return data, locations


def write_structure(jar: zipfile.ZipFile) -> None:
    structure = read_json(jar, "minecraft:village_taiga", "structure")
    structure.update(biomes=f"#hollowvale:has_structure/{NAME}", start_pool=local(START_POOL), size=3, max_distance_from_center=48)
    write_json(DATA / "worldgen/structure" / f"{NAME}.json", structure)
    write_json(DATA / "worldgen/structure_set" / f"{NAME}.json", {
        "structures": [{"structure": f"hollowvale:{NAME}", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 20, "separation": 7, "salt": 1108},
    })
    write_json(DATA / "tags/worldgen/biome/has_structure" / f"{NAME}.json", {"values": ["hollowvale:hollow_vale"]})


def main() -> None:
    jar = zipfile.ZipFile(sys.argv[1] if len(sys.argv) > 1 else JAR)
    pending, pools, templates = [START_POOL], set(), set()

    while pending:
        pool = pending.pop()
        if pool in pools:
            continue
        pools.add(pool)
        data, locations = convert_pool(jar, pool)
        write_json(DATA / "worldgen/template_pool" / NAME / f"{pool.removeprefix('minecraft:village/')}.json", data)
        pending += [data["fallback"].replace(f"hollowvale:{NAME}/", "minecraft:village/")] if is_kept(data["fallback"]) else []
        for location in set(locations) - templates:
            templates.add(location)
            pending += [linked for linked in copy_template(jar, location) if is_kept(linked)]

    write_structure(jar)
    print(f"{len(pools)} pools, {len(templates)} templates")


if __name__ == "__main__":
    main()
