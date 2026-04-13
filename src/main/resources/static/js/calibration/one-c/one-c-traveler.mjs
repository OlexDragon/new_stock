import {serialNumber} from './one-c.mjs';

export const sections = ['bias-and-controller', 'shelf-controller', 'devices', 'converter', 'reference-clock-module', 'input-module', 'converter-tuning', 'unit-tuning-imd-3'];

export function get1CTravelerSection(section, profileSN){		// searchByProfile - profile serial number
	return $.get('/calibration/rest/one-c/profile', {sn: profileSN ?? serialNumber, section: section, byProfile: profileSN ? true : false});	// 192.168.20.241/irt-prod-web/hs/api/travelers?sn=2550008&section=devices
}

export function post1CTravelerSection(sectionObj, section){	
	const json = {serialNumber: serialNumber,  section: section, body: JSON.stringify(sectionObj)};
	return $.ajax({
		url: '/calibration/rest/one-c/profile/save',
		type: 'POST',
		contentType: "application/json",
		data: JSON.stringify(json),
		dataType: 'json'
    });
}											//