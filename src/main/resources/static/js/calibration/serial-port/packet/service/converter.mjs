import {status as alarmStatus} from '../parameter/value/alarm-status.mjs'
import IrtValue from '../parameter/value/irt-value.mjs'

export function shortToBytes(val, reverse){

	if(reverse)
		return val ? numberToBytes(val, 2) : [0, 0];

		const bytes = [0,0];
	for ( let index = 0; index < bytes.length; index++ ) {
        let byte = val & 0xff;
        bytes[index] = byte;
        val = (val - byte) / 256 ;
    }
    return bytes;
}

export function shortToBytesR(val){
    return shortToBytes(val, true);
}

export function intToBytes(val){
	return numberToBytes(val, 4);
}

export function intArratToBytes(...val){
	const result = []
	val.forEach(v=>result.push(intToBytes(v)));
	return result.flat();
}

export function longToBytes(val){
	return numberToBytes(val, 8);
}

export function numberToBytes(val, minBytes){
	let hex = val.toString(16);
	const hexArray = [];
	while(hex.length){
		const start = hex.length-2;
		if(start>=0){
			const substring = hex.substring(start, start+2);
			hexArray.push(substring);
			hex = hex.substring(0, start);
		}else{
			hexArray.push(hex);
			hex = '';
		}
	}
	const bytes = [];
	hexArray.forEach(h=>bytes.unshift(parseInt(h, 16)));
	if(minBytes)
		while(minBytes>bytes.length)
			bytes.unshift(0);
    return bytes;
}

export function parseToString(bytes){
	const b = [...bytes]
	if(!b)
		return '';

	const last = b.length - 1;
	if(b[last]==0)
		b.splice(last, 1);
		
	return String.fromCharCode.apply(String, b);
}

export function parseToInt(bytes, unsigned) {

	let index = bytes.length-1;
	let intValue = !unsigned && index==0 ? bytes[index]<<24>>24 : bytes[index]&0xff;

	for(let i=1; index>0 && i<bytes.length;i++){
		const shift = i*8;
		let v

		if(unsigned)
			v = bytes[--index]&0xff;
		else if(index==1)
			v = bytes[--index]<<24>>24;
		else
			v = bytes[--index];

		intValue |= v<<shift;
	}
	return intValue;
};

function byteToHex(b){
	return (b + 0x100).toString(16).substr(-2).toUpperCase();
}
function bytesToHexString(bytes){
	return bytes.map(byteToHex).join('');
}

export function parseToBigInt(bytes){
	return BigInt('0x' + bytesToHexString(bytes));
}

export function parseToBigIntArray(bytes){
	const ints = [];
	const b = [...bytes];
	//	for(let i=0; b.length && i<3; i++){
	for(let i=0; b.length; i++){
		const fourBigInt = b.splice(0, 8);
		ints.push(parseToBigInt(fourBigInt));
	}
return ints;
}

export function parseToIntUnsigned(bytes) {
	return parseToInt(bytes, true);
};

export function parseToIntArray(bytes){
	return parseToArray(bytes, 4);
}

export function parseToIntSequence(bytes){
	return parseToArray(bytes, 4).join('.');
}

export function parseToShortArray(bytes){
	return parseToArray(bytes, 2);
}

function parseToArray(bytes, size){
	const ints = [];
	const b = [...bytes];
	for(let i=0; b.length; i++){
		const fourBytes = b.splice(0, size);
		ints.push(parseToInt(fourBytes));
	}
	return ints;
}
export function parseToBoolean(bytes){
	if(!bytes?.length)
		return '';
	return bytes[bytes.length-1]>0;
}

const prefixes = [, '', '<', '>', 'N/A']
export function parseToIrtValue(bytes, divider, postfix){

	if(!bytes?.length)
		return new IrtValue();

	let prefix;
	if(bytes.length===3){
		const index = bytes.splice(0,1)&7;
		if(index===0)
			return new IrtValue('UNDEFINED');
		prefix = prefixes[index];
		if(index===4)
			return new IrtValue(prefix);
	}
	return new IrtValue(parseToInt(bytes), prefix, postfix, divider);
}

export function parseToLoFrequency(bytes){
	const b = [...bytes]
	const lo = [];
	while(b.length){
		const index = b.splice(0,1)&0xff;
		let value
		if(b.length)
			value = (parseToBigInt(b.splice(0,8))/1000000n) + ' MHz';
		lo[index] = value;
	}
	return lo;
}

export function parseToFreqyency(bytes){
	const b = [...bytes]
	return parseToBigInt(b)/1000000n;
}

const
	MINUTE = 60,
	HOUR	= 60*MINUTE,
	DAY		= 24*HOUR;
	
export function parseToTimeStr(bytes){
	const time = parseToInt(bytes);
	const days = Math.floor(time / DAY);
	const hours = Math.floor(time%DAY / HOUR);
	const minutes = Math.floor(time%HOUR / MINUTE);
	const sec = time%MINUTE;
	return [days, hours, minutes, sec].map(t=>t.toString().padStart(2,'0')).join(':');
}
const statusBits = {};
statusBits.buc = {};
statusBits.buc.mute = {};
statusBits.buc.mute.value = 1;
statusBits.buc.mute.bitmask = 1;
//statusBits.buc.pll_unknown = {};
//statusBits.buc.pll_unknown.value = 0;
//statusBits.buc.pll_unknown.bitmask = 6;
statusBits.buc.locked = {};
statusBits.buc.locked.value = 2;
statusBits.buc.locked.bitmask = 6;
statusBits.buc.unlocked = {};
statusBits.buc.unlocked.value = 4;
statusBits.buc.unlocked.bitmask = 6;
statusBits.buc.internal = {};
statusBits.buc.internal.value = 16;
statusBits.buc.internal.bitmask = 16;


export function parseToStatus(value, type){
	let status;

	switch(type){
	default:
		status = statusBits.buc;
	}

	const result = []
	const keys =Object.keys(status);
	const v = parseToInt(value);

	for(let key of keys)
		if((v&status[key].bitmask)==status[key].value)
			result.push(key);

	return result;
}

export function parseToAlarmStatus(bytes){
	return alarmStatus(bytes);
}
export function parseToAlarmString(bytes){
	const value = {};
	value.id = parseToInt(bytes.splice(0,2));
	value.string = parseToString(bytes);
	return value;
}
const capabilities = [undefined, 'Internal', 'External', 'Autosense'];
export function parseToCapabilities(bytes){
	const intVal = parseToInt(bytes);
	const result = [];
	for(let i=1; i<capabilities.length; i++){
		const bitmask = 1<<i;
		if(intVal & bitmask)
			result.push({[capabilities[i]]:i});
	}
	return result;
}

