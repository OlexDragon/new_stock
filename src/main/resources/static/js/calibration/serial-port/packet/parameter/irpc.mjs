import {parseToInt, parseToIntArray} from '../service/converter.mjs'
import Parameter  from "./parameters.mjs";

export default class ControlIrpc extends Parameter{

	constructor(){
		super(redundancy, 'Control IRPC');
	}

	get all(){
		const {['Standby Mode']:standbyMode, Status, Switchover, ['Switchover Mode']:switchoverMode} = this.parameters;
		return {standbyMode, Status, Switchover, switchoverMode};
	}
}

const redundancy = {};
redundancy['Switchover Mode'] = {};
redundancy['Switchover Mode'].code = 1;
redundancy['Switchover Mode'].parser = parseSwitchoverMode;

redundancy['Standby Mode'] = {};
redundancy['Standby Mode'].code = 2;
redundancy['Standby Mode'].parser = bytes=>redundancyMode[bytes[0]&1]

redundancy['Status'] = {};
redundancy['Status'].code = 3;
redundancy['Status'].parser = parseStatus;

redundancy['Switchover'] = {};
redundancy['Switchover'].code = 4;
redundancy['Switchover'].parser = parseToInt;

const switchoverMode = redundancy['Switchover Mode'].code;
const standbyMode	 = redundancy['Standby Mode'].code;
const status		 = redundancy['Status'].code;
const switchover	 = redundancy['Switchover'].code;

// Bits masck
const statusBitMask = {
				['SW1 Ready']: 1,
				['SW2 Ready']: 2,
				['Redundancy']: 12,
				['Switchover Mode']: 240,
				['Standby Mode']: 3840};

const statusKeys = Object.keys(statusBitMask);

const flagsBitMask = {
				['Operational']: 1,
				['Connected']: 2,
				['Switchover Alarm']: 4,
				RESERVED1: 8,
				['Status']: 112,
				RESERVED2: 128,
				['BUC Alarm']: 1792,
				RESERVED3: 2048};
const flagsKeys = Object.keys(flagsBitMask);

const ready				 = ['UNKNOWN', 'Ready', 'Warning', 'Alarm'];
const switchoverModes	 = ['NONE', 'Automatic', 'Manual'];
const standbyModes		 = ['NONE', 'Hot', 'Cold']
const redundancyMode	 = ['Cold Standby', 'Hot Standby']
const unitStatusNames	 = ['Unknown', 'Standalone', 'Online', 'Standby', 'Protection A', 'Protection B', 'Unknown'];
const alarmSeverityNames = ['No alarm', 'Indeterminate', 'Warning', 'Minor', 'Major', 'Critical', 'Unknown']

function parseStatusFlags(value){

	const status = {};
	Object.keys(statusBitMask).forEach(key=>{
		switch(key){
		case statusKeys[0]:	// SW1 Ready
		case statusKeys[1]:	// SW2 Ready
			status[key] = value & statusBitMask[key] ? 'Yes' : 'No';
			break;

		case statusKeys[2]: // Redundancy
			{
				const index = (value&statusBitMask[key])>>2;
				status[key] = ready[index];
			}
			break;

		case statusKeys[3]:	// Switchover Mode
			{
				const index = (value&statusBitMask[key])>>4;
				status[key] = switchoverModes[index];
			}
			break;

		case statusKeys[4]:	// Standby Mode
			{
				const index = (value&statusBitMask[key])>>8;
				status[key] = standbyModes[index];
			}
		}
	});
	return status;
}

function parseFlags(flags){
	const f = {};
	Object.keys(flagsBitMask).forEach(key=>{
		switch(key){
		case flagsKeys[0]:	// 'OPERATIONAL':
		case flagsKeys[1]:	// 'CONNECTED':
		case flagsKeys[2]:	// 'SWITCHOVER_ALARM':
			f[key] = flags & flagsBitMask[key] ? 'Yes' : 'No';
			break;

		case flagsKeys[4]:	// 'STATUS':
			f[key] = unitStatusNames[((flags&flagsBitMask[key])>>4)]
			break;

		case flagsKeys[6]:	// 'BUC_ALARM':
			f[key] = alarmSeverityNames[((flags&flagsBitMask[key])>>8)]
		}
	});
	return f;
}

function parseStatus(bytes){
	const statuses = {};
	statuses.status = parseStatusFlags( parseToInt(bytes.splice(0,4)));
	statuses.bucStatus = {};
	while(bytes.length){
		const name = spliceString(bytes);
		const [id, linkId, bucId] =  parseToIntArray(bytes.splice(0, 12));
		const s = {status, id, linkId, bucId};
		s.status = parseFlags(parseToInt(bytes.splice(0, 4)));
		statuses.bucStatus[name] = s;
	}

	return statuses;
}

function spliceString(bytes){
	const length = bytes.indexOf(0);
	const r = String.fromCharCode.apply(null, bytes.splice(0, length));
	bytes.splice(0, 1);
	return r;
}

function parseSwitchoverMode(bytes){
	if(bytes?.length)
		return switchoverModes[bytes[0]];
}

export function code(name){
	if(name===undefined || Array.isArray(name)){
		console.warn('Parameter "name" is missing or wrong', name)
		return;
	}

	if(typeof name === 'number')
		if(name>=0 && name<redundancy.length)
			return  name;
		else
			throw new Error('Wrong index - ' + name);

	const index = redundancy.indexOf(name);
	if(index<0)
		throw new Error('Wrong mane - ' + name);

	return index
}

export function name(code){

	if(typeof code === 'string')
		if(redundancy.includes(code))
			return code;
		else
			throw new Error('Wrong mane - ' + code);

	if(code<0 || code>=redundancy.length)
		throw new Error('Wrong index - ' + name);
	else
		return redundancy[code];
}

