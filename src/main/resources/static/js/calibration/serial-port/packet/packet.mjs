import Header, {HEADER_SIZE, ACKNOWLEDGEMENT_HEADER_SIZE} from './header.mjs'
import Payload from './payload.mjs'
import LinkHeader, {LINK_HEADER_SIZE} from './link-header.mjs'
import Parameter, {PARAMETER_SIZE} from './parameter.mjs'
import packetType from './packet-properties/packet-type.mjs'
import {checksumToBytes} from './service/checksum.mjs'

export {LinkHeader, Header, Payload, Parameter}

const FLAG_SEQUENCE	= 0x7E;
const CONTROL_ESCAPE = 0x7D;
//const LINK_HEADER_SIZE = 4;
//const HEADER_SIZE = 7;
//const PAYLOAD_MIN_SIZE = PARAMETER_SIZE;
const ACKNOWLEDGEMENT_SIZE = 5; // 3 bytes - packet type and packet ID plus 2 byte checksum
//const ACKNOWLEDGEMENT_HEADER_SIZE = 4; // 3 bytes - packet type (0) and packet ID (1, 2)

// Default InfoPacket
export default class Packet{
	// Default constuctor converter INFO Packet
	constructor(header, payloads, unitAddr){
		// From bytes
		if(Array.isArray(header)){
			this.#parseBytes(header);
			return;
		}
		this.header = (header == undefined ? new Header() : header);
		if(this.header.type!=packetType.acknowledgement)
			if(!payloads)
				this.payloads = [new Payload()];
			else if(typeof payloads =='number')
				this.payloads = [new Payload(payloads)] ;
			else if(Array.isArray(payloads))
				this.payloads = payloads;
			else
				this.payloads = [payloads];

		if(unitAddr)
			this.linkHeader = new LinkHeader(unitAddr);

//		console.log(this);
	}
	#parseBytes(array, hasLinkHeader){
		if(array.length<ACKNOWLEDGEMENT_SIZE){
			console.warn('Unable to create packet. Very few bytes.', array);
			return;
		}
		const bytes = this.#findLastPacket(array);
//		console.log(array, bytes);
		const packetArray = bytes.splice(0,bytes.length-2);
		const chcksm = checksumToBytes(packetArray);
		if(chcksm[0]==(bytes[0]&0xff) && chcksm[1]==(bytes[1]&0xff)){
			if(hasLinkHeader){	// Has Link Header
				const linkHeaderArray = packetArray.splice(0, LINK_HEADER_SIZE);
				this.linkHeader = new LinkHeader(linkHeaderArray);
			}
			const headerArray = packetArray.length==ACKNOWLEDGEMENT_HEADER_SIZE ? packetArray.splice(0) : packetArray.splice(0, HEADER_SIZE);
			this.header = new Header(headerArray);
			if(packetArray.length>=PARAMETER_SIZE)
				this.payloads = this.parsePayloads(packetArray);
			else if(packetArray.length)
				console.error('Byte parsing error.');
		}else{
			this.header = new Header(packetType.error, packetArray[2] * 256 + packetArray[1], 'The packet checksum is incorrect');
			console.warn('The packet checksum is incorrect; received: ' + packetArray[array.length-2] +',' + packetArray[array.length-1] + '; calculated: ' + chcksm + '; bytes: ' + bytes, 'header: ', header);
		}
	}
	getAcknowledgement(){
		const header = new Header(packetType.acknowledgement, this.header.packetId);
		return new Packet(header, undefined, this.linkHeader);
	}
	toBytesAcknowledgement(){
		return this.header.toBytesAcknowledgement();
	}
	parsePayloads(bytes){
		if(!bytes)
			return [];

		const pl = []
		while(bytes.length>0){
			const p = bytes.splice(0, PARAMETER_SIZE);
			const parameter = new Parameter(p);
			if (bytes.length < parameter.size) {
			    console.error("Payload size mismatch");
			    break;
			}
			const d = bytes.splice(0, parameter.size);
			const payload = new Payload(parameter,d);
			pl.push(payload);
		}
		return pl;
	}
	toBytes(){
		const linkHeaderBytes = this.linkHeader?.toBytes();
		const headerBytes = this.header.toBytes();
		const payloadBytes = this.payloadsToBytes();
		return [
		    ...(linkHeaderBytes ?? []),
		    ...headerBytes,
		    ...payloadBytes
		];
	}
	toSend(){
		return packetToSend(this);
	}
	payloadsToBytes(){

		let bytes = [];
		if(!this.payloads)
			return bytes;

		for (const pl of this.payloads)
		    bytes.push(...pl.toBytes());

		return bytes;
	}
	toString(){
		const linkHeader = this.linkHeader ? 'linkHeader: ' + this.linkHeader.toString() + ', ' : '';
		const plStr = this.payloads ? ', ' + this.payloads.map(pl=>pl.toString(this.header.groupId)) : '';
		return linkHeader + this.header.toString() + plStr;
	}
	getData(parameterCode){
		if(parameterCode)
			return this.payloads?.filter(pl=>(pl.parameter.code&0xff)==parameterCode).map(pl=>pl.getData(this.header.groupId));
		if(this.payloads?.length)
			return this.payloads[0].getData(this.header.groupId);
	}
	#findLastPacket(bytes){
		let lastIndex = bytes.lastIndexOf(FLAG_SEQUENCE);
		if(lastIndex<0)
			return byteStuffing(bytes);

		while(true){

			const nextIndex = bytes.lastIndexOf(FLAG_SEQUENCE, --lastIndex);

			if(nextIndex<0)
				return byteStuffing(bytes.splice(0, lastIndex));

			if (lastIndex-nextIndex < 3)
				lastIndex = nextIndex;
      		else
		        return byteStuffing(bytes.splice(nextIndex+1, lastIndex-nextIndex));
		}
	}
}

function byteStuffing(bytes){

	if(!bytes)
		return;

	let result = [];

	for(let i=0; i<bytes.length; i++){
		if(bytes[i]==CONTROL_ESCAPE){
			result.push(bytes[++i]^0x20);
		}else
			result.push(bytes[i]);
	}
	return result;
}
function controlEscape(bytes){
	let result = [];
	for(let i=0; i<bytes.length; i++){
		if(bytes[i]==FLAG_SEQUENCE || bytes[i]==CONTROL_ESCAPE){
			result.push(CONTROL_ESCAPE);
			result.push(bytes[i]^0x20);
		}else
			result.push(bytes[i]);
	}
	return result;
}

function packetToSend(packet){
	const bytes = packet.toBytes();
	const checksum = checksumToBytes(bytes);
	return [FLAG_SEQUENCE].concat(controlEscape(bytes.concat(checksum))).concat(FLAG_SEQUENCE);
}

//console.log(new Packet([126]))
