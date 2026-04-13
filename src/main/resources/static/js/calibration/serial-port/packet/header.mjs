import packetType, { name } from './packet-properties/packet-type.mjs'
import { id, toString } from './packet-properties/group-id.mjs'
import { PACKET_ERROR } from './error.mjs'
import { shortToBytes } from './service/converter.mjs'
import { toString as idToString } from './packet-properties/packet-id.mjs'

export const HEADER_SIZE = 7;
export const ACKNOWLEDGEMENT_HEADER_SIZE = 3; // 3 bytes - packet type and packet ID

export default class Header{
	constructor(type, packetId, groupId, errorCode){
		// From bytes
		if(Array.isArray(type)){
			const bytes = type;
			if(bytes.length<ACKNOWLEDGEMENT_HEADER_SIZE){
				console.warn('Unable to create Header. Very few bytes.', bytes);
				return;
			}
			this.type 		= bytes[0]&0xff;						// byte	type;		0
			this.packetId 	= (bytes[2]&0xff) * 256 + (bytes[1]&0xff);		// short packetId;	1,2 

			if(bytes.length>=HEADER_SIZE && this.type != packetType.acknowledgement){
				this.groupId 	= bytes[3]&0xff;											// byte groupId;	3
				this.reserved	= 0;														// short reserved;	4,5
				this.error		= (packetId == undefined ? bytes[6]&0xff : packetId);		// byte errorCode;	6
			}
			return;	
		}

		this.type 		= (type == undefined ? packetType.request : type);								// byte	type;		0
		this.packetId 	= (packetId == undefined ? Math.floor(Math.random() * 32767 ) : packetId);		// short packetId;	1,2

		if(this.type==packetType.acknowledgement)
			return;

		this.groupId 	= (groupId == undefined ? id('deviceInfo') : (typeof groupId == "number") ? groupId : undefined);	// byte groupId;	3; 
		this.reserved	= 0;															// short reserved;	4,5
		this.error		= ((typeof groupId === 'string') ? groupId : errorCode === undefined ? 0 : errorCode);						// byte errorCode;	6
	}
	toBytes(){
		const pid = shortToBytes(this.packetId);
		if(this.type == packetType.acknowledgement)
			return [this.type, pid[0], pid[1]];
		const reserved = shortToBytes(this.reserved);
		return [this.type, pid[0], pid[1], this.groupId, reserved[0], reserved[1], this.error];
	}
	toBytesAcknowledgement(){
		const id = shortToBytes(this.packetId);
		return [this.type, id[0], id[1]];
	}
	toString(){
		if(this.type == packetType.acknowledgement)
			return 'type = ' + name(this.type) + ', ID = ' + this.packetId;

		const grId = toString(this.groupId);
		return 'type = ' + name(this.type) + ', ID = ' + idToString(this.packetId) + ', groupId = ' + (grId ? grId : this.groupId) + ', error = ' + ((typeof this.error !== "number") ? this.error : PACKET_ERROR[this.error]);
	}
}
