
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
PACKET_ERROR[packetError.noError]		 = `No Error (${packetError.noError})`;
PACKET_ERROR[packetError.internalError]	 = `Packet ERROR (${packetError.internalError}):\n Internal System Error`;
PACKET_ERROR[packetError.WriteError]	 = `Packet ERROR (${packetError.WriteError}) :\n Write Error`;
PACKET_ERROR[packetError.functionNotImplemented] = `Packet ERROR (${packetError.functionNotImplemented}):\n Function not implemented`;
PACKET_ERROR[packetError.notInRange]	 = `Packet ERROR (${packetError.notInRange}):\n Value outside of valid range`;
PACKET_ERROR[packetError.canNotGenerate] = `Packet ERROR (${packetError.canNotGenerate}):\n Requested information can’t be generated`;
PACKET_ERROR[packetError.canNotExecute]	 = `Packet ERROR (${packetError.canNotExecute}):\n Command can’t be executed`;
PACKET_ERROR[packetError.InvalidFormat]	 = `Packet ERROR (${packetError.InvalidFormat}):\n Invalid data format`;
PACKET_ERROR[packetError.InvalidValue]	 = `Packet ERROR (${packetError.InvalidValue}):\n Invalid value`;
PACKET_ERROR[packetError.noMemory]		 = `Packet ERROR (${packetError.noMemory}):\n Not enough memory`;
PACKET_ERROR[packetError.notFoundr]		 = `Packet ERROR (${packetError.notFoundr}):\n Requested element not foundr`;
PACKET_ERROR[packetError.timedout]		 = `Packet ERROR (${packetError.timedout}):\n Timed out`;
PACKET_ERROR[packetError.noCommunication] = `Packet ERROR (${packetError.noCommunication}):\n Communication problem`;

function packetErrorCode(name){
	return packetError[name];
}

function packetErrorName(code){
	const keys = Object.keys(packetError);
	for(const key in keys)
		if(packetError[key] == code)
			return key;
}

export function toString(value){

	if(typeof value === 'number'){

		const name = packetErrorName(value);
		return `${name} (${value})`;

	}else{

		const code = packetErrorCode(value);
		return `${value} (${code})`;
	}
}

const packetErrorWorker = {};

packetErrorWorker.code = packetErrorCode;
packetErrorWorker.name = packetErrorName;
packetErrorWorker.toString = toString;

export default packetErrorWorker;
export {packetError, PACKET_ERROR};
