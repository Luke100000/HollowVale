"""Convert a Blockbench Java block model to a block/item model json."""

import base64
import json
import sys
from pathlib import Path

VALID_ANGLES = (0, 22.5, 45)


def convert_element(element: dict, scale: tuple[float, float]) -> dict:
    result = {"from": element["from"], "to": element["to"]}

    rotation = element.get("rotation", [0, 0, 0])
    if any(rotation):
        axes = [axis for axis in range(3) if rotation[axis]]
        if len(axes) > 1:
            sys.exit(f"element {element['name']} rotates around more than one axis, block models allow one")
        if abs(rotation[axes[0]]) not in VALID_ANGLES:
            sys.exit(
                f"element {element['name']} is rotated {rotation[axes[0]]} degrees, block models only allow -45, -22.5, 0, 22.5 and 45")
        result["rotation"] = {"origin": element["origin"], "axis": "xyz"[axes[0]], "angle": rotation[axes[0]]}

    result["faces"] = {
        side: {
            "uv": [round(value * scale[index % 2], 4) for index, value in enumerate(face["uv"])],
            "texture": "#0",
            **({"rotation": face["rotation"]} if face.get("rotation") else {}),
        }
        for side, face in element["faces"].items()
        if face.get("texture") is not None
    }
    return result


def main() -> None:
    if len(sys.argv) != 5:
        sys.exit(__doc__)
    assets, source, model_path, texture_id = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3], sys.argv[4]

    model = json.loads(source.read_text())
    resolution = model.get("resolution", {"width": 16, "height": 16})
    scale = (16 / resolution["width"], 16 / resolution["height"])

    result = {
        "textures": {"0": texture_id, "particle": texture_id},
        "elements": [convert_element(element, scale) for element in model["elements"]],
    }
    if model.get("display"):
        result["display"] = model["display"]

    target = assets / "models" / f"{model_path}.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(result, indent=2))

    texture_target = assets / "textures" / f"{texture_id.split(':')[-1]}.png"
    texture_target.parent.mkdir(parents=True, exist_ok=True)
    texture_target.write_bytes(base64.b64decode(model["textures"][0]["source"].split(",", 1)[1]))
    print(f"converted {source.stem}")


if __name__ == "__main__":
    main()
