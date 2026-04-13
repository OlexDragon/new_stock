import {parseToString, parseToTimeStr, parseToIntSequence} from '../service/converter.mjs'
import Payload from '../payload.mjs'

const deviceInfo = {};

// Parameter CODE
deviceInfo.type				 = 1;
deviceInfo.firmwareVersion	 = 2;
deviceInfo.firmwareBuild	 = 3;
deviceInfo.uptimeCounter	 = 4;
deviceInfo.serialNumber		 = 5;
deviceInfo.description		 = 6;
deviceInfo.partNumber		 = 7;
deviceInfo.all				 = 255;

const o = Object.freeze(structuredClone(deviceInfo));
export default o;

export function code(name){
	return deviceInfo[name];
}

export function name(code){
	const keys = Object.keys(deviceInfo);
	for(const key of keys){
		if(deviceInfo[key] == code)
			return key;
			}
}

// Show order
deviceInfo.sequence = {};
deviceInfo.sequence[deviceInfo.description]		 = 0;
deviceInfo.sequence[deviceInfo.serialNumber]	 = 1;
deviceInfo.sequence[deviceInfo.partNumber]		 = 2;
deviceInfo.sequence[deviceInfo.type]			 = 3;
deviceInfo.sequence[deviceInfo.firmwareVersion]	 = 4;
deviceInfo.sequence[deviceInfo.firmwareBuild]	 = 5;
deviceInfo.sequence[deviceInfo.uptimeCounter]	 = 6;

export function order(name){
	const code = typeof name === 'number' ? name : deviceInfo[name];
	return deviceInfo.sequence[code]
}
export function comparator(index1, index2){
	
	if(index1 instanceof Payload){
		index1 = index1.parameter.code;
		index2 = index2.parameter.code;
	}
	return deviceInfo.sequence[index1] - deviceInfo.sequence[index2]
}

// Description
deviceInfo.string = {};
deviceInfo.string[deviceInfo.description]	 = 'Description';
deviceInfo.string[deviceInfo.serialNumber]	 = 'Serial Number';
deviceInfo.string[deviceInfo.partNumber]	 = 'Part Number';
deviceInfo.string[deviceInfo.type]			 = 'Type';
deviceInfo.string[deviceInfo.firmwareVersion]= 'FW Version';
deviceInfo.string[deviceInfo.firmwareBuild]	 = 'FW Build';
deviceInfo.string[deviceInfo.uptimeCounter]	 = 'Counter';

export function description(value){

	if(typeof value === 'number')
		return deviceInfo.string[value];

	else{

		const code = code(value);
		return deviceInfo.string[code];
	}
}

export function toString(value){

	if(typeof value === 'number'){

		const name = name(value);
		return `deviceInfo: ${name} (${value})`;

	}else{

		const code = code(value);
		return `deviceInfo: ${value} (${code})`;
	}
}

// Device Info parse functions
deviceInfo.parse = {};
deviceInfo.parse[deviceInfo.description]	 = parseToString;
deviceInfo.parse[deviceInfo.serialNumber]	 = parseToString;
deviceInfo.parse[deviceInfo.partNumber]		 = parseToString;
deviceInfo.parse[deviceInfo.type]			 = parseToIntSequence;
deviceInfo.parse[deviceInfo.firmwareVersion]= parseToString;
deviceInfo.parse[deviceInfo.firmwareBuild]	 = parseToString;
deviceInfo.parse[deviceInfo.uptimeCounter]	 = parseToTimeStr;

export function parser(value){

	let parser;
	if(typeof value === 'number')
		parser = deviceInfo.parse[value]

	else{

		const code = code(value);
		parser = deviceInfo.parse[code]
	}

	return parser ? parser : value.toString();
}

