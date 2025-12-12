#!/usr/bin/env python3
import base64
import os

# Minimal 48x48 blue PNG icon (base64 encoded)
# This is a simple solid blue square PNG
icon_data = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAB"
    "OQAAAY8BNb4QMwAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAEkSURBVGiB7dfB"
    "DcIwEAVRc0QFdEAHdAAdUAIdQClQCnRAB3RAB3RAB5QSd4nEYew4jhPxpJUsWfZ+YjseAGKMBzPbm9nB"
    "zN7M7GlmRzM7m9nFzK5m9jCzp5k9zOxqZhczu5rZw8xeZvY0s5eZPc3sZWZPM3ua2cvMXmb2NLOXmb3M"
    "7GVmLzN7mdnLzF5m9jKzl5m9zOxlZi8ze5nZy8xeZvYys5eZvczsbWZvM3ub2dvM3mb2NrO3mb3N7G1m"
    "bzN7m9nbzN5m9jazj5l9zOxjZh8z+5jZx8w+ZvYxs4+Zfczs880s5pwfOecH5/zgn"
    "B+cc35wzg/O+cE5Pzjn"
    "B+f84Jwf/PO3c35wzg/O+cE5Pzjnh38+5/zgnB+c84NzfnDOD875wTk/OOeH/z/nfAcIpDx3TZCauwAA"
    "AABJRU5ErkJggg=="
)

sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

# Since we can't resize without PIL, we'll use the same icon for all densities
# This is not ideal but will work for building the app
for density, size in sizes.items():
    path = f"app/src/main/res/mipmap-{density}/"
    os.makedirs(path, exist_ok=True)

    with open(f"{path}/ic_launcher.png", "wb") as f:
        f.write(icon_data)

    with open(f"{path}/ic_launcher_round.png", "wb") as f:
        f.write(icon_data)

    print(f"Created icons for {density}")

print("All launcher icons created successfully!")
