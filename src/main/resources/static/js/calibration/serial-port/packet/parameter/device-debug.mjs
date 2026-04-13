import {parseToIntArray, parseToBoolean, parseToString} from '../service/converter.mjs'

const deviceDebug = {};

deviceDebug.debugInfo = {};
deviceDebug.debugInfo.code = 1;		/* device information: parts, firmware and etc. */
deviceDebug.debugInfo.parser = parseToString;

deviceDebug.debugDump = {};
deviceDebug.debugDump.code = 2;		/* dump of registers for specified device index */
deviceDebug.debugDump.parser = parseToString;

deviceDebug.readWrite = {};
deviceDebug.readWrite.code = 3;		/* registers read/write operations */
deviceDebug.readWrite.parser = parseToIntArray;

deviceDebug.index = {}; 
deviceDebug.index.code		= 4;		/* device index information print */
deviceDebug.index.parser = parseToIntArray;

deviceDebug.calibrationMode = {};
deviceDebug.calibrationMode.code = 5;	/* calibration mode */
deviceDebug.calibrationMode.parser = parseToBoolean;

deviceDebug.environmentIo = {};
deviceDebug.environmentIo.code = 10;	/* operations with environment variables */
deviceDebug.environmentIo.parser = parseToIntArray;

deviceDebug.devices = {};
deviceDebug.devices.code	= 30;
deviceDebug.devices.parser = parseToIntArray;

export default Object.freeze(deviceDebug);

const deviceDebugNames = Object.keys(deviceDebug).reduce((a,k)=>{

		a[deviceDebug[k].code] = k;
		return a;
	}, []
);

Object.freeze(deviceDebugNames);

export function code(name){
	if(typeof name === 'number')
		return name;

	return deviceDebug[name];
}

export function name(code){
	if(typeof code === 'string')
		return deviceDebugNames.includes(code) ? code : undefined;

	return deviceDebugNames[code];
}

export function parser(code){
	if(!code)
		return parseToIntArray;
	const n = name(code);
	return deviceDebug[n].parser;
}

export function toString(code){
	return `${name(code)} (${code});`
}
