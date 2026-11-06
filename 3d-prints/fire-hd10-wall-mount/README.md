# Fire HD 10 (13th gen, TG425K) Landscape Wall Mount

![preview](preview.png)

A 3D-printable wall mount that holds the tablet **horizontally**: camera at the top middle, power/volume buttons and USB-C on the **right**.

## Design
- **Right edge left open.** Nothing sits beside the buttons or USB-C port. The back plate stops 2 mm short of the tablet's right edge, so a **90° USB-C plug** can point up or down along the edge. The only part on the right is a 14 mm stop at the bottom corner. Set `right_stop = false` if it hits your plug.
- **6 mm stand-off from the wall** (`back_t`) gives room for the part of the plug that is thicker than the tablet.
- **Camera is clear.** The top hooks sit only at the two corners.
- **Tilt-in mounting, no clips to snap.** Push the top edge of the tablet up under the top hooks (there is 9 mm of headroom), swing the bottom in, and let it drop behind the 6 mm bottom lips. Gravity holds it in place.
- The left side has corner stops only, so the speakers and edge stay uncovered.
- An open-window back saves filament. It has 6 countersunk screw holes (4 mm / #8 wood screws). The bottom lips have notches so a screwdriver can reach the lower holes.

## Files
| File | Use |
|---|---|
| `fire_hd10_wall_mount_full.stl` | One piece, about 256 × 189 × 17 mm. Needs a 256 mm bed (e.g. Bambu X1/P1). |
| `fire_hd10_wall_mount_left.stl` + `_right.stl` | Two halves, each about 140 mm wide, for 220 mm beds. They overlap at a lap joint in the centre. The two centre wall screws clamp the halves together. |
| `fire_hd10_wall_mount.scad` | Parametric OpenSCAD source. |

## Printing
PETG or PLA, 0.2 mm layers, 4 walls, 25 % infill. Print with the **flat back on the bed**. No supports are needed; the lips are short (≤ 6 mm) bridges.

## Adjusting
Tablet size is set to Amazon's listed 247.1 × 165.9 × 8.6 mm plus 0.8 mm clearance. **Measure your tablet first.** If you use a skin or slim case, increase `tab_w`, `tab_h` and `clear_t`. Re-export the files with:

```bash
openscad -o fire_hd10_wall_mount_full.stl -D 'part="full"' fire_hd10_wall_mount.scad
```

To see the ghost tablet and plug in the preview, set `show_tablet = true`.
