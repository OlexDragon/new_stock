import {parseToIrtValue, parseToShortArray, parseToBigInt, parseToBigIntArray, parseToBoolean} from '../service/converter.mjs'

const control = {};

// BUC Parameter CODE
control['LO Set']		 = {}
control['LO Set'].code	 = 1;
control['LO Set'].parser =  parseToBigInt;

control.Mute		 = {}
control.Mute.code	 = 2;
control.Mute.parser	 =  parseToBoolean;

control.Gain		 = {}
control.Gain.code	 = 3;
control.Gain.parser	 = bytes=>parseToIrtValue(bytes, 1);

control['Gain Range']		 = {}
control['Gain Range'].code	 = 5;
control['Gain Range'].parser = parseToShortArray;

control.Attenuation			 = {}
control.Attenuation.code	 = 4;
control.Attenuation.parser	 = bytes=>parseToIrtValue(bytes, 1);

control['Attenuation Range'] = {}
control['Attenuation Range'].code		 = 6;
control['Attenuation Range'].parser = parseToShortArray;

control.LO			 = {}
control.LO.code		 = 7;
control.LO.parser	 = data=>data.toString();

control.Frequency		 = {}
control.Frequency.codr	 = 8;
control.Frequency.parser = parseToBigInt;

control['Frequency Range']			 = {}
control['Frequency Range'].code		 = 9;
control['Frequency Range'].parser	 = parseToBigIntArray;

control.Redundancy = {}	// Enable
control.Redundancy.code		 = 10;
control.Redundancy.parser = parseToBoolean;

control.Mode		 = {}	// Redundancy
control.Mode.code	 = 11;
control.Mode.parser = data=>data.toString();

control.Name		 = {}	// Redundancy
control.Name.code	 = 12;
control.Name.parser	 = data=>data.toString();

control.Status			 = {}	// Redundancy
control.Status.code		 = 15;
control.Status.parser	 = parseToIrtValue;

control.Online			 = {}	// Redundancy
control.Online.code		 = 14;
control.Online.parser	 = data=>data.toString();

control['Spectrum Inversion']		 = {}
control['Spectrum Inversion'].code	 = 20;
control['Spectrum Inversion'].parser = data=>data.toString();

// LNB Parameter CODE
control.lnb = {};
control.lnb[0] = {}
control.lnb.none					 = 0;
control.lnb[0].description = 'None';

Object.freeze(control);
export default control;

export function code(name){
	if(typeof name === 'number')
		return name;
	return group[name].code;
}

export function name(code){
	const keys = Object.keys(control);

	for(const key of keys)
		if(control[key].code == code)
			return key;
}

export function toString(value){
	const c = code(value)
	const n = name(value)
	return `configuration: ${n} (${c})`;
}

export function parser(value){
	const n = name(value)
	return control[n].parser;
}
