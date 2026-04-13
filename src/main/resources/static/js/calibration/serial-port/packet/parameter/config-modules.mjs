import {parseToString} from '../service/converter.mjs'

const config = {};

config.saveProfile	 = 1;
config.reset		 = 2;
config.restor		 = 3;
config.activeModule	 = 10;
config.moduleList	 = 11;

Object.freeze(config);

const configNames = Object.keys(config).reduce((a,k)=>{

		a[config[k].code] = k;
		return a;
	}, []
);
Object.freeze(configNames);

export default config;

export function code(name){
	if(typeof name === 'number')
		return name;

	return config[name];
}
export function name(code){
	if(typeof code === 'string')
		return configNames.includes(code) ? code : undefined;

	return configNames[code];
}

export function parser(codeId){

	if(typeof codeId === 'string')
		codeId = code(codeId);

	switch(codeId){

	case config.moduleList:
		return parseModuleList;

	case config.activeModule:
		return b=>b[0];

	default:
		return b=>b;
	}
}


export function toString(value){
	const c = code(value)
	const n = name(value)
	return `configuration: ${n} (${c})`;
}

function parseModuleList(bytes){
	if(!bytes?.length)
		return;

	const modules = {};
	const b = [...bytes];
	while(b.length){

		const moduleId = b.splice(0,1)&0xff;
		const length = b.indexOf(0) + 1;
		const name = parseToString(b.splice(b, length || b.length));
		modules[name] = moduleId;
	}

	return modules;
}
