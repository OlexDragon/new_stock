
const groupId = {};

groupId.alarm		 = 1;
groupId.configuration= 2;
groupId.filetransfer = 3;
groupId.measurement	 = 4;
groupId.reset		 = 5;
groupId.deviceInfo	 = 8;
groupId.control		 = 9;
groupId.protocol	 = 10;
groupId.network		 = 11;
groupId.redundancy	 = 12;
groupId.deviceDebug	 = 61;
groupId.production	 = 100;
groupId.developer	 = 120;

Object.freeze(groupId);

export default groupId;

export function id(name){
	return groupId[name];
}

export function name(code){
	const keys = Object.keys(groupId);
	for(const key of keys)
		if(groupId[key] == code)
			return key;
}

export function toString(value){

	if(typeof value === 'number'){

		const n = name(value);
		return `${n} (${value})`;

	}else{

		const code = id(value);
		return `${value} (${code})`;
	}
}
