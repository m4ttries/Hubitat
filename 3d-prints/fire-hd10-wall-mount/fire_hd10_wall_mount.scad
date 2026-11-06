// Amazon Fire HD 10 (13th gen, model TG425K) - landscape wall mount
// -------------------------------------------------------------------
// Orientation (looking at the wall): camera top-middle, buttons and
// USB-C on the RIGHT edge. The right edge is left completely open (only
// a small stop at the very bottom-right corner) so a 90-degree USB-C
// plug can be used, and the tablet is held off the wall by `back_t` so
// the part of the plug that is thicker than the tablet clears the wall.
//
// Mounting: screw the frame to the wall. Push the tablet's top edge up
// under the two top hooks (there is `lift` mm of extra room), swing the
// bottom in, and let it drop onto the bottom ledge behind the bottom lips.
//
// Coordinates: X = across the wall (left->right), Y = up, Z = out of wall.
// Print with the back (Z=0) on the bed. No supports needed.
//
// part = "full"  : one piece (~256 x 189 mm - needs a 256+ mm bed)
// part = "left" / "right" : two halves joined by a screwed lap joint at
//                  the centre (each ~140 x 189 mm, fits any 220 mm bed)
part = "full";
show_tablet = false;   // preview only: ghost tablet + 90-degree USB-C plug

/* [Tablet] */
tab_w = 247.1;     // landscape width  (Amazon spec 9.73")
tab_h = 165.9;     // landscape height (Amazon spec 6.53")
tab_t = 8.6;       // thickness        (Amazon spec 0.34")
clear  = 0.8;      // total clearance added to width/height
clear_t = 0.8;     // clearance added to thickness (use more for a skin/case)

/* [Frame] */
back_t   = 6;      // back plate thickness = stand-off from wall
band     = 24;     // width of back frame rails
wall     = 4;      // side/floor wall thickness
lip_t    = 2.4;    // thickness of front retaining lips
lip_bot  = 6;      // how far the bottom lips rise over the screen bezel
lip_top  = 5;      // how far the top hooks reach down over the bezel
lift     = 9;      // extra headroom at top for tilt-in insertion (> lip_bot)
top_hook_w = 45;   // width of each top hook (camera is in the clear middle)
side_stop_h = 14;  // height of corner side stops
right_stop  = true;// small bottom-right stop; set false if it hits your plug
center_bar  = 34;  // vertical centre bar width (carries lap joint)

/* [Screws] */
screw_d   = 4.5;   // #8 / 4mm wood screw clearance
head_d    = 9;     // countersink diameter
lap_screw_d = 4.5;

$fn = 48;

W = tab_w + clear;
H = tab_h + clear;
T = tab_t + clear_t;
D = back_t + T + lip_t;     // total depth from wall
eps = 0.01;

// ---------- primitives ----------
module rbox(size, r=2) {    // box rounded in XY
    translate([r, r, 0]) linear_extrude(size[2])
        offset(r) square([size[0]-2*r, size[1]-2*r]);
}

module csk_hole(x, y) {     // countersunk from the FRONT face of back plate
    translate([x, y, -1]) cylinder(d=screw_d, h=back_t+2);
    translate([x, y, back_t-(head_d-screw_d)/2]) cylinder(d1=screw_d, d2=head_d, h=(head_d-screw_d)/2+eps);
    translate([x, y, back_t]) cylinder(d=head_d, h=T+lip_t+1); // tool access
}

// ---------- back frame ----------
module back_frame() {
    x0 = -wall; x1 = W - 2;          // stops short of right edge (plug!)
    y0 = -wall; y1 = H + lift + wall;
    difference() {
        translate([x0, y0, 0]) rbox([x1-x0, y1-y0, back_t], 4);
        // two windows either side of the centre bar
        for (s = [[x0+band, W/2-center_bar/2], [W/2+center_bar/2, x1-band]])
            translate([s[0], y0+band, -1])
                rbox([s[1]-s[0], (y1-y0)-2*band, back_t+2], 6);
    }
}

// ---------- bottom ledge with lips ----------
module bottom_segment(xa, xb) {
    // floor
    translate([xa, -wall, 0]) cube([xb-xa, wall, D]);
    // front lip rising over the bezel
    translate([xa, -wall, back_t+T]) cube([xb-xa, wall+lip_bot, lip_t]);
}

// ---------- top hook ----------
module top_hook(xa, xb) {
    translate([xa, H+lift, 0]) cube([xb-xa, wall, D]);
    translate([xa, H-lip_top, back_t+T]) cube([xb-xa, lift+lip_top+wall, lip_t]);
}

// ---------- side stops ----------
module left_stop(ya, yb) {
    translate([-wall, ya, 0]) cube([wall, yb-ya, back_t+T]);
}

module mount() {
    gap = 10;   // gap at the centre so nothing but the back frame crosses the split
    difference() {
        union() {
            back_frame();
            bottom_segment(-wall, W/2 - gap);
            bottom_segment(W/2 + gap, W + (right_stop ? wall : 0));
            top_hook(-wall, -wall + top_hook_w);
            top_hook(W - 2 - top_hook_w, W - 2);
            // left: stops only at the corners (keeps speakers/edge open)
            left_stop(-wall, side_stop_h);
            left_stop(H + lift - side_stop_h, H + lift + wall);
            // right: only a short stop at the bottom corner
            if (right_stop)
                translate([W, -wall, 0]) cube([wall, side_stop_h, back_t+T]);
        }
        // wall screw holes (in the frame rails, outside the windows)
        for (p = [[band/2 - wall, band/2 - wall],
                  [band/2 - wall, H + lift + wall - band/2],
                  [W - 2 - band/2, band/2 - wall],
                  [W - 2 - band/2, H + lift + wall - band/2]])
            csk_hole(p[0], p[1]);
        // centre screws (also clamp the lap joint when printed in halves)
        for (y = [H*0.3, H*0.7]) csk_hole(W/2, y);
    }
}

// ---------- split into halves with a lap joint ----------
lap = center_bar - 6;   // overlap width
module half(side) {
    big = 1000;
    intersection() {
        mount();
        if (side == "left")
            union() {
                translate([-big, -big, -big]) cube([big + W/2 - lap/2, 2*big, 2*big]);
                translate([W/2 - lap/2 - eps, -big, -big]) cube([lap + 2*eps, 2*big, big + back_t/2]);
            }
        else
            union() {
                translate([W/2 + lap/2, -big, -big]) cube([big, 2*big, 2*big]);
                translate([W/2 - lap/2, -big, back_t/2]) cube([lap + eps, 2*big, big]);
            }
    }
}

module ghost() {
    color("DimGray", 0.85) translate([clear/2, 0, back_t]) rbox([tab_w, tab_h, tab_t], 9);
    color("Black") translate([clear/2 + tab_w/2, tab_h - 5, back_t + tab_t]) cylinder(d=3, h=0.3);
    // right-angle USB-C plug on the right edge, cable heading down
    color("White") translate([W, tab_h*0.6, back_t + tab_t/2]) {
        translate([0, -3.5, -1.5]) cube([6, 7, 3]);
        translate([6, -22, -4]) cube([10, 28, 8]);
    }
}
if (show_tablet) ghost();
if (part == "full") mount();
else if (part == "left") half("left");
else if (part == "right")
    // flip so the lap layer prints flat; back face still flat on bed
    translate([0, 0, 0]) half("right");
