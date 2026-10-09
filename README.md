# Hubitat Apps

## Sprinkler Blowout (`apps/SprinklerBlowout.groovy`)

Runs your irrigation zones for a compressed-air winterization blowout.

**Settings**
- **Zones**: any switch-capable sprinkler zone devices. Zones run in natural name order (Zone 1, Zone 2, ... Zone 10).
- **Times to run each zone**: number of passes.
- **Run time per zone**: seconds each zone stays open per pass.
- **Pause between zone runs**: seconds with all zones closed so the compressor can recover.
- **Per-zone overrides** (optional): a different pass count or run time for each zone. Set the count to 0 to skip a zone.
- **Trigger switch** (optional): turning it on starts a blowout and turning it off stops one. A virtual switch works well for dashboards.
- **Notifications** (optional): sent when a blowout starts, finishes, or stops.

Zones run round-robin (Z1, pause, Z2, pause, Z3, pause, Z1, ...), so each line drains while the others run. Only one zone is open at a time. Stopping the app, or a hub reboot during a run, turns every zone off.

**Install**
1. In Hubitat, go to **Apps Code → New App**, paste in `apps/SprinklerBlowout.groovy`, and click **Save**.
2. Go to **Apps → Add User App → Sprinkler Blowout**, set it up, and click **Done**.
3. Open the app again and click **Start Blowout**.

> Safety: Start the compressor at a low pressure (most systems call for 50 PSI or less), and never let it run with every zone closed. Keep the pause short enough that the compressor tank doesn't over-pressurize the main line.
