# Hubitat Apps

## SMA Sunny Boy Inverter (`drivers/SmaSunnyBoy.groovy`)

A device driver that reads your SMA solar inverter over the local network, the same way the Home Assistant **SMA Solar** integration does (the inverter's WebConnect JSON API, as used by pysma). It needs no cloud account and works with Sunny Boy, Sunny Tripower, Sunny Boy Storage, and Sunny Island models that have WebConnect.

**Preferences**
- **IP address**, **password**, and **user group** (User or Installer): the same login you use for the inverter's web page.
- **Use HTTPS**: leave on unless your inverter only serves HTTP. Self-signed certificates are accepted.
- **Poll interval**: 15 seconds to 15 minutes.
- **Report as Energy**: whether the standard `energy` attribute shows lifetime or today's yield.

**What it reports**
- `power` (AC output, W), `energy` (kWh), `voltage`, `dailyYield`, `totalYield`
- PV strings A/B: `pvPowerA/B`, `pvVoltageA/B`, `pvCurrentA/B`
- Grid: `frequency`, per-phase `voltageL1-3`, `currentL1-3`, `powerL1-3`, `gridRelayStatus`
- `inverterStatus` (Ok, Warning, Fault, Off), `connection` (online or offline), `lastUpdate`
- If you have an SMA Energy Meter: `gridPowerSupplied`, `gridPowerAbsorbed`, `gridTotalSupplied`, `gridTotalAbsorbed`
- If you have an SMA battery: `batterySoc`, `batteryChargePower`, `batteryDischargePower`, `batteryTemperature`

Attributes your inverter doesn't support stay blank. At night, or while the inverter is unreachable, power and current read 0.

**Install**
1. In Hubitat, go to **Drivers Code → New Driver**, paste in `drivers/SmaSunnyBoy.groovy`, and click **Save**.
2. Go to **Devices → Add Device → Virtual**, name it, and choose **SMA Sunny Boy Inverter** as the type.
3. Enter the inverter's IP address and password and click **Save Preferences**.

> Notes: Give the inverter a fixed IP address (a DHCP reservation). SMA inverters allow only a few logins at once. If you see "too many open sessions", wait a few minutes for old ones to expire. Newer SMA models built on ennexOS (Sunny Tripower X, Sunny Boy Smart Energy) use a different API and aren't supported, the same as in Home Assistant.

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
