let info;

/**
 * Fetches home page information from the server. If 'update' is false and cached info is available, it returns the cached info. Otherwise, it fetches new data from the server.
 * @param {boolean} update - If true, forces fetching new data from the server; if false, returns cached info if available.
 */
export default async function alarmsInfo(update = false) {	
	if(!update && info)
		return info;
	return info = await $.get('/calibration/rest/alarms_info', {sn: originalSN});	// originalSN is set in calibration.html by the thymeleaf
}
/* Example response from the server:
[
    {
        "desc": "Under Current",
        "status": "no alarm"
    },
    {
        "desc": "Over-temperature",
        "status": "no alarm"
    },
    {
        "desc": "Hardware",
        "status": "no alarm"
    },
    {
        "desc": "Redundancy",
        "status": "no alarm"
    },
    {
        "desc": "RF overdrive",
        "status": "no alarm"
    },
    {
        "desc": "Reflected power",
        "status": "no alarm"
    },
    {
        "desc": "HPBM1 Over-current",
        "status": "no alarm"
    },
    {
        "desc": "Test Alarm",
        "status": "no alarm"
    }
]
*/