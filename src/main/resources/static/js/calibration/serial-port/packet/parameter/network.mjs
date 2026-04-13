import {parseToIrtValue, parseToStatus} from '../service/converter.mjs'
import {type} from '../service/device-type.js'

const network = {};
network.buc = {};

function chooseGroup(){
	let t
	switch(type){
	default:
		t = 'buc';
	}
	return network[t];
}

// BUC Parameter CODE
network.buc[0] = {}
network.buc.none					 = 0;
network.buc[0].description = 'None';
network.buc[0].parser = data=>data.toString();
network.buc[1] = {}
network.buc.inputPower				 = 1;
network.buc[1].description = 'Input Power';
network.buc[1].parser = bytes=>parseToIrtValue(bytes, 10, ' dBm');
network.buc[2] = {}
network.buc.outputPower				 = 2;
network.buc[2].description = 'Output Power';
network.buc[2].parser =  bytes=>parseToIrtValue(bytes, 10, ' dBm');
network.buc[3] = {}
network.buc.unitTemperature			 = 3;
network.buc[3].description = 'Temperature';
network.buc[3].parser = bytes=>parseToIrtValue(bytes, 10, ' °C');
network.buc[4] = {}
network.buc.status					 = 4;
network.buc[4].description = 'Status';
network.buc[4].parser = parseToStatus;
network.buc[5] = {}
network.buc.lnb1Status				 = 5;
network.buc[5].description = 'LNB 1';
network.buc[5].parser = data=>data.toString();
network.buc[6] = {}
network.buc.lnb2Status				 = 6;
network.buc[6].description = 'LNB 2';
network.buc[6].parser = data=>data.toString();
network.buc[7] = {}
network.buc.reflectedPower			 = 7;
network.buc[7].description = 'Reflected Power';
network.buc[7].parser = data=>data.toString();
network.buc[8] = {}
network.buc.downlinkWaveguideSwitch	 = 8;
network.buc[8].description = 'Switch';
network.buc[8].parser = data=>data.toString();
network.buc[9] = {}
network.buc.downlinkStatus			 = 9;
network.buc[9].description = 'Status';
network.buc[9].parser = data=>data.toString();

export function code(name){
	if(typeof name === 'number')
		return name;
	const group = chooseGroup();
	return group[name];
}

export function name(code){
	const group = chooseGroup();
	const keys = Object.keys(group);

	for(const key of keys)
		if(deviceInfo[key] == code)
			return key;
}

export function description(value){
	const c = code(value)
	return chooseGroup()[c].description;
}

export function toString(value){
	const c = code(value)
	const name = name(value)
	return `measurement: ${name} (${c})`;
}

export function parser(value){
	const c = code(value)
	return chooseGroup()[c].parser;
}
