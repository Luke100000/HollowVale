"""Builds the forgotten structures from vanilla ones.

Run: python3 scripts/forgotten_structures.py [minecraft-1.21.1.jar]

Copies the reachable templates and template pools into the hollowvale namespace, so the jigsaw blocks inside the
templates lead to the copies, and drops villagers, animals and entities. Spruce becomes dark oak and the spruce trees dead trees. Every chest gets LOOT_TABLE and every piece
the decay processors from DECAY.
"""
import json
import re
import sys
import zipfile
from dataclasses import dataclass, field
from pathlib import Path

import structure_loot as nbt

ROOT = Path(__file__).resolve().parent.parent
JAR = Path.home() / ".gradle/caches/fabric-loom/1.21.1/minecraft-merged.jar"
DATA = ROOT / "common/src/main/resources/data/hollowvale"
EMPTY = "minecraft:empty"
LOOT_TABLE = "hollowvale:chests/forgotten_village"
DROPPED_POOLS = re.compile(r"villagers|animals|cats|sheep|iron_golem")
DROPPED_ELEMENTS = re.compile(r"allays")
# Spruce and dark oak share their block states, so renaming a block keeps them.
SPRUCE = re.compile(
    r"minecraft:(potted_)?(stripped_)?spruce_(planks|log|wood|stairs|slab|fence|fence_gate|door|trapdoor|button"
    r"|pressure_plate|sign|wall_sign|leaves|sapling)"
)
# Vanilla's spruce trees, replaced by the biome's dead trees.
SPRUCE_TREE = re.compile(r'"minecraft:(spruce|pine)"')
DEAD_TREE = '"hollowvale:dead_tree_decor"'


@dataclass(frozen=True)
class Build:
    name: str
    source: str
    start_pool: str
    size: int
    max_distance: int
    biomes: list[str]
    placement: dict
    spawns: list[dict] = field(default_factory=list)


def spawn(entity: str, weight: int) -> dict:
    return {"type": f"hollowvale:{entity}", "weight": weight, "minCount": 1, "maxCount": 1}


BUILDS = [
    Build(
        name="forgotten_village", source="village_taiga", start_pool="minecraft:village/taiga/town_centers",
        size=3, max_distance=48, biomes=["hollowvale:hollow_vale"],
        placement={"spacing": 20, "separation": 7, "salt": 1108},
    ),
    Build(
        name="forgotten_outpost", source="pillager_outpost", start_pool="minecraft:pillager_outpost/base_plates",
        size=7, max_distance=80, biomes=["hollowvale:hollow_vale", "hollowvale:ossuary"],
        placement={"spacing": 22, "separation": 8, "salt": 1109,
                   "exclusion_zone": {"chunk_count": 8, "other_set": "hollowvale:forgotten_village"}},
        spawns=[spawn("bone_sludge", 4), spawn("skull_sludge", 1), spawn("fry", 4)],
    ),
]


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
    # Skips 15% of the blocks, which leaves the terrain in their place.
    {"processor_type": "minecraft:block_rot", "integrity": 0.85},
    {"processor_type": "minecraft:rule", "rules": [
        rule("dark_oak_log", AIR, 0.3),
        rule("dark_oak_log", COBWEB, 0.15),
        rule("cobblestone", block("mossy_cobblestone"), 0.5),
        rule("cobblestone", COBWEB, 0.1),
        rule("glass_pane", COBWEB, 0.5),
        rule("white_wool", COBWEB, 0.3),
        rule("torch", AIR),
        rule("wall_torch", AIR),
        rule("campfire", UNLIT_CAMPFIRE),
    ]},
]


def local(build: Build, name: str) -> str:
    path = name.removeprefix("minecraft:").removeprefix("village/")
    # Drops the vanilla biome from folders and file names, the copies are no longer tied to one.
    path = re.sub(r"(^|/)(taiga|plains)/", r"\1", path)
    path = re.sub(r"(^|/)taiga_", r"\1", path)
    return f"hollowvale:{build.name}/{path}"


def to_dark_oak(text: str) -> str:
    return SPRUCE.sub(lambda match: match.group().replace("spruce", "dark_oak"), text)


def is_kept(pool: str) -> bool:
    return pool != EMPTY and not DROPPED_POOLS.search(pool)


def mapped_pool(build: Build, pool: str) -> str:
    return local(build, pool) if is_kept(pool) else EMPTY


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2))


def read_json(jar: zipfile.ZipFile, name: str, kind: str) -> dict:
    namespace, path = name.split(":")
    return json.loads(jar.read(f"data/{namespace}/worldgen/{kind}/{path}.json"))


def copy_template(jar: zipfile.ZipFile, build: Build, location: str) -> list[str]:
    """Saves a template with its jigsaw pools redirected and returns the vanilla pools it links to."""
    root = nbt.parse(jar.read(f"data/minecraft/structure/{location.removeprefix('minecraft:')}.nbt"))
    root["entities"] = (nbt.LIST, (nbt.END, []))
    for state in root["palette"][1][1]:
        state["Name"] = (nbt.STRING, to_dark_oak(state["Name"][1]))
    linked = []
    for entry in root["blocks"][1][1]:
        fields = entry.get("nbt", (nbt.COMPOUND, {}))[1]
        if "LootTable" in fields:
            fields["LootTable"] = (nbt.STRING, LOOT_TABLE)
        if "final_state" in fields:
            fields["final_state"] = (nbt.STRING, to_dark_oak(fields["final_state"][1]))
        if "pool" in fields:
            linked.append(fields["pool"][1])
            fields["pool"] = (nbt.STRING, mapped_pool(build, linked[-1]))
    target = local(build, location).removeprefix("hollowvale:")
    path = DATA / "structure" / f"{target}.nbt"
    path.parent.mkdir(parents=True, exist_ok=True)
    nbt.save(path, root)
    return linked


def convert_element(jar: zipfile.ZipFile, build: Build, element: dict) -> list[str]:
    """Redirects an element, and any it contains, to the copies. Returns the vanilla templates it uses."""
    locations = []
    for inner in element.get("elements", []):
        locations += convert_element(jar, build, inner)
    if "location" in element:
        processors = element["processors"]
        if isinstance(processors, str):
            processors = read_json(jar, processors, "processor_list")
        vanilla = json.loads(to_dark_oak(json.dumps(processors["processors"])))
        element["processors"] = {"processors": vanilla + DECAY}
        locations.append(element["location"])
        element["location"] = local(build, element["location"])
    return locations


def convert_pool(jar: zipfile.ZipFile, build: Build, pool: str) -> tuple[dict, str, list[str]]:
    """Returns the pool redirected to the copies, its vanilla fallback pool and the vanilla templates it uses."""
    text = SPRUCE_TREE.sub(DEAD_TREE, to_dark_oak(json.dumps(read_json(jar, pool, "template_pool"))))
    data = json.loads(text)
    fallback = data["fallback"]
    data["fallback"] = mapped_pool(build, fallback)
    data["elements"] = [entry for entry in data["elements"] if not DROPPED_ELEMENTS.search(json.dumps(entry))]
    locations = []
    for entry in data["elements"]:
        locations += convert_element(jar, build, entry["element"])
    return data, fallback, locations


def write_structure(jar: zipfile.ZipFile, build: Build) -> None:
    structure = read_json(jar, f"minecraft:{build.source}", "structure")
    structure.update(biomes=f"#hollowvale:has_structure/{build.name}", start_pool=local(build, build.start_pool),
                     size=build.size, max_distance_from_center=build.max_distance)
    structure["spawn_overrides"] = {"monster": {"bounding_box": "full", "spawns": build.spawns}} if build.spawns else {}
    write_json(DATA / "worldgen/structure" / f"{build.name}.json", structure)
    write_json(DATA / "worldgen/structure_set" / f"{build.name}.json", {
        "structures": [{"structure": f"hollowvale:{build.name}", "weight": 1}],
        "placement": {"type": "minecraft:random_spread"} | build.placement,
    })
    write_json(DATA / "tags/worldgen/biome/has_structure" / f"{build.name}.json", {"values": build.biomes})


def build_structure(jar: zipfile.ZipFile, build: Build) -> None:
    pending, pools, templates = [build.start_pool], set(), set()
    while pending:
        pool = pending.pop()
        if pool in pools:
            continue
        pools.add(pool)
        data, fallback, locations = convert_pool(jar, build, pool)
        write_json(DATA / "worldgen/template_pool" / build.name / f"{local(build, pool).split('/', 1)[1]}.json", data)
        if is_kept(fallback):
            pending.append(fallback)
        for location in set(locations) - templates:
            templates.add(location)
            pending += [linked for linked in copy_template(jar, build, location) if is_kept(linked)]
    write_structure(jar, build)
    print(f"{build.name}: {len(pools)} pools, {len(templates)} templates")


def main() -> None:
    jar = zipfile.ZipFile(sys.argv[1] if len(sys.argv) > 1 else JAR)
    for build in BUILDS:
        build_structure(jar, build)


if __name__ == "__main__":
    main()
