import {id} from './group-id.js'
import {description as diDescription, parser as diParser, name as diName, toString as diToSyting} from '../parameter/device-info.js'
import {description as measDescription, parser as measParser, name as measName, toString as measToSyting} from '../parameter/measurement.js'
import {parser as confParser, name as confName, toString as confToSyting} from '../parameter/config-modules.js'
import {parser as contrParser, name as contrName, toString as contrToSyting} from '../parameter/config-buc.js'
import {parser as protocolParser, name as protocolName, toString as protocolToSyting} from '../parameter/protocol.js'
import {parser as irpcParser, name as irpcName, toString as irpcToSyting} from '../parameter/irpc.js'

const deviceInfo = id('deviceInfo')
const deviceDebug = id('deviceDebug')
const measurement = id('measurement')
const configuration = id('configuration')
const control = id('control')
const protocol = id('protocol')
const redundancy = id('redundancy')
const functions = {};

// Device Info

functions[deviceInfo] = {};
functions[deviceDebug] = {};

// Device Info
functions[deviceInfo].name = diName;
functions[deviceInfo].description = diDescription;
functions[deviceInfo].parser = diParser;
functions[deviceInfo].toString = diToSyting;

// measurement
functions[measurement] = {};
functions[measurement].name = measName;
functions[measurement].description = measDescription;
functions[measurement].parser = measParser;
functions[measurement].toString = measToSyting;

// Configuration
functions[configuration] = {};
functions[configuration].name = confName;
functions[configuration].description = confDescription;
functions[configuration].parser = confParser;
functions[configuration].toString = confToSyting;

// Control
functions[control] = {};
functions[control].name = contrName;
functions[control].description = contrDescription;
functions[control].parser = contrParser;
functions[control].toString = contrToSyting;

// Protocol
functions[protocol] = {};
functions[protocol].name = protocolName;
functions[protocol].parser = protocolParser;
functions[protocol].toString = protocolToSyting;

// IRPC
functions[redundancy] = {};
functions[redundancy].name = irpcName;
functions[redundancy].parser = irpcParser;
functions[redundancy].toString = irpcToSyting;

export function name(groupId){

	if(typeof groupId === 'string')
		groupId = id(groupId);

	return functions[groupId]?.name ?? `groupId (${groupId}) - Have to ass parameter to the parameter-code.js file`;
}

export function description(groupId){

	if(typeof groupId === 'string')
		groupId = id(groupId);

	return functions[groupId]?.description ?? `groupId (${groupId}) - Have to ass parameter to the parameter-code.js file`;
}

export function parser(groupId){

	if(typeof groupId === 'string')
		groupId = id(groupId);

	return functions[groupId]?.parser ?? function(){ return bytes=>`${bytes}; groupId (${groupId}) - Have to ass parser to the parameter-code.js file'}`; };
}

export function toString(groupId){

	if(typeof groupId === 'string')
		groupId = id(groupId);

	return functions[groupId]?.toString ?? function(value){ return `${value}; groupId (${groupId}) - Have to ass parameter to the parameter-code.js file`; };
}

