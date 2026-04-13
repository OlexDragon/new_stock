import CachedInfo from './cached-info.mjs';
import { handleLoginIfNeeded } from '../global-login-handler.mjs';
import { unitSerialNumber } from '../../calibration.mjs';

class CalibRwInfo extends CachedInfo {
    constructor() {
		super(
		    CachedInfo.makeFetcher("/calibration/rest/calib_rw_info", { sn: unitSerialNumber }, raw => this.#parse(raw)), 1000 );
    }

    async getValueAsync() {
        try {
            return await super.getValueAsync();
        } catch (err) {
            if (err.message === "not_logged_in") {
                await handleLoginIfNeeded("<!DOCTYPE html>");
                return super.getValueAsync();
            }
            throw err;
        }
    }

    // -------------------------------------------------------------
    // Parsing pipeline
    // -------------------------------------------------------------
    #parse(obj) {
        if (!obj) return obj;

        // Detect JS variable assignment
        if (typeof obj === "string" && obj.trim().startsWith("var calib_rw_info")) {

            // Case 1: empty → login required
            if (obj.trim() === "var calib_rw_info = { };" ||
                obj.trim() === "var calib_rw_info = {};") {
                throw new Error("not_logged_in");
            }

            // Case 2: full data → parse it
            obj = this.#extractJsVariable(obj);
        }

        // If still a string → return raw
        if (typeof obj === "string") return obj;

        // Walk recursively
        const walk = value => {
            if (typeof value === "string") {
                const extracted = this.#extractMultiline(value);
                if (extracted !== value) {
                    return this.#parseSensorTextGrouped(extracted);
                }
                return value;
            }

            if (Array.isArray(value)) return value.map(walk);

            if (value && typeof value === "object") {
                const out = {};
                for (const k in value) out[k] = walk(value[k]);
                return out;
            }

            return value;
        };

        return walk(obj);
    }

    // -------------------------------------------------------------
    // JS variable parser
    // -------------------------------------------------------------
    #extractJsVariable(str) {
        const match = str.match(/var\s+calib_rw_info\s*=\s*(\{[\s\S]*\});?/);
        if (!match) return str;

        try {
            let json = match[1];

            // Convert JS object → JSON
            json = json
                .replace(/([a-zA-Z0-9_]+)\s*:/g, '"$1":')  // quote keys
                .replace(/'/g, '"');                      // single → double quotes

            return JSON.parse(json);

        } catch (e) {
            console.warn("Failed to parse JS variable:", e);
            return str;
        }
    }

    // -------------------------------------------------------------
    // Multiline extraction
    // -------------------------------------------------------------
    #extractMultiline(str) {
        const match = str.match(/function\s*\(\)\s*\{\/\*([\s\S]*?)\*\/\}/);
        if (!match) return str;

        let text = match[1];
        text = text.replace(/^<pre>/, "").replace(/<\/pre>$/, "");
        return text.trim();
    }

    // -------------------------------------------------------------
    // Sensor text parser
    // -------------------------------------------------------------
    #parseSensorTextGrouped(text) {
        const lines = text.split(/\r?\n/).map(l => l.trimEnd());

        const sections = {};
        let currentSection = "General";

        const sectionHeaderRegex = /^(.+):\s*$/;
        const entryRegex = /^(.+?)\s*\(([^)]+)\):\s*([+-]?\d+(?:\.\d+)?)\s*(.*)$/;

        for (let raw of lines) {
            const line = raw.trim();
            if (!line) continue;

            const sec = line.match(sectionHeaderRegex);
            if (sec) {
                currentSection = sec[1].trim();
                if (!sections[currentSection]) sections[currentSection] = [];
                continue;
            }

            const m = line.match(entryRegex);
            if (m) {
                const [, name, address, value, unit] = m;

                if (!sections[currentSection]) {
                    sections[currentSection] = [];
                }

                sections[currentSection].push({
                    name: name.trim(),
                    address: address.trim(),
                    value: Number(value),
                    unit: unit.trim()
                });

                continue;
            }
        }

        return sections;
    }
}

const calibRwInfo = new CalibRwInfo();
export default calibRwInfo;
/* RETURNED DATA EXAMPLE:
calib_ro_info = { bias: {
							title: 'On-board sensors:', 
							class: 'biasinfo', 
							visible: 1, 
							power: { value: '489', unit: 'mV' }, 
							refl_power: { value: '203', unit: 'mV' }, 
							temperature: '+37.6', last:1}, 
							epsu: {
									title: 'External PSU monitor:', 
									class: 'psuinfo', 
									visible: 0, 
									data: [{ name: 'HPBM1', data: {text: (function () {/*<pre>
On-board sensors:
       FP_DET (0x00):     0.0 mV
       RP_DET (0x01):     0.0 mV

  PWR_ENTRY_1_MON (100): 25.47 V
    Current_mon_1 (101): 3.15 A

  PWR_ENTRY_2_MON (102): 25.40 V
    Current_mon_2 (103): 3.55 A

  PWR_ENTRY_3_MON (104): 25.24 V
    Current_mon_3 (105): 2.21 A

  PWR_ENTRY_4_MON (106): 25.28 V
    Current_mon_4 (107): 2.14 A

  PWR_ENTRY_5_MON (108): 11.49 V
    Current_mon_5 (109): 1.88 A

  PWR_ENTRY_6_MON (110): 0.18 V
    Current_mon_6 (111): 0.01 A

          -5V_MON (112): -4.97 V
      DAC_REF_mon (113): 3.02 V

      TEMP_SENSE1 (114): 29.75 degC
      TEMP_SENSE2 (115): 31.75 degC
    TMON_MEAS_INT (116): 32.75 degC
      Temp_S_Filt (117): 0.01 V (7.20 degC)
  TSENSE_CONV_RES (118): 27.00 degC
*/
//</pre>*/}).toString().match(/[^]*\/\*([^]*)\*\/\}$/)[1] }},{}] }, last: 1 };
