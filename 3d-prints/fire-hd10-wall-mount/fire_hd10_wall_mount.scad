// Amazon Fire HD 10 (13th gen, model TG425K) - landscape picture-frame wall mount
// -----------------------------------------------------------------------------
// Two parts:
//   TRAY  - screws to the wall; the tablet drops into it face-up.
//   BEZEL - a front frame that SNAPS onto the tray (no screws) and covers
//           everything except the screen (with a notch for the front camera).
//           Flexible tabs on its underside sit flush in recesses in the tray's
//           outer walls and their barbs click into a catch groove. To remove,
//           slip a small flat screwdriver into the gap below a tab's tip and
//           lever the tab outward.
// Both sides of the tablet have an equal hidden pocket, so the tablet sits
// centred in the frame. The right pocket holds a 90-degree USB-C plug; the
// cable leaves through a slot in the bottom edge or a hole in the back
// (for running it into the wall).
//
// Orientation (looking at the wall): camera top-middle, buttons + USB-C right.
// Coordinates: X = across the wall, Y = up, Z = out of the wall.
//
// The frame is ~303 x 196 mm, so every part also comes as left/right halves
// that fit a 220 mm bed. Tray halves overlap in a lap joint clamped by the
// centre wall screws. Bezel halves butt together at the centre; each half
// has its own snap tabs on both sides of the seam.

part = "assembly";  // assembly | tray | tray_left | tray_right | bezel | bezel_left | bezel_right
show_tablet = true; // assembly preview only

/* [Tablet] */
// Measured by the owner (letters refer to docs/tablet_dimensions.png)
tab_w = 246;        // A  landscape width
tab_h = 165;        // B  landscape height (measured tight, so clearance is generous)
tab_t = 8.5;        // C  thickness
screen_border = 14.7; // E/F/G/H  black border, edge of tablet -> lit screen (all four sides)
cam_x  = 123;       // J  camera centre from the tablet's left edge
cam_y  = 7;         // K  camera centre from the tablet's top edge
port_y = 45.5;      // L  USB-C centre from the tablet's bottom edge (approx.)
                    // M  USB-C centre is mid-thickness (C/2)
                    // N = 19.3, P = 40.2: buttons start 19.3 mm from the top edge
                    //     and span 40.2 mm (covered by the bezel)
clear   = 1.0;      // clearance added to width and height
clear_t = 0.6;      // clearance added to thickness (add more for a skin)

/* [Frame] */
pocket_w = 22;      // hidden pocket each side (room for the 90-degree plug)
outer    = 6;       // outer wall thickness (houses the snap recesses)
border   = 14;      // top / bottom border width
back_t   = 4;       // back plate under the tablet
pocket_floor = 2;   // back plate under the pockets (plug can sit lower)
/* [Edge style] */
// sharp   : square edges (original)
// chamfer : 45-degree chamfer on the front edge, small back chamfer
// round   : rounded front edge (bezel 1 mm thicker)
// frame   : rounded outer edge + 45-degree bevel down to the screen
// soft    : deep roundover, larger bevel to the screen, big corners (bezel 2 mm thicker)
// frame_sq        : "frame" with square outer corners
// frame_sq_window : "frame" with square outer corners AND a square screen opening
edge_style = "sharp";
// [bezel_t, corner_r, front_type, front_size, inner_bevel, back_chamfer, window_r]
style = edge_style == "chamfer"   ? [3, 8,   "chamfer", 2, 0,   1, 3]
      : edge_style == "round"     ? [4, 10,  "round",   3, 0,   1, 3]
      : edge_style == "frame"     ? [4, 10,  "round",   3, 2.5, 1, 3]
      : edge_style == "frame_sq"  ? [4, 0.01,"round",   3, 2.5, 1, 3]
      : edge_style == "frame_sq_window" ? [4, 0.01, "round", 3, 2.5, 1, 0.01]
      : edge_style == "soft"      ? [5, 14,  "round",   4, 3.5, 2, 3]
      :                             [3, 4,   "none",    0, 0,   0, 3];
bezel_t   = style[0];   // front frame thickness
corner_r  = style[1];   // plan-view corner radius of the frame
front_type = style[2];
front_sz  = style[3];   // size of the front-edge chamfer / roundover
inner_bev = style[4];   // 45-degree bevel from the bezel face down to the screen
back_ch   = style[5];   // chamfer on the wall-side edge (makes it look thinner)
window_r  = style[6];   // corner radius of the screen opening
overlap  = screen_border - 0.5; // bezel covers the black border, stopping just short of the screen
pocket_relief = 2;  // recess under the bezel above the pockets
cam_notch_w = 26;   // bezel notch around the front camera (top middle)
cam_margin  = 4.5;  // notch extends this far beyond the camera centre (toward the top edge)
corner_stop = 11;   // side stops at the corners keep the tablet from sliding
                    // into the pockets (they leave the middle of each edge open)

/* [Cable] */
cable_slot_w = 7;   // slot through the bottom edge (cable ~3.5-4.5 mm)
wall_hole_d  = 12;  // hole through the back of the right pocket, 30 mm below the port; 0 = none

/* [Snap fit] */
snap_w  = 18;       // width of each snap tab
snap_t  = 1.8;      // tab thickness (flex) - print in PETG for best results
snap_len = 11;       // tab length below the bezel
barb    = 0.9;      // barb depth (how far it hooks into the catch)
barb_h  = 2.5;      // barb height (lead-in ramp length)
snap_gap = 0.3;     // clearance around the tab in its recess
pry_gap = 1.5;      // recess extends this far below the tab tip, for prying

/* [Wall screws] */
wall_screw_d = 4.5; wall_head_d = 9;    // 4 mm / #8 wood screws

$fn = 48;
eps = 0.01;

W = tab_w + clear;
H = tab_h + clear;
T = tab_t + clear_t;
S = pocket_w + outer;               // side border width (same both sides)
X0 = -S;  X1 = W + S;               // outer extents
Y0 = -border;  Y1 = H + border;
DT = back_t + T;                    // tray height = tablet front face
FW = X1 - X0;  FH = Y1 - Y0;

// ---------------------------------------------------------------- helpers
module rrect(x, y, w, h, r) {
    translate([x + r, y + r]) offset(r) square([w - 2*r, h - 2*r]);
}
module rslab(x, y, w, h, z0, z1, r = 3) {
    translate([0, 0, z0]) linear_extrude(z1 - z0) rrect(x, y, w, h, r);
}

// Outer body of the whole frame (tray + bezel) with the chosen edge profile.
// The outline is convex, so a hull of inset slices gives exact chamfers/rounds.
ZT = DT + bezel_t;
function front_pts() =
    front_type == "chamfer" ? [[ZT - front_sz, 0], [ZT, front_sz]] :
    front_type == "round"   ? [for (i = [0:10]) let(a = 90*i/10)
                                 [ZT - front_sz + front_sz*sin(a), front_sz - front_sz*cos(a)]] :
                              [[ZT, 0]];
function back_pts() = back_ch > 0 ? [[0, back_ch], [back_ch, 0]] : [[0, 0]];
module outline(ins) {
    rrect(X0 + ins, Y0 + ins, FW - 2*ins, FH - 2*ins, max(corner_r - ins, 0.01));
}
module frame_body() {
    pts = concat(back_pts(), front_pts());
    hull() for (p = pts)
        translate([0, 0, min(p[0], ZT - 0.01)]) linear_extrude(0.01) outline(p[1]);
}
module frame_layer(z0, z1) {
    intersection() {
        frame_body();
        translate([X0 - 1, Y0 - 1, z0]) cube([FW + 2, FH + 2, z1 - z0]);
    }
}
// screen window through the bezel, with optional 45-degree bevel
module screen_window() {
    hull() {
        translate([0, 0, DT - 1]) linear_extrude(bezel_t + 1 - inner_bev + 0.01)
            rrect(overlap, overlap, W - 2*overlap, H - 2*overlap, window_r);
        translate([0, 0, ZT - 0.01]) linear_extrude(1)
            rrect(overlap - inner_bev, overlap - inner_bev,
                  W - 2*overlap + 2*inner_bev, H - 2*overlap + 2*inner_bev, window_r + (window_r > 0.1 ? inner_bev : 0));
    }
}

// snap tab positions: [x, y, rotation]; rotation turns local +Y (inward)
// to face into the frame from that edge. Kept clear of the cable slot.
tab_x = [22, W/2 - 24, W/2 + 24, W - 22];
tab_y = [H*0.28, H*0.72];
snap_tabs = concat(
    [for (x = tab_x) [x, Y0, 0]],       // bottom edge
    [for (x = tab_x) [x, Y1, 180]],     // top edge
    [for (y = tab_y) [X0, y, -90]],     // left edge
    [for (y = tab_y) [X1, y, 90]]);     // right edge

// local frame: X along the edge, Y inward from the outer face, Z up.
// profile drawn as [inward n, z] with z = 0 at the bezel underside (z = DT)
module at_edge(t) { translate([t[0], t[1], DT]) rotate([0, 0, t[2]]) children(); }
module edge_extrude(w) { translate([-w/2, 0, 0]) rotate([90, 0, 90]) linear_extrude(w) children(); }

module snap_tab() {                     // added to the bezel
    edge_extrude(snap_w) polygon([
        [0, eps], [0, -snap_len], [snap_t, -snap_len],
        [snap_t + barb, -snap_len + barb_h], [snap_t, -snap_len + barb_h],
        [snap_t, eps]]);
}
module snap_recess() {                  // cut from the tray
    r = snap_t + snap_gap;
    edge_extrude(snap_w + 2*snap_gap) polygon([
        [-1, 1], [-1, -snap_len - pry_gap], [r, -snap_len - pry_gap], [r, 1]]);
    // catch groove for the barb (flat top face holds the bezel down)
    edge_extrude(snap_w + 2*snap_gap) polygon([
        [r - eps, -snap_len - 0.4], [r + barb + 0.2, -snap_len - 0.4],
        [r + barb + 0.2, -snap_len + barb_h + 0.05], [r - eps, -snap_len + barb_h + 0.05]]);
    // lead-in chamfer on the top edge of the recess
    edge_extrude(snap_w + 2*snap_gap) polygon([[r - eps, 1], [r + barb + 0.4, 1], [r - eps, -barb - 0.4]]);
}

// wall screw positions: in the back plate under the tablet
wall_screws = [[22, 22], [22, H - 22], [W - 22, 22], [W - 22, H - 22],
               [W/2, H*0.3], [W/2, H*0.7]];
center_bar = 34;

// ---------------------------------------------------------------- tray
module tray() {
    difference() {
        frame_layer(0, DT);

        // tablet cavity
        translate([0, 0, back_t]) cube([W, H, DT]);
        // hidden pockets, left and right: full tablet height, open to the
        // tablet cavity except for 3 mm stop ribs at the four corners
        for (px = [-pocket_w, W]) {
            translate([px, corner_stop, pocket_floor]) cube([pocket_w, H - 2*corner_stop, DT]);
            translate([px + (px < 0 ? 0 : 3), 0, pocket_floor]) cube([pocket_w - 3, H, DT]);
        }

        // back windows (ventilation + less filament), either side of centre bar
        for (s = [[24, W/2 - center_bar/2], [W/2 + center_bar/2, W - 24]])
            translate([0, 0, -1]) linear_extrude(back_t + 2)
                rrect(s[0], 34, s[1] - s[0], H - 68, 6);

        // cable slot through the bottom edge, below the right pocket
        translate([W + pocket_w/2 - cable_slot_w/2, Y0 - 1, -eps])
            cube([cable_slot_w, border + corner_stop + 2, DT + 1]);
        // optional hole straight into the wall behind the right pocket
        if (wall_hole_d > 0)
            translate([W + pocket_w/2, clear/2 + port_y - 30, -1]) cylinder(d = wall_hole_d, h = pocket_floor + 2);

        // wall screws, countersunk into the floor of the tablet cavity
        for (p = wall_screws) translate([p[0], p[1], 0]) {
            translate([0, 0, -1]) cylinder(d = wall_screw_d, h = back_t + 2);
            translate([0, 0, back_t - (wall_head_d - wall_screw_d)/2])
                cylinder(d1 = wall_screw_d, d2 = wall_head_d, h = (wall_head_d - wall_screw_d)/2 + eps);
        }
        // snap-fit recesses + catch grooves in the outer walls
        for (t = snap_tabs) at_edge(t) snap_recess();
    }
}

// ---------------------------------------------------------------- bezel
// modelled in place (z = DT .. DT + bezel_t)
module bezel() {
    difference() {
        frame_layer(DT, ZT);
        // screen window
        screen_window();
        // camera notch, top middle
        translate([clear/2 + cam_x - cam_notch_w/2, H - overlap - 1, DT - 1])
            cube([cam_notch_w, overlap + 1 - (cam_y - cam_margin) - clear/2, bezel_t + 2]);
        // relief over the pockets so a chunky plug fits
        for (px = [-pocket_w, W])
            translate([px, 0, DT - eps]) cube([pocket_w, H, pocket_relief]);
    }
    for (t = snap_tabs) at_edge(t) snap_tab();
}

// ---------------------------------------------------------------- splitting
lap = center_bar - 8;
big = 2000;
module tray_half(side) {
    intersection() {
        tray();
        if (side == "left") union() {
            translate([-big, -big, -big]) cube([big + W/2 - lap/2, 2*big, 2*big]);
            translate([W/2 - lap/2 - eps, -big, -big]) cube([lap + 2*eps, 2*big, big + back_t/2]);
        } else union() {
            translate([W/2 + lap/2, -big, -big]) cube([big, 2*big, 2*big]);
            translate([W/2 - lap/2, -big, back_t/2]) cube([lap + eps, 2*big, big]);
        }
    }
}
module bezel_half(side) {
    intersection() {
        bezel();
        translate([side == "left" ? -big : W/2, -big, -big]) cube([big + (side == "left" ? W/2 : 0), 2*big, 2*big]);
    }
}
// bezels print face-down: flip so the front face is on the bed
module print_bezel() { translate([0, 0, DT + bezel_t]) mirror([0, 0, 1]) children(); }

// ---------------------------------------------------------------- preview
module ghost() {
    color("DimGray") translate([clear/2, clear/2, back_t]) linear_extrude(tab_t)
        rrect(0, 0, tab_w, tab_h, 9);
    color("Black") translate([clear/2 + cam_x, clear/2 + tab_h - cam_y, back_t + tab_t]) cylinder(d = 3, h = 0.3);
    // 90-degree USB-C plug, cable running down
    color("White") translate([W, clear/2 + port_y, back_t + tab_t/2]) {
        translate([0, -4, -1.6]) cube([5, 8, 3.2]);
        translate([5, -22, -5]) cube([10, 30, 10]);
        translate([10, -60, 0]) rotate([-90, 0, 0]) cylinder(d = 4, h = 40);
    }
}

if (part == "assembly") {
    color("SteelBlue") tray();
    color("LightSteelBlue") bezel();
    if (show_tablet) ghost();
}
else if (part == "tray")        tray();
else if (part == "tray_left")   tray_half("left");
else if (part == "tray_right")  tray_half("right");
else if (part == "bezel")       print_bezel() bezel();
else if (part == "bezel_left")  print_bezel() bezel_half("left");
else if (part == "bezel_right") print_bezel() bezel_half("right");
