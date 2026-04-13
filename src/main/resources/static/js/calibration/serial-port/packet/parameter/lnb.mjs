import Parameter  from "./parameters.mjs";

export default class ControlLnb extends Parameter{

	constructor(){
		super(config, 'Control LNB');
	}
}

const config = {};

config.Switchover = {};
config.Switchover.code = 14;  /* Switch the unit to be online */
config.Switchover.parser = parser;

config.Status = {};
config.Status.code = 15;		/* config Unit status */
config.Status.parser = parser;

config['Band Select'] = {};
config['Band Select'].code = 124;/* Frequency range select  */
config['Band Select'].parser = parser;

config['Mode Select'] = {};
config['Mode Select'].code = 125;/* Mode select. Not saving parameters, that is AUTO on startup. Possible options: AUTO/MANUAL/UNKNOWN */
config['Mode Select'].parser = parser;

config['LNB1 Band Select'] = {};
config['LNB1 Band Select'].code = 131;/* LNB1 Frequency range select */
config['LNB1 Band Select'].parser = parser;

config['LNB2 Band Select'] = {};
config['LNB2 Band Select'].code = 132;/* LNB2 Frequency range select */
config['LNB2 Band Select'].parser = parser;


function parser(value){
	const n = name(value);
	switch(n){

	case 'Mode Select':
			return bytes => {
				const modes = [,'AUTO', 'MANUAL'];
				const b = bytes[0];
				return {key: b, name: modes[b]};
			};

	default:
		console.warn('No parser for irpc config parameter ' + value);
		return b=>b;
	}
}
