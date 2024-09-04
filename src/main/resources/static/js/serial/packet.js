// *** Checksum

const  fcstab = [
		0x0000, 0x1189, 0x2312, 0x329b, 0x4624, 0x57ad, 0x6536, 0x74bf,
		0x8c48, 0x9dc1, 0xaf5a, 0xbed3, 0xca6c, 0xdbe5, 0xe97e, 0xf8f7,
		0x1081, 0x0108, 0x3393, 0x221a, 0x56a5, 0x472c, 0x75b7, 0x643e,
		0x9cc9, 0x8d40, 0xbfdb, 0xae52, 0xdaed, 0xcb64, 0xf9ff, 0xe876,
		0x2102, 0x308b, 0x0210, 0x1399, 0x6726, 0x76af, 0x4434, 0x55bd,
		0xad4a, 0xbcc3, 0x8e58, 0x9fd1, 0xeb6e, 0xfae7, 0xc87c, 0xd9f5,
		0x3183, 0x200a, 0x1291, 0x0318, 0x77a7, 0x662e, 0x54b5, 0x453c,
		0xbdcb, 0xac42, 0x9ed9, 0x8f50, 0xfbef, 0xea66, 0xd8fd, 0xc974,
		0x4204, 0x538d, 0x6116, 0x709f, 0x0420, 0x15a9, 0x2732, 0x36bb,
		0xce4c, 0xdfc5, 0xed5e, 0xfcd7, 0x8868, 0x99e1, 0xab7a, 0xbaf3,
		0x5285, 0x430c, 0x7197, 0x601e, 0x14a1, 0x0528, 0x37b3, 0x263a,
		0xdecd, 0xcf44, 0xfddf, 0xec56, 0x98e9, 0x8960, 0xbbfb, 0xaa72,
		0x6306, 0x728f, 0x4014, 0x519d, 0x2522, 0x34ab, 0x0630, 0x17b9,
		0xef4e, 0xfec7, 0xcc5c, 0xddd5, 0xa96a, 0xb8e3, 0x8a78, 0x9bf1,
		0x7387, 0x620e, 0x5095, 0x411c, 0x35a3, 0x242a, 0x16b1, 0x0738,
		0xffcf, 0xee46, 0xdcdd, 0xcd54, 0xb9eb, 0xa862, 0x9af9, 0x8b70,
		0x8408, 0x9581, 0xa71a, 0xb693, 0xc22c, 0xd3a5, 0xe13e, 0xf0b7,
		0x0840, 0x19c9, 0x2b52, 0x3adb, 0x4e64, 0x5fed, 0x6d76, 0x7cff,
		0x9489, 0x8500, 0xb79b, 0xa612, 0xd2ad, 0xc324, 0xf1bf, 0xe036,
		0x18c1, 0x0948, 0x3bd3, 0x2a5a, 0x5ee5, 0x4f6c, 0x7df7, 0x6c7e,
		0xa50a, 0xb483, 0x8618, 0x9791, 0xe32e, 0xf2a7, 0xc03c, 0xd1b5,
		0x2942, 0x38cb, 0x0a50, 0x1bd9, 0x6f66, 0x7eef, 0x4c74, 0x5dfd,
		0xb58b, 0xa402, 0x9699, 0x8710, 0xf3af, 0xe226, 0xd0bd, 0xc134,
		0x39c3, 0x284a, 0x1ad1, 0x0b58, 0x7fe7, 0x6e6e, 0x5cf5, 0x4d7c,
		0xc60c, 0xd785, 0xe51e, 0xf497, 0x8028, 0x91a1, 0xa33a, 0xb2b3,
		0x4a44, 0x5bcd, 0x6956, 0x78df, 0x0c60, 0x1de9, 0x2f72, 0x3efb,
		0xd68d, 0xc704, 0xf59f, 0xe416, 0x90a9, 0x8120, 0xb3bb, 0xa232,
		0x5ac5, 0x4b4c, 0x79d7, 0x685e, 0x1ce1, 0x0d68, 0x3ff3, 0x2e7a,
		0xe70e, 0xf687, 0xc41c, 0xd595, 0xa12a, 0xb0a3, 0x8238, 0x93b1,
		0x6b46, 0x7acf, 0x4854, 0x59dd, 0x2d62, 0x3ceb, 0x0e70, 0x1ff9,
		0xf78f, 0xe606, 0xd49d, 0xc514, 0xb1ab, 0xa022, 0x92b9, 0x8330,
		0x7bc7, 0x6a4e, 0x58d5, 0x495c, 0x3de3, 0x2c6a, 0x1ef1, 0x0f78
	];
function getChecksum(bytes){
	let fcs = 0xFFFF;
	for(let i=0; i<bytes.length; i++){
		const index = (fcs^bytes[i]) & 0xff;
		fcs = (fcs>>8) ^ fcstab[index];
	}
	return fcs^0xFFFF;
}
function checksumToBytes(bytes){
	return shortToBytes(getChecksum(bytes));
}

// Packet Help functionf
function packetToSend(packet){
	const bytes = packet.toBytes();
	const checksum = checksumToBytes(bytes);
	return [FLAG_SEQUENCE].concat(controlEscape(bytes.concat(checksum))).concat(FLAG_SEQUENCE);
}
function shortToBytes(val){
	const bytes = [0,0];
	for ( let index = 0; index < bytes.length; index++ ) {
        let byte = val & 0xff;
        bytes[index] = byte;
        val = (val - byte) / 256 ;
    }
    return bytes;
}
function byteStuffing(bytes){

	if(!byteStuffing)
		return byteStuffing;

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
function parsePackets(bytes){

	if(!bytes){
		console.warn('Nothing to parse');
		return 0;
	}

	const indexes = [];
	for(let index=0; index<bytes.length;){
		index = bytes.indexOf(126, index);
		if(index<0)
			break;
		else
			indexes.push(index++);
	}
	if(indexes.length<2){
		console.warn("Wrong data to parse\n bytes: " + bytes + '\n indexes: ' + indexes);
		alert("Wrong data to parse");
		return 0;
	}
	const pairs = [];
	for(let i=0; i<indexes.length;){
		const first = ++indexes[i];
		const cout = indexes[++i]-first;
		if(first<cout){
			const pair= {};
			pair.first = first;
			pair.count = cout;
			pairs.push(pair);
			++i;
		}
	}
	const packets = [];
	for(const key in pairs){
		const b = [...bytes];
		const toSpliced = b.splice(pairs[key].first, pairs[key].count);
		const stuffing = byteStuffing(toSpliced);
		if(stuffing.length >= ACKNOWLEDGEMENT_SIZE){
			const packet = new Packet(stuffing);
			packets.push(packet);
		}else
			console.warn("Wrong array size - " + stuffing.toString());
		
	}

	return packets;
}
function parseToString(data){
	if(!data)
		return '';

	const last = data.length - 1;
	if(data[last]==0)
		data.splice(last, 1);
		
	return String.fromCharCode.apply(String, data);
}
function toIrtRegister(bytes, packetId){
	const packets = parsePackets(bytes);
	if(!packets || !packets.length){
		console.warn('Something went wrong.\n bytes: ' + bytes);
		alert('Something went wrong.');
		return 0;
	}
//	console.log(packets);
	//remove acknowledgement
	if(packets[0].header.type==packetType.acknowledgement)
		packets.shift();

	let index = -1;
	if(packetId){
		for(let i=0; i<packets.length; i++)
			if(packets[i].header.packetId == packetId){
				index = i;
				break;
			}
	}else
		index = 0;
	if(index<0 || !packets.length){
		console.warn('index=' + index + '; packets.length=' + packets.length + '; packetId=' + packetId + '; packets: ' + JSON.stringify(packets) + '\n bytes: ' + bytes);
		return 0;
	}
	const packet = packets[index];
	// Send Acknowledgement
	const acknowledgement = packet.getAcknowledgement();
	command = getCommand(acknowledgement);
	sendCommand(command, ()=>{});

	if(packet.header.error){
		f_stop();
		alert(PACKET_ERROR[packet.header.error]);
		return 0;
	}
	return packet.getData();
}
function parseIrtRegister(bytes){
	const register = [];
	const b = [...bytes];
	for(let i=0; i<3 && i<b.length; i++){
		const fourBytes = b.splice(0, 4);
		register.push(bytesToInt(fourBytes));
	}
	return new Register(...register);
}
const shiftSize = 8;
function bytesToInt(bytes){
	let index = bytes.length-1;
	let intValue = bytes[index];
	for(let i=1; index>0 && i<4;i++){
		const shift = i*shiftSize;
		const v = bytes[--index]&0xff;
		intValue |= v<<shift;
	}
	return intValue;
}
function intToBytes(intValue){
	if(typeof intValue === 'undefined') return;
    return [(intValue>>shiftSize*3) & 0xff, (intValue>>shiftSize*2) & 0xff, (intValue>>shiftSize) & 0xff, intValue & 0xff];
}

// *** Packet
const FLAG_SEQUENCE	= 0x7E;
const CONTROL_ESCAPE = 0x7D;
const HEADER_SIZE = 7;
const PARAMETER_SIZE= 3;
const PAYLOAD_MIN_SIZE = PARAMETER_SIZE;
const ACKNOWLEDGEMENT_SIZE = 5; // 3 bytes - packet type and packet ID plus 2 byte checksum
const ACKNOWLEDGEMENT_HEADER_SIZE = 3; // 3 bytes - packet type and packet ID 
const packetError = {};
packetError.noError					 = 0;
packetError.internalError			 = 1;
packetError.WriteError				 = 2;
packetError.functionNotImplemented	 = 3;
packetError.notInRange				 = 4;
packetError.canNotGenerate			 = 5;
packetError.canNotExecute			 = 6;
packetError.InvalidFormat			 = 7;
packetError.InvalidValue			 = 8;
packetError.noMemory				 = 9;
packetError.notFoundr				 = 10;
packetError.timedout				 = 11;
packetError.noCommunication			 = 20;
const PACKET_ERROR = {};
PACKET_ERROR[packetError.noError]		 = 'No Error';
PACKET_ERROR[packetError.internalError]	 = 'Internal System Error';
PACKET_ERROR[packetError.WriteError]	 = 'Write Error';
PACKET_ERROR[packetError.functionNotImplemented] = 'Function not implemented';
PACKET_ERROR[packetError.notInRange]	 = 'Value outside of valid range';
PACKET_ERROR[packetError.canNotGenerate] = 'Requested information can’t be generated';
PACKET_ERROR[packetError.canNotExecute]	 = 'Command can’t be executed';
PACKET_ERROR[packetError.InvalidFormat]	 = 'Invalid data format';
PACKET_ERROR[packetError.InvalidValue]	 = 'Invalid value';
PACKET_ERROR[packetError.noMemory]		 = 'Not enough memory';
PACKET_ERROR[packetError.notFoundr]		 = 'Requested element not foundr';
PACKET_ERROR[packetError.timedout]		 = 'Timed out';
PACKET_ERROR[packetError.noCommunication] = 'Communication problem';

// Packet type
const packetType = {};
packetType.spontaneous		= 0x0;			/* Spontaneous message, generated by device. */
packetType.response			= 0x1;			/* Response, generated as response to command or status request. */
packetType.request			= 0x2;			/* Status request. */
packetType.command			= 0x3;			/* Command. */
packetType.error			= 0xFE;			/* Error Packet. */
packetType.acknowledgement	= 0xFF;			/* Layer 2 acknowledgement. */
const PACKET_TYPE = {};
PACKET_TYPE[packetType.spontaneous]		 = 'spontaneous';		/* Spontaneous message, generated by device. */
PACKET_TYPE[packetType.response]		 = 'response';			/* Response, generated as response to command or status request. */
PACKET_TYPE[packetType.request]			 = 'request';			/* Status request. */
PACKET_TYPE[packetType.command]			 = 'command';			/* Command. */
PACKET_TYPE[packetType.error]			 = 'error';				/* Error Packet. */
PACKET_TYPE[packetType.acknowledgement]	 = 'acknowledgement';	/* Layer 2 acknowledgement. */

// Packet Group ID
const packetGroupId = {};
packetGroupId.alarm			 = 1;
packetGroupId.configuration	 = 2;
packetGroupId.filetransfer	 = 3;
packetGroupId.measurement	 = 4;
packetGroupId.reset			 = 5;
packetGroupId.deviceInfo	 = 8;
packetGroupId.control		 = 9;
packetGroupId.protocol		 = 10;
packetGroupId.network		 = 11;
packetGroupId.redundancy	 = 12;
packetGroupId.deviceDebug	 = 61;
packetGroupId.production	 = 100;
packetGroupId.developer		 = 120;
const PACKET_GROUP_ID = {};
PACKET_GROUP_ID[packetGroupId.alarm]		 = 'alarm';
PACKET_GROUP_ID[packetGroupId.configuration] = 'configuration';
PACKET_GROUP_ID[packetGroupId.filetransfer]	 = 'filetransfer';
PACKET_GROUP_ID[packetGroupId.measurement]	 = 'measurement';
PACKET_GROUP_ID[packetGroupId.reset]			 = 'reset';
PACKET_GROUP_ID[packetGroupId.deviceInfo]	 = 'device info';
PACKET_GROUP_ID[packetGroupId.control]		 = 'control';
PACKET_GROUP_ID[packetGroupId.protocol]		 = 'protocol';
PACKET_GROUP_ID[packetGroupId.network]		 = 'network';
PACKET_GROUP_ID[packetGroupId.redundancy]	 = 'redundancy';
PACKET_GROUP_ID[packetGroupId.deviceDebug]	 = 'device debug';
PACKET_GROUP_ID[packetGroupId.production]	 = 'production generic set 1';
PACKET_GROUP_ID[packetGroupId.developer]		 = 'developer generic set 1';
const deviceInfo = {};
deviceInfo.serialNumber	 = 5;
deviceInfo.description	 = 6;
const deviceDebug = {};
deviceDebug.parameter = {};
deviceDebug.parameter.debugInfo = 1;		/* device information: parts, firmware and etc. */
deviceDebug.parameter.debugDump = 2;		/* dump of registers for specified device index */
deviceDebug.parameter.readWrite = 3;		/* registers read/write operations */
deviceDebug.parameter.index		= 4;		/* device index information print */
deviceDebug.parameter.calibrationMode = 5;	/* calibration mode */
deviceDebug.parameter.environmentIo = 10;	/* operations with environment variables */
deviceDebug.parameter.devices	= 30;
const parameterCode = {};
parameterCode[packetGroupId.deviceInfo] = {};
parameterCode[packetGroupId.deviceInfo][deviceInfo.serialNumber] = {}
parameterCode[packetGroupId.deviceInfo][deviceInfo.serialNumber].description	 = 'Serial Number';
parameterCode[packetGroupId.deviceInfo][deviceInfo.serialNumber].parseFunction = parseToString; // Serial Number
parameterCode[packetGroupId.deviceInfo][deviceInfo.description] = {};
parameterCode[packetGroupId.deviceInfo][deviceInfo.description].description	 = 'Description'
parameterCode[packetGroupId.deviceInfo][deviceInfo.description].parseFunction = parseToString; // Description

parameterCode[packetGroupId.deviceDebug] = {};
parameterCode[packetGroupId.deviceDebug][deviceDebug.parameter.readWrite] = {};
parameterCode[packetGroupId.deviceDebug][deviceDebug.parameter.readWrite].description	 = 'Device Debug Register Read/Write'
parameterCode[packetGroupId.deviceDebug][deviceDebug.parameter.readWrite].parseFunction = parseIrtRegister; // IRT Register

// Default InfoPacket
class Packet{
	// Default constuctor converter INFO Packet
	constructor(header, payloads, unitAddr){
		// From bytes
		if(Array.isArray(header)){
			const bytes = header;
//			console.log(bytes);
			const packetArray = bytes.splice(0,bytes.length-2);
			const chcksm = checksumToBytes(packetArray);
			if(chcksm[0]==(bytes[0]&0xff) && chcksm[1]==(bytes[1]&0xff)){
				const headerArray = packetArray.length==ACKNOWLEDGEMENT_HEADER_SIZE ? packetArray.splice(0) : packetArray.splice(0, HEADER_SIZE);
				this.header = new Header(headerArray);
				if(packetArray.length>=PARAMETER_SIZE)
					this.payloads = this.parsePayloads(packetArray);
				else if(packetArray.length)
					console.error('Byte parsing error.');
			}else{
				this.header = new Header(packetType.error, packetArray[2] * 256 + packetArray[1], 'The packet checksum is incorrect');
				console.warn('The packet checksum is incorrect; received: ' + header[header.length-2] +',' + header[header.length-1] + '; calculated: ' + chcksm + '; bytes: ' + header);
			}
//			console.log(this);
			return;
		}
		this.header = (header == undefined ? new Header() : header);
		if(this.header.type!=packetType.acknowledgement)
			this.payloads = payloads == undefined ? [new Payload()] : Array.isArray(payloads) ? payloads : [payloads];

		if(unitAddr!=undefined)
			this.linkHeader = new LinkHeader(unitAddr);

//		console.log(this);
	}
	getAcknowledgement(){
		const header = new Header(packetType.acknowledgement, this.header.packetId);
		return new Packet(header);
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
			const d = bytes.splice(0, parameter.size);
			const payload = new Payload(parameter,d);
			pl.push(payload);
		}
		return pl;
	}
	toBytes(){
		const headerBytes = this.header.toBytes();
		const payloadBytes = this.payloadsToBytes();
		return headerBytes.concat(payloadBytes);
	}
	payloadsToBytes(){

		let bytes = [];
		if(!this.payloads)
			return bytes;

		this.payloads.forEach(pl=>bytes=bytes.concat(pl.toBytes()));

		return bytes;
	}
	toString(){
		return this.header.toString() + (this.payloads ? ', ' + this.payloads.map(pl=>pl.toString(this.header.groupId)) : '');
	}
	getData(parameterCode){
		if(parameterCode)
			return this.payloads.filter(pl=>(pl.parameter.code&0xff)==parameterCode).map(pl=>pl.getData(this.header.groupId));
		if(this.payloads.length)
			return this.payloads[0].getData(this.header.groupId);
	}
}
class LinkHeader{
	constructor(unitAddr){
		this.unitAddr = unitAddr;
	}
	toBytes(){
		return [this.unitAddr, 0, 0];
	}
	toString(){
		return this.unitAddr;
	}
}
class Header{
	constructor(type, packetId, groupId, error){
		// From bytes
		if(Array.isArray(type)){
			const bytes = type;
			this.type 		= bytes[0]&0xff;						// byte	type;		0
			this.packetId 	= (bytes[2]&0xff) * 256 + (bytes[1]&0xff);		// short packetId;	1,2 

			if(bytes.length>=HEADER_SIZE && this.type != packetType.acknowledgement){
				this.groupId 	= bytes[3]&0xff;				// byte groupId;	3
				this.reserved	= 0;							// short reserved;	4,5
				this.error		= (packetId == undefined ? bytes[6]&0xff : packetId);
			}
			return;							// byte errorCode;	6
		}

		this.type 		= (type == undefined ? packetType.request : type);								// byte	type;		0
		this.packetId 	= (packetId == undefined ? Math.floor(Math.random() * 32767 ) : packetId);		// short packetId;	1,2

		if(this.type==packetType.acknowledgement)
			return;

		this.groupId 	= (groupId == undefined ? packetGroupId.deviceInfo : (typeof groupId == "number") ? groupId : undefined);	// byte groupId;	3; 
		this.reserved	= 0;															// short reserved;	4,5
		this.error		= (error == undefined ? groupId : error);						// byte errorCode;	6
	}
	toBytes(){
		const id = shortToBytes(this.packetId);
		if(this.type == packetType.acknowledgement)
			return [this.type, id[0], id[1]];
		const reserved = shortToBytes(this.reserved);
		return [this.type, id[0], id[1], this.groupId, reserved[0], reserved[1], this.error];
	}
	toBytesAcknowledgement(){
		const id = shortToBytes(this.packetId);
		return [this.type, id[0], id[1]];
	}
	toString(){
		if(this.type == packetType.acknowledgement)
			return 'type = ' + PACKET_TYPE[this.type] + ', ID = ' + this.packetId;
		return 'type = ' + PACKET_TYPE[this.type] + ', ID = ' + this.packetId + ', groupId = ' + PACKET_GROUP_ID[this.groupId] + ', error = ' + ((typeof this.error != "number") ? this.error : PACKET_ERROR[this.error]);
	}
}
class Payload{
	constructor(parameter, data){
		// From bytes
		if(Array.isArray(parameter)){
			this.parameter = new Parameter(parameter.subarray(0, PARAMETER_SIZE));
			if(this.parameter.size)
				this.data = parameter.subarray(PARAMETER_SIZE);
				return;
		}
		this.parameter = (parameter == undefined ? new Parameter() : parameter);
		if(data){
			this.data = data;
			this.parameter.size = data.length;
		}
	}
	toBytes(){
		if(this.data)
			return this.parameter.toBytes().concat(this.data);
		return this.parameter.toBytes();
	}
	toString(packetGroupId){
		let str;
		if(!this.data)
			str = '';
		else if(packetGroupId){
			const tmp = parameterCode[packetGroupId][this.parameter.code];
			if(tmp)
				str = tmp.parseFunction(this.data);
			else
				str = this.data;
		}else
			str = this.data;
		return 'Payload:{Parameter:{' + this.parameter.toString(packetGroupId) + '}' + str + '}'
	}
	getData(packetGroupId){
		if(!this.data)
			return null;
		else if(packetGroupId){
			const tmp = parameterCode[packetGroupId][this.parameter.code];
			if(tmp)
				return tmp.parseFunction(this.data);
			else
				return this.data;
		}else
			return this.data;
	}
}

const PARAMETER_ALL			= 255;
const PARAMETER_READ_WRITE	= 3;
class Parameter{
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

		let str;

		if(packetGroupId){
			const tmp = parameterCode[packetGroupId][this.code];
			if(tmp)
				str = ' (' + tmp.description + ')';
			else
				str = '';
		}else
			str = '';

		return 'code: ' + this.code  + str + ', size: ' + this.size;
	}
}
const IS_FCM = true;
const IS_BUC = !IS_FCM;
class Register{
	constructor(index, addr, value){
		this.index = index;
		this.addr = addr;
		this.value = value;
	}
	toBytes(){
		if(typeof this.index === 'undefined' || typeof this.addr === 'undefined')
			return;
		const indexAddr = intToBytes(this.index).concat(intToBytes(this.addr));
		if(typeof this.value === 'undefined')
			return indexAddr;
		return indexAddr.concat(intToBytes(this.value));
	}
}
const DEVICE_FCM_ADC_INPUT_POWER	 = ()=>new Register(10,0);
const DEVICE_FCM_ADC_OUTPUT_POWER	 = ()=>new Register(10,1);
const DEVICE_CONVERTER_DAC1		 = ()=>new Register(1,0);
const DEVICE_CONVERTER_DAC2		 = ()=>new Register(2,0);
const DEVICE_CONVERTER_DAC3		 = ()=>new Register(3,0);
const DEVICE_CONVERTER_DAC4		 = ()=>new Register(4,0);