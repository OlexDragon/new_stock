import Payload from '../payload.mjs'
import Parameter from '../parameter.mjs'
import {parseToAlarmStatus, parseToAlarmString, parseToShortArray, shortToBytes} from '../service/converter.mjs'

const alarms = Object.freeze([

	 'none',
	 'number of alarms',
	 'IDs',
	 'summary status',
	 'config',
	 'status',
	 'description',
	 'name']);

const parsers = Object.freeze([

	 ,
	  bytes=>bytes[0],
	  parseToShortArray,
	  parseToAlarmStatus,
	  ,
	  parseToAlarmStatus,
	  parseToAlarmString,
	  parseToAlarmString ]);

const alarmDescription = Object.freeze([
	 'none - Not Implemented',
	 'number of alarms',
	 'IDs - Presented alarm codes.',
	 'summary status',
	  'config - I don\'t know.',
	  'status - Alarm status.',
	  'description',
	  'name']);

export const alarmCode = alarms.reduce((a,k,i)=>{

	  		a[k] = i;
	  		return a;
	  	}, []
	  );

export default class Alarm{
	constructor(pl){
		this.code = code(pl.parameter.code);
		this.name = name(this.code);
		this.description = description(this.code);
		this.parser = parser(this.code);
	}
}
export function code(name){
	if(typeof name === 'number')
		return name;
	return alarms.indexOf(name);
}

export function name(code){
	return alarms[code];
}

export function description(value){
	const c = code(value)
	return alarmDescription[c];
}

export function toString(value){
	const c = code(value)
	const name = name(value)
	return `alarm: ${name} (${c})`;
}

export function parser(value){
	const c = code(value)
	return parsers[c];
}

export function payload(id, value){
	const c = code(id)
	const parameter = new Parameter(c);
	const bytes = shortToBytes(value);
	const array = [bytes[1], bytes[0]];
	return new Payload(parameter, array);
}

export function payloads(ids, withName){
	const array = [];
	ids.forEach(id=>{
		if(withName){
//			array.push(payload(code('name'), id));
			array.push(payload(code('description'), id));
		}else
			array.push(payload(code('status'), id));
	})
	return array;
}
