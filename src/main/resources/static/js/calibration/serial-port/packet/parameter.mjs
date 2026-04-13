//import {toString} from './packet-properties/parameter-code.js'
import {shortToBytes} from './service/converter.mjs'

const PARAMETER_ALL			= 255;
export const PARAMETER_SIZE= 3;

export default class Parameter{

	constructor(code, size){
		// From bytes
		if(Array.isArray(code)){
			this.code = code[0];
			this.size = code[1] * 256 + code[2];
			return;
		}
		this.code = (code == undefined ? PARAMETER_ALL : code);
		this.size = (size == undefined ? 0 : size);
	}

	toBytes(){
		const sizeBytes = shortToBytes(this.size);
		return [this.code, sizeBytes[1], sizeBytes[0]];
	}

	toString(packetGroupId){

//		let str;
//
//		if(packetGroupId){
//			let tmp = toString(packetGroupId);
//			if(tmp){
//				tmp = tmp(this.code);
//
//				if(tmp)
//					str = tmp;
//				else
//					str = 'code: ' + this.code;
//			}else
//				str = 'code: ' + this.code;
//		}else
//			str = 'code: ' + this.code;
//
//		return str + ', size: ' + this.size;
	}
}

//const PARAMETER_READ_WRITE	= deviceDebug.parameter.readWrite ;
//const DUMP_REGISTERS	= deviceDebug.parameter.debugDump ;
