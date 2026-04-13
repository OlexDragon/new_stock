let info;

/**
 * Fetches home page information from the server. If 'update' is false and cached info is available, it returns the cached info. Otherwise, it fetches new data from the server.
 * @param {boolean} update - If true, forces fetching new data from the server; if false, returns cached info if available.
 */
export default async function homePageInfo(timeout = 200, update){	
	if(!update && info)
		return info;
	return info = await $.get('/calibration/rest/home-page-info', {sn: originalSN, timeout: timeout});	// originalSN is set in calibration.html by the thymeleaf
}