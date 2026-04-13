
const packetIdArray = Object.freeze(
	[
		'deviceInfo',

		'measurement',
		'measurementIRPC',

		'configAll',
		'attenuation',
		'attenuationSet',
		'gain',
		'gainSet',
		'frequency',
		'frequencySet',
		'muteSet',
		'loSet',

		'network',
		'networkSet',

		'alarmDescription',
		'alarm',
		'alarmSummary',
		'alarmIDs',

		'redundancyAll',
		'redundancySetOnline',
		'redundancySetEnable',
		'redundancySetDisable',
		'redundancySetCold',
		'redundancySetHot',
		'redundancySetNameA',
		'redundancySetNameB',

		'irpc',
		'irpcSalectSwtchHvr',
		'irpcStandBy',
		'irpcDefault',
		'irpcHoverA',
		'irpcHoverB',

		'odrc',
		'odrcSetMode',
		'odrcLNBSelect',

		'comAll',
		'comSetAddress',
		'comSetRetransmit',
		'comSetStandard',
		'comSetBaudrate',

		'module',
		'moduleSet',

		'register',
		'register1',
		'register2',
		'register3',
		'register4',
		'registerSet',

		'calMode',
		'calModeSet',

		'dacs',
		'dacsSet',

		'lnbSetMode',
		'lnbOverSet',

		'lnbRegisters',
		'lnbRegistersSet',

		'lnbBand',
		'lnbBandSet',

		'dacRcm',
		'dacSetRcm',

		'admv1013',
		'admv1013Set',

		'admv1013Bias',
		'admv1013BiasSet',

		'stuw81300',
		'stuw81300Set',

		'stuw81300Bias',
		'stuw81300BiasSet',

		'dump',
		'dumpHelp',

		'noAction',

		'saveConfig',

		'rcmSourceSet',
		'rcmDacSet',
		'rcmDacSave',
		'rcmDacDefault',
	
		'POTs_KA_BIAS',
		'POTs_KA_Converter'
	]);

const packetId = Object.freeze(packetIdArray.reduce((a, v, i)=>({...a, [v]: i}), {}));
export default packetId;
//console.log(packetId);

export function id(name){
	if(typeof name === 'number')
		return name;
	return packetId[name];
}

export function name(code){
	return packetIdArray[code];
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