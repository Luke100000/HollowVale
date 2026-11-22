"""Convert Blockbench models to GeckoLib geometry and animation files.

Usage: python3 scripts/bb2geo.py <assets_dir> <model.bbmodel>...

Each <name>.bbmodel becomes geo/entity/<name>.geo.json and
animations/entity/<name>.animation.json under <assets_dir>. A <name>.png
next to the model is copied to textures/entity/.

The conversion matches Blockbench's own Bedrock export: X is mirrored and
X/Y rotations of bones and animations are negated, and so is X of animation positions. Cubes outside any group go into a "root" bone.
Meshes, locators and bezier keyframes are not supported.
"""

import json
import shutil
import sys
from pathlib import Path

ROOT_BONE = "root"


def mirror_x(point: list[float]) -> list[float]:
    return [-point[0], point[1], point[2]]


def to_bedrock_rotation(rotation: list[float]) -> list[float]:
    return [-rotation[0], -rotation[1], rotation[2]]


def parse_value(value: str) -> float | str:
    """Keyframe values are strings in Blockbench; anything non-numeric is molang."""
    try:
        number = float(value)
    except ValueError:
        return value
    return int(number) if number.is_integer() else number


def negate(value: float | str) -> float | str:
    if isinstance(value, str):
        return f"-({value})"
    return -value if value else 0


def convert_cube(element: dict) -> dict:
    start, end = element["from"], element["to"]
    cube = {
        "origin": [-end[0], start[1], start[2]],
        "size": [end[i] - start[i] for i in range(3)],
    }
    if element.get("inflate"):
        cube["inflate"] = element["inflate"]

    rotation = element.get("rotation", [0, 0, 0])
    if any(rotation):
        cube["rotation"] = to_bedrock_rotation(rotation)
        cube["pivot"] = mirror_x(element.get("origin", [0, 0, 0]))

    uv = {}
    for side, face in element["faces"].items():
        if face.get("texture") is None:
            continue
        u1, v1, u2, v2 = face["uv"]
        # Bedrock flips up/down faces in both directions.
        if side in ("up", "down"):
            uv[side] = {"uv": [u2, v2], "uv_size": [u1 - u2, v1 - v2]}
        else:
            uv[side] = {"uv": [u1, v1], "uv_size": [u2 - u1, v2 - v1]}
    cube["uv"] = uv
    return cube


def build_bones(model: dict) -> list[dict]:
    elements = {
        element["uuid"]: element
        for element in model["elements"]
        if element.get("type", "cube") == "cube" and element.get("export", True)
    }
    groups = {group["uuid"]: group for group in model.get("groups", [])}
    bones = []

    def add_bone(node: dict, parent: str | None) -> None:
        group = groups[node["uuid"]]
        bone = {
            "name": group["name"],
            "pivot": mirror_x(group.get("origin", [0, 0, 0])),
        }
        if parent:
            bone["parent"] = parent
        rotation = group.get("rotation", [0, 0, 0])
        if any(rotation):
            bone["rotation"] = to_bedrock_rotation(rotation)
        bones.append(bone)

        cubes = []
        for child in node.get("children", []):
            if isinstance(child, dict):
                add_bone(child, group["name"])
            elif child in elements:
                cubes.append(convert_cube(elements[child]))
        if cubes:
            bone["cubes"] = cubes

    outliner = model["outliner"]
    top_groups = [node for node in outliner if isinstance(node, dict)]
    loose_cubes = [
        convert_cube(elements[node])
        for node in outliner
        if isinstance(node, str) and node in elements
    ]

    if len(top_groups) == 1 and not loose_cubes:
        add_bone(top_groups[0], None)
        return bones

    root = {"name": ROOT_BONE, "pivot": [0, 0, 0]}
    if loose_cubes:
        root["cubes"] = loose_cubes
    bones.append(root)
    for node in top_groups:
        add_bone(node, ROOT_BONE)
    return bones


def build_geometry(model: dict) -> dict:
    resolution = model.get("resolution", {"width": 16, "height": 16})
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": f"geometry.{model['name']}",
                    "texture_width": resolution["width"],
                    "texture_height": resolution["height"],
                    "visible_bounds_width": 8,
                    "visible_bounds_height": 4,
                    "visible_bounds_offset": [0, 1.5, 0],
                },
                "bones": build_bones(model),
            }
        ],
    }


def build_animations(model: dict) -> dict:
    loop_modes = {"loop": True, "hold": "hold_on_last_frame"}
    animations = {}
    for animation in model.get("animations", []):
        bones = {}
        for animator in animation.get("animators", {}).values():
            if animator.get("type", "bone") != "bone":
                continue
            channels = bones.setdefault(animator["name"], {})
            keyframes = sorted(animator.get("keyframes", []), key=lambda k: k["time"])
            for keyframe in keyframes:
                point = keyframe["data_points"][0]
                vector = [parse_value(point[axis]) for axis in "xyz"]
                if keyframe["channel"] == "rotation":
                    # GeckoLib negates X and Y of rotation keyframes on load, like it does for bone rotations.
                    vector[0], vector[1] = negate(vector[0]), negate(vector[1])
                elif keyframe["channel"] == "position":
                    vector[0] = negate(vector[0])
                if keyframe.get("interpolation") == "catmullrom":
                    value = {"post": vector, "lerp_mode": "catmullrom"}
                else:
                    value = vector
                timestamp = str(round(keyframe["time"], 4))
                channels.setdefault(keyframe["channel"], {})[timestamp] = value

        animations[animation["name"]] = {
            "animation_length": animation["length"],
            "loop": loop_modes.get(animation.get("loop"), False),
            "bones": bones,
        }
    return {"format_version": "1.8.0", "animations": animations}


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent="\t"))


def main() -> None:
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    assets = Path(sys.argv[1])

    for model_path in map(Path, sys.argv[2:]):
        model = json.loads(model_path.read_text())
        name = model_path.stem
        write_json(assets / "geo/entity" / f"{name}.geo.json", build_geometry(model))
        write_json(
            assets / "animations/entity" / f"{name}.animation.json",
            build_animations(model),
        )

        texture = model_path.with_suffix(".png")
        if texture.exists():
            target = assets / "textures/entity" / texture.name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(texture, target)
        print(f"converted {name}")


if __name__ == "__main__":
    main()
