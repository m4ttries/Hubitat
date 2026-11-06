// Sump-pit probe separator for a ThirdReality 3RWS18BZ water leak sensor
// (or any conductive-probe leak sensor) using extension wires.
//
// Each wire runs down its own leg. Only the stripped tip at the bottom of a
// leg is bare metal. The legs are separated by an air gap, and each leg has
// two "umbrella" drip sheds (like a power-line insulator) so condensation
// running down the plastic drips off instead of forming a continuous film
// that links one tip to another.
//
// Mount it with zip ties through the two tunnels in the top block, around
// the sump discharge pipe. Set the height so the tips sit ABOVE the pump's
// normal turn-on level: the tips should only touch water if the pump fails.
//
// Units: mm. Modeled in installed orientation (tips down, z = 0 at the tips).
// Leave print_orientation = true to flip it for printing (top block on the
// bed, legs pointing up: no supports needed).

/* [Probes] */
probes        = 3;     // 2 is enough if your sensor triggers on any pair
pitch         = 25;    // centre-to-centre distance between legs
leg_d         = 10;
leg_len       = 45;    // tip to underside of the top block
bore_d        = 2.5;   // wire hole: wire outer diameter + ~0.4
tip_d         = 5;     // leg diameter at the very tip (45 degree chamfer)

/* [Drip sheds] */
shed_d        = 17;
shed_wall     = 2.2;   // vertical wall thickness (about 1.5 mm normal to the surface)
shed_z        = [12, 28]; // height of the lower rim of each shed above the tip

/* [Top block] */
block_h       = 20;
block_back    = 15;    // how far the block extends behind the leg centres
block_front   = 6;     // how far in front of the leg centres
tie_w         = 5.2;   // zip-tie tunnel (fits ties up to ~4.8 mm wide)
tie_t         = 2.2;
pipe_d        = 48.3;  // discharge pipe OD (1.5" PVC = 48.3, 1.25" = 42.2)
pipe_seat     = 1.5;   // depth of the curved seat on the back face (0 = flat)

print_orientation = true;

$fn = 64;
eps = 0.01;
leg_r  = leg_d / 2;
shed_h = shed_d / 2 - leg_r;              // 45 degree cone
span   = (probes - 1) * pitch;
blk_w  = span + shed_d;
xs     = [for (i = [0 : probes - 1]) i * pitch - span / 2];

module leg() {
    // shaft with a 45 degree chamfer down to the tip
    cylinder(h = (leg_r - tip_d / 2), r1 = tip_d / 2, r2 = leg_r);
    translate([0, 0, leg_r - tip_d / 2 - eps])
        cylinder(h = leg_len - (leg_r - tip_d / 2) + 2 * eps, r = leg_r);
    // umbrella sheds
    for (z = shed_z) translate([0, 0, z]) difference() {
        cylinder(h = shed_h, r1 = shed_d / 2, r2 = leg_r);
        translate([0, 0, -shed_wall]) cylinder(h = shed_h, r1 = shed_d / 2, r2 = leg_r);
    }
}

module block() {
    difference() {
        translate([-blk_w / 2, -block_back, leg_len])
            cube([blk_w, block_back + block_front, block_h]);
        // zip-tie tunnels, running the full width
        for (zc = [leg_len + block_h * 0.25, leg_len + block_h * 0.75])
            translate([-blk_w / 2 - 1, -block_back + 3, zc - tie_w / 2])
                cube([blk_w + 2, tie_t, tie_w]);
        // curved seat for the pipe
        if (pipe_seat > 0)
            translate([0, -block_back - pipe_d / 2 + pipe_seat, leg_len - 1])
                cylinder(h = block_h + 2, d = pipe_d, $fn = 180);
    }
}

module separator() {
    difference() {
        union() {
            for (x = xs) translate([x, 0, 0]) leg();
            block();
        }
        for (x = xs) translate([x, 0, 0]) {
            // wire bore, full length
            translate([0, 0, -1]) cylinder(h = leg_len + block_h + 2, d = bore_d, $fn = 24);
            // countersink at the top for a dab of silicone
            translate([0, 0, leg_len + block_h - 2.5 + eps])
                cylinder(h = 2.5, d1 = bore_d, d2 = bore_d + 5, $fn = 32);
        }
    }
}

if (print_orientation)
    translate([0, 0, leg_len + block_h]) rotate([180, 0, 0]) separator();
else
    separator();
