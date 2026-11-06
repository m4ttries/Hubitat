"""Builds sump_probe_separator.stl with manifold3d (mirrors the .scad file).
pip install manifold3d trimesh numpy ; python3 build_stl.py [probes]"""
import sys
import numpy as np, trimesh
from manifold3d import Manifold as M

probes = int(sys.argv[1]) if len(sys.argv) > 1 else 3
pitch, leg_d, leg_len, bore_d, tip_d = 25, 10, 45, 2.5, 5
shed_d, shed_wall, shed_z = 17, 2.2, [12, 28]
block_h, block_back, block_front = 20, 15, 6
tie_w, tie_t, pipe_d, pipe_seat = 5.2, 2.2, 48.3, 1.5
fn, eps = 64, 0.01
leg_r = leg_d / 2
shed_h = shed_d / 2 - leg_r
span = (probes - 1) * pitch
blk_w = span + shed_d
xs = [i * pitch - span / 2 for i in range(probes)]

def cyl(h, r1, r2=None, n=fn):
    return M.cylinder(h, r1, r1 if r2 is None else r2, n)

def leg():
    ch = leg_r - tip_d / 2
    m = cyl(ch, tip_d / 2, leg_r) + cyl(leg_len - ch + 2 * eps, leg_r).translate([0, 0, ch - eps])
    for z in shed_z:
        shed = cyl(shed_h, shed_d / 2, leg_r) - cyl(shed_h, shed_d / 2, leg_r).translate([0, 0, -shed_wall])
        m += shed.translate([0, 0, z])
    return m

blk = M.cube([blk_w, block_back + block_front, block_h]).translate([-blk_w / 2, -block_back, leg_len])
for zc in (leg_len + block_h * 0.25, leg_len + block_h * 0.75):
    blk -= M.cube([blk_w + 2, tie_t, tie_w]).translate([-blk_w / 2 - 1, -block_back + 3, zc - tie_w / 2])
if pipe_seat > 0:
    blk -= cyl(block_h + 2, pipe_d / 2, n=180).translate([0, -block_back - pipe_d / 2 + pipe_seat, leg_len - 1])

part = blk
for x in xs:
    part += leg().translate([x, 0, 0])
for x in xs:
    part -= cyl(leg_len + block_h + 2, bore_d / 2, n=24).translate([x, 0, -1])
    part -= cyl(2.5, bore_d / 2, bore_d / 2 + 2.5, n=32).translate([x, 0, leg_len + block_h - 2.5 + eps])

# flip for printing: top block on the bed, legs up
part = part.rotate([180, 0, 0]).translate([0, 0, leg_len + block_h])
mesh = part.to_mesh()
tm = trimesh.Trimesh(np.asarray(mesh.vert_properties)[:, :3], np.asarray(mesh.tri_verts))
out = f"sump_probe_separator_{probes}probe.stl"
tm.export(out)
print(out, "watertight:", tm.is_watertight, "bounds:", tm.bounds.round(2).tolist(), "vol cm3:", round(tm.volume / 1000, 2))
