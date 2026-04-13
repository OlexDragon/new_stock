import Parameter  from "./parameters.mjs";

export default class ControlDrcp extends Parameter{

	constructor(){
		super(config, 'Control DRCP');
	}

	get all(){
		const {['Band Select']:bandSelect, Status, Switchover, ['LNB1 Band Select']:lNB1BandSelect, ['LNB2 Band Select']:lNB2BandSelect} = this.parameters;
		return {bandSelect, Status, Switchover, lNB1BandSelect, lNB2BandSelect};
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
config['Mode Select'].parser = parseModeSelect;

config['LNB1 Band Select'] = {};
config['LNB1 Band Select'].code = 131;/* LNB1 Frequency range select */
config['LNB1 Band Select'].parser = parser;

config['LNB2 Band Select'] = {};
config['LNB2 Band Select'].code = 132;/* LNB2 Frequency range select */
config['LNB2 Band Select'].parser = parser;


function parseModeSelect(data){
	const modes = [,'AUTO', 'MANUAL'];
	const b = data[0];
	return {key: b, name: modes[b]};
}
function parser(data){
	console.warn('No parser for irpc config parameter ' + data);
	return data[0];
}
