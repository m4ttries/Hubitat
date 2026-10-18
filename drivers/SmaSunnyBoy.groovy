/**
 *  SMA Sunny Boy Solar Inverter
 *
 *  Polls an SMA inverter (Sunny Boy, Sunny Tripower, Sunny Island, Sunny Boy
 *  Storage) over its local WebConnect interface, the same JSON API the Home
 *  Assistant "SMA Solar" integration (pysma) uses. No cloud or Sunny Portal
 *  account is needed.
 *
 *  The inverter must have Speedwire/WebConnect enabled and be reachable from
 *  the hub. Log in with the same password you use for the inverter's web page.
 *
 *  Only values the inverter reports are published; anything it doesn't
 *  support (battery, energy meter, a second PV string) is simply left blank.
 *
 *  Licensed under the Apache License, Version 2.0
 */

import groovy.json.JsonOutput
import groovy.transform.Field
import java.math.RoundingMode

// key: SMA object id. idx: position in the value list (PV string or phase).
// factor: divide the raw value by this. tag: value is a status tag, not a number.
@Field static final List SENSORS = [
    [attr: "power",                 key: "6100_40263F00", unit: "W"],
    [attr: "totalYield",            key: "6400_00260100", unit: "kWh", factor: 1000],
    [attr: "dailyYield",            key: "6400_00262200", unit: "kWh", factor: 1000],
    [attr: "pvPowerA",              key: "6380_40251E00", idx: 0, unit: "W"],
    [attr: "pvPowerB",              key: "6380_40251E00", idx: 1, unit: "W"],
    [attr: "pvVoltageA",            key: "6380_40451F00", idx: 0, unit: "V", factor: 100],
    [attr: "pvVoltageB",            key: "6380_40451F00", idx: 1, unit: "V", factor: 100],
    [attr: "pvCurrentA",            key: "6380_40452100", idx: 0, unit: "A", factor: 1000],
    [attr: "pvCurrentB",            key: "6380_40452100", idx: 1, unit: "A", factor: 1000],
    [attr: "frequency",             key: "6100_00465700", unit: "Hz", factor: 100],
    [attr: "voltageL1",             key: "6100_00464800", unit: "V", factor: 100],
    [attr: "voltageL2",             key: "6100_00464900", unit: "V", factor: 100],
    [attr: "voltageL3",             key: "6100_00464A00", unit: "V", factor: 100],
    [attr: "currentL1",             key: "6100_40465300", unit: "A", factor: 1000],
    [attr: "currentL2",             key: "6100_40465400", unit: "A", factor: 1000],
    [attr: "currentL3",             key: "6100_40465500", unit: "A", factor: 1000],
    [attr: "powerL1",               key: "6100_40464000", unit: "W"],
    [attr: "powerL2",               key: "6100_40464100", unit: "W"],
    [attr: "powerL3",               key: "6100_40464200", unit: "W"],
    [attr: "gridPowerSupplied",     key: "6100_40463600", unit: "W"],
    [attr: "gridPowerAbsorbed",     key: "6100_40463700", unit: "W"],
    [attr: "gridTotalSupplied",     key: "6400_00462400", unit: "kWh", factor: 1000],
    [attr: "gridTotalAbsorbed",     key: "6400_00462500", unit: "kWh", factor: 1000],
    [attr: "batterySoc",            key: "6100_00295A00", unit: "%"],
    [attr: "batteryChargePower",    key: "6100_00496900", unit: "W"],
    [attr: "batteryDischargePower", key: "6100_00496A00", unit: "W"],
    [attr: "batteryTemperature",    key: "6100_40495B00", unit: "°C", factor: 10],
    [attr: "inverterStatus",        key: "6180_08214800", tag: true],
    [attr: "gridRelayStatus",       key: "6180_08416500", tag: true],
]

@Field static final String KEY_SERIAL = "6800_00A21E00"
@Field static final String KEY_NAME = "6800_10821E00"

@Field static final Map TAGS = [
    35: "Fault", 51: "Closed", 303: "Off", 307: "Ok", 311: "Open",
    455: "Warning", 16777213: "Not available"
]

// Attributes that read 0 rather than stale values when the inverter is asleep or offline.
@Field static final List ZERO_WHEN_IDLE = [
    "power", "pvPowerA", "pvPowerB", "pvCurrentA", "pvCurrentB", "powerL1", "powerL2", "powerL3",
    "currentL1", "currentL2", "currentL3"
]

metadata {
    definition(name: "SMA Sunny Boy Inverter", namespace: "m4ttries", author: "m4ttries") {
        capability "PowerMeter"
        capability "EnergyMeter"
        capability "VoltageMeasurement"
        capability "Sensor"
        capability "Refresh"
        capability "Initialize"

        attribute "connection", "enum", ["online", "offline"]
        attribute "lastUpdate", "string"
        SENSORS.each { attribute it.attr, it.tag ? "string" : "number" }

        command "logout"
    }

    preferences {
        input "host", "text", title: "Inverter IP address or hostname", required: true
        input "password", "password", title: "Inverter password", required: true
        input "group", "enum", title: "User group", options: ["usr": "User", "istl": "Installer"],
            defaultValue: "usr", required: true
        input "useSsl", "bool", title: "Use HTTPS", defaultValue: true,
            description: "Most inverters with current firmware only accept HTTPS."
        input "pollSeconds", "enum", title: "Poll interval",
            options: ["15": "15 seconds", "30": "30 seconds", "60": "1 minute", "300": "5 minutes", "900": "15 minutes"],
            defaultValue: "60", required: true
        input "energySource", "enum", title: "Report as Energy (kWh)",
            options: ["total": "Lifetime yield", "daily": "Today's yield"], defaultValue: "total", required: true
        input "logEnable", "bool", title: "Enable debug logging", defaultValue: false
    }
}

// ---------------------------------------------------------------------------
// Lifecycle
// ---------------------------------------------------------------------------

def installed() {
    initialize()
}

def updated() {
    logout()
    state.remove("serial")
    initialize()
    if (settings.logEnable) runIn(1800, "logsOff")
}

def uninstalled() {
    logout()
    unschedule()
}

def initialize() {
    unschedule("refresh")
    if (!settings.host || !settings.password) {
        log.warn "Set the inverter's address and password in Preferences"
        return
    }
    int secs = (settings.pollSeconds ?: "60") as int
    if (secs < 60) {
        schedule("${new Random().nextInt(secs)}/${secs} * * ? * *", "refresh")
    } else {
        schedule("${new Random().nextInt(60)} */${secs.intdiv(60)} * ? * *", "refresh")
    }
    runIn(2, "refresh")
}

def logsOff() {
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

// ---------------------------------------------------------------------------
// SMA WebConnect API
// ---------------------------------------------------------------------------

def refresh() {
    if (!state.sid && !login()) {
        markOffline()
        return
    }
    List keys = SENSORS*.key.unique()
    if (!state.serial) keys += [KEY_SERIAL, KEY_NAME]
    asynchttpPost("handleValues", requestParams("/dyn/getValues.json", [destDev: [], keys: keys], true),
        [retry: true])
}

def handleValues(resp, data) {
    if (resp.hasError()) {
        logDebug "getValues failed: ${resp.getErrorMessage()}"
        state.remove("sid")
        markOffline()
        return
    }
    Map json
    try {
        json = parseJson(resp.data)
    } catch (e) {
        log.warn "Unexpected response from inverter: ${resp.data}"
        markOffline()
        return
    }
    if (json.err) {
        // 401 means the session expired: log in again once.
        logDebug "getValues error ${json.err}"
        state.remove("sid")
        if (json.err == 401 && data?.retry && login()) {
            List keys = SENSORS*.key.unique()
            asynchttpPost("handleValues", requestParams("/dyn/getValues.json", [destDev: [], keys: keys], true),
                [retry: false])
        } else {
            markOffline()
        }
        return
    }

    // The result is keyed by the device id; a single inverter has one entry.
    Map values = json.result?.values()?.find { it instanceof Map } ?: [:]
    publish(values)
}

private void publish(Map values) {
    SENSORS.each { s ->
        def raw = extract(values[s.key], s.idx ?: 0)
        if (s.tag) {
            if (raw != null) sendEvent(name: s.attr, value: TAGS[raw as int] ?: "Code ${raw}")
            return
        }
        if (raw == null) {
            // The inverter reports this slot but has no reading (e.g. at night): read as zero.
            if (s.attr in ZERO_WHEN_IDLE && hasSlot(values[s.key], s.idx ?: 0)) {
                sendEvent(name: s.attr, value: 0, unit: s.unit)
            }
            return
        }
        def val = s.factor ? ((raw as BigDecimal) / s.factor).setScale(2, RoundingMode.HALF_UP) : raw
        sendEvent(name: s.attr, value: val, unit: s.unit)
    }

    if (values[KEY_SERIAL]) {
        state.serial = extract(values[KEY_SERIAL], 0)?.toString()
        device.updateDataValue("serialNumber", state.serial)
        def name = extract(values[KEY_NAME], 0)
        if (name) device.updateDataValue("deviceName", name.toString())
    }

    // Standard capability attributes mirror the detailed ones.
    def energy = device.currentValue(settings.energySource == "daily" ? "dailyYield" : "totalYield")
    if (energy != null) sendEvent(name: "energy", value: energy, unit: "kWh")
    def volts = device.currentValue("voltageL1")
    if (volts != null) sendEvent(name: "voltage", value: volts, unit: "V")

    sendEvent(name: "connection", value: "online")
    sendEvent(name: "lastUpdate", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))
    logDebug "Power ${device.currentValue('power')} W, today ${device.currentValue('dailyYield')} kWh"
}

// A value looks like {"1": [{"val": 1234}, {"val": 567}]}: one list entry per string or phase.
// Status values are tag lists: {"1": [{"val": [{"tag": 307}]}]}.
private extract(entry, int idx) {
    if (!(entry instanceof Map)) return null
    List list = entry.values().find { it instanceof List }
    if (!list || idx >= list.size()) return null
    def val = list[idx]?.val
    if (val instanceof List) return val ? val[0]?.tag : null
    return val
}

private boolean hasSlot(entry, int idx) {
    if (!(entry instanceof Map)) return false
    List list = entry.values().find { it instanceof List }
    return list && idx < list.size()
}

private boolean login() {
    boolean ok = false
    try {
        httpPost(requestParams("/dyn/login.json", [right: settings.group ?: "usr", pass: settings.password], false)) { resp ->
            def json = resp.data instanceof Map ? resp.data : parseJson(resp.data.toString())
            if (json.result?.sid) {
                state.sid = json.result.sid
                ok = true
                logDebug "Logged in"
            } else if (json.err == 503) {
                log.warn "Inverter refused login: too many open sessions. They time out after a few minutes."
            } else if (json.err) {
                log.warn "Inverter login failed with error ${json.err}"
            } else {
                log.warn "Inverter login failed: check the password and user group"
            }
        }
    } catch (e) {
        logDebug "Login failed: ${e.message}"
    }
    return ok
}

def logout() {
    if (!state.sid) return
    try {
        httpPost(requestParams("/dyn/logout.json", [:], true)) { logDebug "Logged out" }
    } catch (e) {
        logDebug "Logout failed: ${e.message}"
    }
    state.remove("sid")
}

private Map requestParams(String path, Map body, boolean withSid) {
    Map params = [
        uri: "${settings.useSsl == false ? 'http' : 'https'}://${settings.host}",
        path: path,
        requestContentType: "application/json",
        contentType: "application/json",
        body: JsonOutput.toJson(body),
        ignoreSSLIssues: true,
        timeout: 15
    ]
    if (withSid) params.query = [sid: state.sid]
    return params
}

private void markOffline() {
    if (device.currentValue("connection") != "offline") log.info "Inverter is offline"
    sendEvent(name: "connection", value: "offline")
    ZERO_WHEN_IDLE.each { attr ->
        if (device.currentValue(attr)) sendEvent(name: attr, value: 0)
    }
}

private void logDebug(String msg) {
    if (settings.logEnable) log.debug msg
}
