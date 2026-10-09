/**
 *  Sprinkler Blowout
 *
 *  Helps winterize an irrigation system with compressed air. Select the zones,
 *  how many times to run each zone, how long each run lasts, and how long to
 *  pause between runs (to let the compressor recover). The app then cycles
 *  through the zones one at a time, round-robin, until every zone has been
 *  blown out the requested number of times.
 *
 *  Example with 3 zones, 2 passes: Z1, pause, Z2, pause, Z3, pause, Z1, pause, Z2, pause, Z3
 *
 *  Licensed under the Apache License, Version 2.0
 */

definition(
    name: "Sprinkler Blowout",
    namespace: "m4ttries",
    author: "m4ttries",
    description: "Cycle sprinkler zones for a compressed-air winterization blowout.",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    singleInstance: false
)

preferences {
    page(name: "mainPage")
}

// ---------------------------------------------------------------------------
// UI
// ---------------------------------------------------------------------------

def mainPage() {
    dynamicPage(name: "mainPage", title: "Sprinkler Blowout", install: true, uninstall: true) {
        section("Status") {
            paragraph statusText()
            if (state.running) {
                input "btnStop", "button", title: "Stop Blowout"
            } else if (app.getInstallationState() == "COMPLETE" && settings.zones) {
                input "btnStart", "button", title: "Start Blowout"
            } else {
                paragraph "<i>Select zones and click Done, then reopen the app to start a blowout.</i>"
            }
            input "btnRefresh", "button", title: "Refresh Status"
        }

        section("Zones") {
            input "zones", "capability.switch", title: "Sprinkler zones to blow out",
                multiple: true, required: true, submitOnChange: true
            paragraph "<small>Zones run in name order (e.g. Zone 1, Zone 2, ... Zone 10). " +
                "Only one zone is ever on at a time.</small>"
        }

        section("Run Settings") {
            input "cycles", "number", title: "Times to run each zone",
                range: "1..20", defaultValue: 3, required: true, submitOnChange: true
            input "runSeconds", "number", title: "Run time per zone (seconds)",
                range: "1..900", defaultValue: 30, required: true, submitOnChange: true
            input "pauseSeconds", "number", title: "Pause between zone runs (seconds)",
                range: "0..3600", defaultValue: 60, required: true, submitOnChange: true
            input "perZone", "bool", title: "Customize times / run time per zone",
                defaultValue: false, submitOnChange: true
        }

        if (settings.perZone && settings.zones) {
            section("Per-Zone Overrides (blank = use defaults above)") {
                sortedZones().each { z ->
                    paragraph "<b>${z.displayName}</b>"
                    input "cycles_${z.id}", "number", title: "Times to run",
                        range: "0..20", required: false, submitOnChange: true, width: 6
                    input "runSeconds_${z.id}", "number", title: "Run time (seconds)",
                        range: "1..900", required: false, submitOnChange: true, width: 6
                }
            }
        }

        if (settings.zones) {
            section("Plan") {
                paragraph planSummary()
            }
        }

        section("Options") {
            input "triggerSwitch", "capability.switch",
                title: "Start/stop with this switch (optional, e.g. a virtual switch on a dashboard)",
                required: false, submitOnChange: true
            input "notifyDevices", "capability.notification",
                title: "Send notifications to (optional)", multiple: true, required: false
            input "logEnable", "bool", title: "Enable debug logging", defaultValue: false
        }

        section {
            label title: "Name this app instance", required: false
        }
    }
}

def appButtonHandler(String btn) {
    switch (btn) {
        case "btnStart":
            startBlowout()
            break
        case "btnStop":
            stopBlowout("Stopped manually")
            break
        default:
            break // btnRefresh just re-renders the page
    }
}

// ---------------------------------------------------------------------------
// Lifecycle
// ---------------------------------------------------------------------------

def installed() {
    logDebug "installed"
    initialize()
}

def updated() {
    logDebug "updated"
    // Deliberately do not unschedule here: clicking Done while a blowout is
    // running should not orphan a zone in the "on" state.
    unsubscribe()
    initialize()
}

def uninstalled() {
    unschedule()
    allZonesOff()
}

def initialize() {
    if (settings.triggerSwitch) subscribe(settings.triggerSwitch, "switch", triggerSwitchHandler)
    subscribe(location, "systemStart", systemStartHandler)
}

def triggerSwitchHandler(evt) {
    logDebug "Trigger switch ${evt.value}"
    if (evt.value == "on") {
        startBlowout()
    } else if (state.running) {
        stopBlowout("Stopped by ${evt.displayName}")
    }
}

def systemStartHandler(evt) {
    // A hub reboot drops scheduled jobs; make sure nothing is left running.
    if (state.running) {
        allZonesOff()
        resetState()
        syncTriggerSwitch(false)
        notify("Sprinkler blowout interrupted by hub restart. All zones turned off.")
    }
}

// ---------------------------------------------------------------------------
// Sequencing
// ---------------------------------------------------------------------------

def startBlowout() {
    if (state.running) {
        log.info "Blowout already running"
        return
    }
    List queue = buildQueue()
    if (!queue) {
        log.warn "Nothing to run: select at least one zone with run count above 0"
        syncTriggerSwitch(false)
        return
    }

    allZonesOff()
    state.running = true
    state.queue = queue
    state.step = 0
    state.startedAt = now()
    syncTriggerSwitch(true)

    long totalSecs = estimateSeconds(queue)
    notify("Sprinkler blowout started: ${queue.size()} zone runs, about ${formatDuration(totalSecs)}.")
    runStep()
}

def runStep() {
    if (!state.running) return
    Map entry = state.queue[state.step]
    def zone = zoneById(entry.id)
    if (!zone) {
        log.warn "Zone ${entry.id} is no longer selected; skipping"
        advance()
        return
    }

    // Safety: never have more than one zone open with the compressor on.
    settings.zones.findAll { it.id != zone.id && it.currentValue("switch") == "on" }*.off()

    log.info "Blowing out ${zone.displayName} (pass ${entry.cycle}) for ${entry.secs}s " +
        "[run ${state.step + 1} of ${state.queue.size()}]"
    state.phase = "running"
    state.phaseEndsAt = now() + entry.secs * 1000L
    zone.on()
    runIn(entry.secs as Integer, "endStep")
}

def endStep() {
    if (!state.running) return
    Map entry = state.queue[state.step]
    def zone = zoneById(entry.id)
    zone?.off()
    logDebug "${zone?.displayName ?: entry.id} off"
    advance()
}

private void advance() {
    state.step = state.step + 1
    if (state.step >= state.queue.size()) {
        finish()
        return
    }
    Integer pause = pauseSecs()
    if (pause > 0) {
        state.phase = "pausing"
        state.phaseEndsAt = now() + pause * 1000L
        logDebug "Pausing ${pause}s before next zone"
        runIn(pause, "runStep")
    } else {
        runStep()
    }
}

private void finish() {
    long elapsed = (now() - (long) (state.startedAt ?: now())).intdiv(1000L)
    allZonesOff()
    resetState()
    syncTriggerSwitch(false)
    log.info "Sprinkler blowout complete"
    notify("Sprinkler blowout complete (${formatDuration(elapsed)}).")
}

def stopBlowout(String reason) {
    unschedule("runStep")
    unschedule("endStep")
    allZonesOff()
    boolean wasRunning = state.running
    resetState()
    syncTriggerSwitch(false)
    if (wasRunning) {
        log.info "Sprinkler blowout stopped: ${reason}"
        notify("Sprinkler blowout stopped: ${reason}. All zones turned off.")
    }
}

private void resetState() {
    state.running = false
    state.remove("queue")
    state.remove("step")
    state.remove("phase")
    state.remove("phaseEndsAt")
    state.remove("startedAt")
}

/**
 * Builds the ordered list of zone runs. Zones are run round-robin so each zone
 * gets time to drain while the others run and the compressor recovers.
 */
private List buildQueue() {
    List zs = sortedZones()
    int maxCycles = zs.collect { cyclesFor(it) }.max() ?: 0
    List queue = []
    (1..maxCycles).each { c ->
        zs.each { z ->
            if (cyclesFor(z) >= c) queue << [id: z.id.toString(), cycle: c, secs: runSecsFor(z)]
        }
    }
    return maxCycles > 0 ? queue : []
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private List sortedZones() {
    // Natural sort so "Zone 10" comes after "Zone 2".
    (settings.zones ?: []).sort { z ->
        z.displayName.toLowerCase().replaceAll(/\d+/) { it.padLeft(8, "0") }
    }
}

private zoneById(id) {
    settings.zones?.find { it.id.toString() == id.toString() }
}

private Integer cyclesFor(z) {
    def v = settings.perZone ? settings["cycles_${z.id}"] : null
    return (v != null ? v : (settings.cycles ?: 3)) as Integer
}

private Integer runSecsFor(z) {
    def v = settings.perZone ? settings["runSeconds_${z.id}"] : null
    return (v ?: (settings.runSeconds ?: 30)) as Integer
}

private Integer pauseSecs() {
    return (settings.pauseSeconds != null ? settings.pauseSeconds : 60) as Integer
}

private long estimateSeconds(List queue) {
    if (!queue) return 0
    long run = queue.sum { it.secs as long }
    return run + (queue.size() - 1) * (long) pauseSecs()
}

private void allZonesOff() {
    settings.zones?.each { it.off() }
}

private void syncTriggerSwitch(boolean on) {
    def sw = settings.triggerSwitch
    if (!sw) return
    String want = on ? "on" : "off"
    if (sw.currentValue("switch") != want) on ? sw.on() : sw.off()
}

private void notify(String msg) {
    settings.notifyDevices?.each { it.deviceNotification(msg) }
}

private String planSummary() {
    List queue = buildQueue()
    if (!queue) return "No zone runs configured."
    String rows = sortedZones().collect { z ->
        "<li>${z.displayName}: ${cyclesFor(z)} × ${runSecsFor(z)}s</li>"
    }.join("")
    return "<ul>${rows}</ul>" +
        "${queue.size()} zone runs, ${pauseSecs()}s pause between runs. " +
        "Estimated total time: <b>${formatDuration(estimateSeconds(queue))}</b>."
}

private String statusText() {
    if (!state.running) return "Idle."
    Map entry = state.queue[state.step]
    String name = zoneById(entry.id)?.displayName ?: "unknown zone"
    long remaining = Math.max(0L, ((long) (state.phaseEndsAt ?: now()) - now()).intdiv(1000L))
    String phase = state.phase == "pausing" ?
        "Pausing (${remaining}s left) before <b>${name}</b>" :
        "Blowing out <b>${name}</b> (${remaining}s left)"
    return "${phase}, pass ${entry.cycle}. Run ${state.step + 1} of ${state.queue.size()}."
}

private String formatDuration(long secs) {
    long m = secs.intdiv(60)
    long s = secs % 60
    return m > 0 ? "${m}m ${s}s" : "${s}s"
}

private void logDebug(String msg) {
    if (settings.logEnable) log.debug msg
}
