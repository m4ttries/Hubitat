/**
 *  SMA ennexOS Inverter
 *
 *  Polls an SMA inverter built on ennexOS (Sunny Tripower X, Sunny Boy Smart
 *  Energy, and similar) over its local web API, the same one its own web page
 *  and the Home Assistant "SMA Devices Plus" integration (pysmaplus) use.
 *  No cloud or Sunny Portal account is needed.
 *
 *  Log in with a user account set up on the inverter's web page.
 *
 *  Only values the inverter reports are published; anything it doesn't
 *  support (battery, energy meter, a third PV string) is simply left blank.
 *  Use the "discover" command to log every value the inverter offers.
 *
 *  Licensed under the Apache License, Version 2.0
 */

import groovy.json.JsonOutput
import groovy.transform.Field
import java.math.RoundingMode

// channel: the ennexOS measurement name, without the "Measurement." prefix.
// Array channels get ".1", ".2", ... appended per string or battery.
// factor: divide the value by this. tag: value is a status tag, not a number.
@Field static final List SENSORS = [
    [attr: "power",                 channel: "GridMs.TotW", unit: "W"],
    [attr: "pvPower",               channel: "GridMs.TotW.Pv", unit: "W"],
    [attr: "totalYield",            channel: "Metering.TotWhOut", unit: "kWh", factor: 1000],
    [attr: "dailyYield",            channel: "Metering.DyWhOut", unit: "kWh", factor: 1000],
    [attr: "pvPowerA",              channel: "DcMs.Watt.1", unit: "W"],
    [attr: "pvPowerB",              channel: "DcMs.Watt.2", unit: "W"],
    [attr: "pvPowerC",              channel: "DcMs.Watt.3", unit: "W"],
    [attr: "pvVoltageA",            channel: "DcMs.Vol.1", unit: "V"],
    [attr: "pvVoltageB",            channel: "DcMs.Vol.2", unit: "V"],
    [attr: "pvVoltageC",            channel: "DcMs.Vol.3", unit: "V"],
    [attr: "pvCurrentA",            channel: "DcMs.Amp.1", unit: "A"],
    [attr: "pvCurrentB",            channel: "DcMs.Amp.2", unit: "A"],
    [attr: "pvCurrentC",            channel: "DcMs.Amp.3", unit: "A"],
    [attr: "frequency",             channel: "GridMs.Hz", unit: "Hz"],
    [attr: "voltageL1",             channel: "GridMs.PhV.phsA", unit: "V"],
    [attr: "voltageL2",             channel: "GridMs.PhV.phsB", unit: "V"],
    [attr: "voltageL3",             channel: "GridMs.PhV.phsC", unit: "V"],
    [attr: "currentL1",             channel: "GridMs.A.phsA", unit: "A"],
    [attr: "currentL2",             channel: "GridMs.A.phsB", unit: "A"],
    [attr: "currentL3",             channel: "GridMs.A.phsC", unit: "A"],
    [attr: "powerL1",               channel: "GridMs.W.phsA", unit: "W"],
    [attr: "powerL2",               channel: "GridMs.W.phsB", unit: "W"],
    [attr: "powerL3",               channel: "GridMs.W.phsC", unit: "W"],
    [attr: "gridPowerSupplied",     channel: "Metering.GridMs.TotWOut", unit: "W"],
    [attr: "gridPowerAbsorbed",     channel: "Metering.GridMs.TotWIn", unit: "W"],
    [attr: "gridTotalSupplied",     channel: "Metering.GridMs.TotWhOut", unit: "kWh", factor: 1000],
    [attr: "gridTotalAbsorbed",     channel: "Metering.GridMs.TotWhIn", unit: "kWh", factor: 1000],
    [attr: "batterySoc",            channel: "Bat.ChaStt", unit: "%"],
    [attr: "batteryChargePower",    channel: "BatChrg.CurBatCha", unit: "W"],
    [attr: "batteryDischargePower", channel: "BatDsch.CurBatDsch", unit: "W"],
    [attr: "batteryTemperature",    channel: "Bat.TmpVal.1", unit: "°C"],
    [attr: "inverterTemperature",   channel: "Coolsys.Inverter.TmpVal.1", unit: "°C"],
    [attr: "inverterStatus",        channel: "Operation.Health", tag: true],
    [attr: "operatingStatus",       channel: "Operation.OpStt", tag: true],
    [attr: "gridRelayStatus",       channel: "Operation.GriSwStt", tag: true],
]

@Field static final String DEVICE = "IGULD:SELF"
@Field static final String PLANT = "Plant:1"

@Field static final Map TAGS = [
    35: "Fault", 51: "Closed", 295: "MPP", 303: "Off", 307: "Ok", 308: "On", 309: "Operation",
    311: "Open", 381: "Stop", 455: "Warning", 1295: "Standby", 1392: "Errors",
    1393: "Waiting for PV voltage", 1394: "Waiting for valid AC grid", 1455: "Emergency stop",
    1466: "Waiting", 1467: "Starting", 1468: "MPP search", 1469: "Shutdown", 1795: "Locked",
    2119: "Derating", 16777213: "Not available"
]

// Attributes that read 0 rather than stale values when the inverter is asleep or offline.
@Field static final List ZERO_WHEN_IDLE = [
    "power", "pvPower", "pvPowerA", "pvPowerB", "pvPowerC", "pvCurrentA", "pvCurrentB", "pvCurrentC",
    "powerL1", "powerL2", "powerL3", "currentL1", "currentL2", "currentL3"
]

metadata {
    definition(name: "SMA ennexOS Inverter", namespace: "m4ttries", author: "m4ttries") {
        capability "PowerMeter"
        capability "EnergyMeter"
        capability "VoltageMeasurement"
        capability "Sensor"
        capability "Refresh"
        capability "Initialize"

        attribute "connection", "enum", ["online", "offline"]
        attribute "lastUpdate", "string"
        SENSORS.each { attribute it.attr, it.tag ? "string" : "number" }

        command "discover"
    }

    preferences {
        input "host", "text", title: "Inverter IP address or hostname", required: true
        input "username", "text", title: "Username", required: true,
            description: "A user account from the inverter's web page."
        input "password", "password", title: "Password", required: true
        input "useSsl", "bool", title: "Use HTTPS", defaultValue: true
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
    clearToken()
    state.remove("serial")
    initialize()
    if (settings.logEnable) runIn(1800, "logsOff")
}

def uninstalled() {
    unschedule()
}

def initialize() {
    unschedule("refresh")
    if (!settings.host || !settings.username || !settings.password) {
        log.warn "Set the inverter's address, username, and password in Preferences"
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
// ennexOS web API
// ---------------------------------------------------------------------------

def refresh() {
    if (!ensureToken()) {
        markOffline()
        return
    }
    if (!state.serial) readDeviceInfo()
    requestLive([retry: true])
}

// Logs every measurement the inverter and plant report, to help map new values.
def discover() {
    if (!ensureToken()) {
        log.warn "Can't reach the inverter to discover its values"
        return
    }
    requestLive([discover: true])
}

// Asks for the inverter's own values and the plant totals (energy meter, battery) together.
private void requestLive(Map data) {
    Map params = requestParams("/api/v1/measurements/live")
    params.body = JsonOutput.toJson([[componentId: DEVICE], [componentId: PLANT]])
    asynchttpPost("handleLive", params, data)
}

def handleLive(resp, data) {
    if (resp.hasError()) {
        int status = resp.status ?: 0
        logDebug "Live data request failed (${status}): ${resp.getErrorMessage()}"
        clearToken()
        // 401/400 means the token was rejected: log in again once.
        if ((status == 401 || status == 400) && data?.retry && ensureToken()) {
            requestLive([retry: false])
        } else {
            markOffline()
        }
        return
    }
    List items
    try {
        items = parseJson(resp.data) as List
    } catch (e) {
        log.warn "Unexpected response from inverter: ${resp.data}"
        markOffline()
        return
    }

    Map inverter = [:]
    Map plant = [:]
    items.each { item ->
        flatten(item, item.componentId == PLANT ? plant : inverter)
    }
    if (data?.discover) {
        inverter.sort().each { k, v -> log.info "Inverter ${k} = ${v}" }
        plant.sort().each { k, v -> log.info "Plant ${k} = ${v}" }
        return
    }
    // Prefer the inverter's own reading; fall back to the plant total.
    publish(plant + inverter)
}

// Turns {"channelId": "Measurement.DcMs.Watt[]", "values": [{"values": [1, 2]}]}
// into ["DcMs.Watt.1": 1, "DcMs.Watt.2": 2]. A channel with no reading
// (common at night) maps to null so it can read as zero.
private void flatten(Map item, Map out) {
    String name = item.channelId?.toString()?.replace("Measurement.", "")?.replace("[]", "")
    if (!name) return
    Map entry = item.values instanceof List && item.values ? item.values[0] : [:]
    if (entry.containsKey("value")) {
        out[name] = entry.value
    } else if (entry.values instanceof List) {
        entry.values.eachWithIndex { v, i -> out["${name}.${i + 1}".toString()] = v }
    } else {
        out[name] = null
    }
}

private void publish(Map values) {
    SENSORS.each { s ->
        if (!values.containsKey(s.channel)) return
        def raw = values[s.channel]
        if (s.tag) {
            if (raw != null) sendEvent(name: s.attr, value: tagName(raw))
            return
        }
        if (raw == null || !(raw.toString().isNumber())) {
            if (s.attr in ZERO_WHEN_IDLE) sendEvent(name: s.attr, value: 0, unit: s.unit)
            return
        }
        BigDecimal val = raw.toString() as BigDecimal
        if (s.factor) val = val / s.factor
        sendEvent(name: s.attr, value: tidy(val), unit: s.unit)
    }

    // Some models have no daily yield channel: count from the first reading of each day.
    if (values.containsKey("Metering.DyWhOut")) state.hasDailyYield = true
    if (!state.hasDailyYield) trackDailyYield()

    def energy = device.currentValue(settings.energySource == "daily" ? "dailyYield" : "totalYield")
    if (energy != null) sendEvent(name: "energy", value: energy, unit: "kWh")
    def volts = device.currentValue("voltageL1")
    if (volts != null) sendEvent(name: "voltage", value: volts, unit: "V")

    sendEvent(name: "connection", value: "online")
    sendEvent(name: "lastUpdate", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))
    logDebug "Power ${device.currentValue('power')} W, today ${device.currentValue('dailyYield')} kWh"
}

private void trackDailyYield() {
    def total = device.currentValue("totalYield")
    if (total == null) return
    String today = new Date().format("yyyy-MM-dd", location.timeZone)
    if (state.yieldDay != today) {
        state.yieldDay = today
        state.yieldDayStart = total
    }
    BigDecimal daily = (total as BigDecimal) - (state.yieldDayStart as BigDecimal)
    sendEvent(name: "dailyYield", value: tidy(daily.max(0)), unit: "kWh")
}

// Rounds to 2 decimals and drops ".00" from whole numbers.
private tidy(BigDecimal val) {
    BigDecimal r = val.setScale(2, RoundingMode.HALF_UP)
    return r.signum() == 0 || r.scale() <= 0 || r.stripTrailingZeros().scale() <= 0 ? r.longValue() : r
}

private String tagName(raw) {
    if (raw.toString().isInteger()) {
        int code = raw.toString() as int
        return TAGS[code] ?: "Code ${code}"
    }
    return raw.toString()
}

private void readDeviceInfo() {
    try {
        httpGet(requestParams("/api/v1/plants/${PLANT}/devices/${DEVICE}")) { resp ->
            Map info = resp.data instanceof Map ? resp.data : parseJson(resp.data.toString())
            if (info.serial) {
                state.serial = info.serial.toString()
                device.updateDataValue("serialNumber", state.serial)
            }
            if (info.product) device.updateDataValue("model", info.product.toString())
            if (info.name) device.updateDataValue("deviceName", info.name.toString())
            if (info.firmwareVersion) device.updateDataValue("firmware", info.firmwareVersion.toString())
        }
    } catch (e) {
        logDebug "Couldn't read device info: ${e.message}"
    }
}

// Logs in when there's no token or it's about to expire. Tokens last about an hour.
private boolean ensureToken() {
    if (state.token && now() < (state.tokenExpires ?: 0)) return true
    boolean ok = false
    Map params = [
        uri: baseUri(),
        path: "/api/v1/token",
        requestContentType: "application/x-www-form-urlencoded",
        contentType: "application/json",
        body: [grant_type: "password", username: settings.username, password: settings.password],
        ignoreSSLIssues: true,
        timeout: 15
    ]
    try {
        httpPost(params) { resp ->
            Map json = resp.data instanceof Map ? resp.data : parseJson(resp.data.toString())
            if (json.access_token) {
                long expiresIn = json.expires_in?.toString()?.isNumber() ? (json.expires_in as long) : 3600
                state.token = json.access_token
                state.tokenExpires = now() + (long) (Math.max(expiresIn, 60) * 900)
                ok = true
                logDebug "Logged in"
            } else {
                log.warn "Inverter login failed: no token in reply"
            }
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        if (e.statusCode in [400, 401, 403]) {
            log.warn "Inverter login failed: check the username and password"
        } else {
            log.warn "Inverter login failed with HTTP ${e.statusCode}"
        }
    } catch (e) {
        logDebug "Login failed: ${e.message}"
    }
    return ok
}

private void clearToken() {
    state.remove("token")
    state.remove("tokenExpires")
}

private String baseUri() {
    return "${settings.useSsl == false ? 'http' : 'https'}://${settings.host}"
}

private Map requestParams(String path) {
    return [
        uri: baseUri(),
        path: path,
        requestContentType: "application/json",
        contentType: "application/json",
        headers: [Authorization: "Bearer ${state.token}"],
        ignoreSSLIssues: true,
        timeout: 15
    ]
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
