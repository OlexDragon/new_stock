import {parseToInt} from '../../service/converter.mjs'

class AlarmStatus{
	constructor(index, severities, text, boorstrapClass){
		this.index = index;
		this.severities = severities;
		this.text = text;
		this.boorstrapClass = boorstrapClass;
	}
}

const statuses = [];	// Do not change the order of these statuses
statuses.push(new AlarmStatus(statuses.length,'NO_ALARM', 'No Alarm', 'text-bg-success'));
statuses.push(new AlarmStatus(statuses.length,'INFO'	, 'No Alarm', 'text-bg-success'));
statuses.push(new AlarmStatus(statuses.length,'WARNING'	, 'Warning'	, 'text-bg-warning'));
statuses.push(new AlarmStatus(statuses.length,'MINOR'	, 'Warning'	, 'text-bg-warning'));
statuses.push(new AlarmStatus(statuses.length,'MAJOR'	, 'Alarm'	, 'text-bg-danger'));
statuses.push(new AlarmStatus(statuses.length,'CRITICAL', 'Alarm'	, 'text-bg-danger'));
statuses.push(new AlarmStatus(statuses.length,'SP Error', undefined	, 'text-bg-danger'));
statuses.push(new AlarmStatus(statuses.length,'Closed'	, 	'This program has been closed. \nDouble-click the jar file to open it again.', undefined));
statuses.push(new AlarmStatus(statuses.length,'TIMEOUT', undefined, undefined));
statuses.push(new AlarmStatus(statuses.length,'NC', 'No Connection', undefined));
statuses.push(new AlarmStatus(statuses.length,'UA Error', 'Invalid unit address. The unit address value can be between 0 and 254 inclusive.', 'text-bg-warning'));
statuses.push(new AlarmStatus(statuses.length,'The port is locked.', undefined, undefined));

export function status(bytes){

	if(Array.isArray(bytes))
		return parseStatus(bytes);

	const split = bytes.split(':');
	const s = statuses.filter(s=>s.severities===split[0]);
	
	if(s.length){
		const as = s[0];
		if(split.length>1)
			as.text = split[1];
		return s[0];
	}
		

	const status = new AlarmStatus();
	status.index = -1;
	status.severities = split[1] ? split[0] : 'UNKNOWN';
	status.text = split[1] ? split[1] : split[0];
	return status;
}

function parseStatus(bytes){
	const id = parseToInt(bytes.splice(0,2));
	const index = parseToInt(bytes)&7;
	const s = index<statuses.length && statuses[index]; 
	s.id = id;
	return s;
}
