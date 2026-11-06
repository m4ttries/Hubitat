// Amazon Fire HD 10 (13th gen, model TG425K) - landscape picture-frame wall mount
// -----------------------------------------------------------------------------
// Two parts:
//   TRAY  - screws to the wall; the tablet drops into it face-up.
//   BEZEL - a front frame that screws onto the tray and covers everything
//           except the screen (with a notch for the front camera).
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
// centre wall screws. Bezel halves butt together at the centre, and each
// half is screwed to the tray on both sides of the seam.

part = "assembly";  // assembly | tray | tray_left | tray_right | bezel | bezel_left | bezel_right
show_tablet = true; // assembly preview only

/* [Tablet] */
tab_w = 247.1;      // landscape width  (Amazon spec 9.73")
tab_h = 165.9;      // landscape height (Amazon spec 6.53")
tab_t = 8.6;        // thickness        (Amazon spec 0.34")
clear   = 0.8;      // clearance added to width and height
clear_t = 0.6;      // clearance added to thickness (add more for a skin)

/* [Frame] */
pocket_w = 22;      // hidden pocket each side (room for the 90-degree plug)
outer    = 5;       // outer wall thickness
border   = 14;      // top / bottom border width
back_t   = 4;       // back plate under the tablet
pocket_floor = 2;   // back plate under the pockets (plug can sit lower)
bezel_t  = 3;       // front frame thickness
overlap  = 7;       // how far the bezel covers the tablet's black border
pocket_relief = 2;  // recess under the bezel above the pockets
cam_notch_w = 26;   // bezel notch around the front camera (top middle)
corner_stop = 11;   // side stops at the corners keep the tablet from sliding
                    // into the pockets (they leave the middle of each edge open)

/* [Cable] */
cable_slot_w = 7;   // slot through the bottom edge (cable ~3.5-4.5 mm)
wall_hole_d  = 12;  // hole through the back of the right pocket; 0 = none

/* [Screws] */
wall_screw_d = 4.5; wall_head_d = 9;    // 4 mm / #8 wood screws
m3_pilot = 2.6;     // M3 self-tapping / machine screw into plastic
m3_clear = 3.4; m3_head = 6.4;

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

// bezel screw positions: all in the solid top/bottom borders
bezel_screw_x = [X0 + 8, W*0.25, W/2 - 14, W/2 + 14, W*0.75, X1 - 8];
bezel_screws = [for (x = bezel_screw_x) for (y = [Y0 + border/2, Y1 - border/2]) [x, y]];

// wall screw positions: in the back plate under the tablet
wall_screws = [[22, 22], [22, H - 22], [W - 22, 22], [W - 22, H - 22],
               [W/2, H*0.3], [W/2, H*0.7]];
center_bar = 34;

// ---------------------------------------------------------------- tray
module tray() {
    difference() {
        rslab(X0, Y0, FW, FH, 0, DT, 4);

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
            translate([W + pocket_w/2, H/2, -1]) cylinder(d = wall_hole_d, h = pocket_floor + 2);

        // wall screws, countersunk into the floor of the tablet cavity
        for (p = wall_screws) translate([p[0], p[1], 0]) {
            translate([0, 0, -1]) cylinder(d = wall_screw_d, h = back_t + 2);
            translate([0, 0, back_t - (wall_head_d - wall_screw_d)/2])
                cylinder(d1 = wall_screw_d, d2 = wall_head_d, h = (wall_head_d - wall_screw_d)/2 + eps);
        }
        // pilot holes for the bezel screws
        for (p = bezel_screws) translate([p[0], p[1], DT - 10]) cylinder(d = m3_pilot, h = 11);
    }
}

// ---------------------------------------------------------------- bezel
// modelled in place (z = DT .. DT + bezel_t)
module bezel() {
    difference() {
        rslab(X0, Y0, FW, FH, DT, DT + bezel_t, 4);
        // screen window
        translate([0, 0, DT - 1]) linear_extrude(bezel_t + 2)
            rrect(overlap, overlap, W - 2*overlap, H - 2*overlap, 3);
        // camera notch, top middle
        translate([W/2 - cam_notch_w/2, H - overlap - 1, DT - 1])
            cube([cam_notch_w, overlap - 1.5, bezel_t + 2]);
        // relief over the pockets so a chunky plug fits
        for (px = [-pocket_w, W])
            translate([px, 0, DT - eps]) cube([pocket_w, H, pocket_relief]);
        // countersunk M3 holes
        for (p = bezel_screws) translate([p[0], p[1], DT - 1]) {
            cylinder(d = m3_clear, h = bezel_t + 2);
            translate([0, 0, 1 + bezel_t - (m3_head - m3_clear)/2])
                cylinder(d1 = m3_clear, d2 = m3_head, h = (m3_head - m3_clear)/2 + eps);
        }
    }
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
    color("Black") translate([W/2, H - 7.5, back_t + tab_t]) cylinder(d = 3, h = 0.3);
    // 90-degree USB-C plug, cable running down
    color("White") translate([W, H*0.55, back_t + tab_t/2]) {
        translate([0, -4, -1.6]) cube([5, 8, 3.2]);
        translate([5, -22, -5]) cube([10, 30, 10]);
        translate([10, -60, 0]) rotate([-90, 0, 0]) cylinder(d = 4, h = 40);
    }
}

if (part == "assembly") {
    color("SteelBlue") tray();
    color("LightSteelBlue", 0.9) translate([0, 0, 0]) bezel();
    if (show_tablet) ghost();
}
else if (part == "tray")        tray();
else if (part == "tray_left")   tray_half("left");
else if (part == "tray_right")  tray_half("right");
else if (part == "bezel")       print_bezel() bezel();
else if (part == "bezel_left")  print_bezel() bezel_half("left");
else if (part == "bezel_right") print_bezel() bezel_half("right");
