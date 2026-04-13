import {parseToInt} from '../service/converter.mjs'

const protocol = {address: {code: 3, parser:b=>b}, baudrate: {code: 4, parser:parseToInt}, retransmit: {code: 5, parser:b=>b}, tranceiver_mode: {code: 6, parser:b=>b}}

Object.freeze(protocol);

export default protocol;

export function code(name){

	return protocol[name].code;
}

export function name(code){
	const keys = Object.keys(protocol);
	for(const key of keys)
		if(protocol[key].code===code)
			return key;
}

export function toString(value){
	if(typeof value === 'number'){
		const n = name(value)
		return `protocol: ${n} (${value})`;
	}else{
		const c = code(value)
		return `protocol: ${value} (${c})`;
	}
}

export function parser(code){
	const n = name(code)
	return protocol[n].parser;
}
