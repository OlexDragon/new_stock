import * as converter from '../service/converter.mjs'
import Parameter  from "./parameters.mjs";

export default class ControlRcm extends Parameter{

	constructor(){
		super(config, 'Control RCM');
	}

	get all(){
		const {Capabilities, ['DAC Range']:range, DAC, Source} = this.parameters;
		return {Capabilities, range, DAC, Source};
	}
}

const config = {};

config.Source				 = {}
config.Source.code			 = 1;
config.Source.parser		 = bytes=>bytes[0];	// UNDEFINED = 0, INTERNAL = 1, EXTERNAL  = 2, AUTOSENSE = 3

config['DAC Range']			 = {}
config['DAC Range'].code	 = 2;
config['DAC Range'].parser	 = converter.parseToIntArray;

config.DAC					 = {}
config.DAC.code				 = 3;
config.DAC.parser			 = converter.parseToIntUnsigned;

config['DAC Step Range']		 = {}
config['DAC Step Range'].code	 = 4;
config['DAC Step Range'].parser	 = bytes=>converter.parseToIrtValue(bytes, 10);

config['DAC Step']			 = {}
config['DAC Step'].code		 = 5;
config['DAC Step'].parser		 = converter.parseToInt;

config.Increment		 = {}
config.Increment.code	 = 6;
//config.Increment.parser	 = converter.parseToInt;

config.Decrement			 = {}
config.Decrement.code		 = 7;
//config.Decrement.parser		 = converter.parseToInt;

// Factory Reset is a command, not a setting, so it doesn't need a parser
config['Factory Reset']			 = {}
config['Factory Reset'].code	 = 8;
//config['Factory Reset'].parser		 = converter.parseToBigInt;

// Factory Value is a command, not a setting, so it doesn't need a parser
config['Factory Value']			 = {}
config['Factory Value'].code	 = 9;
//config['Factory Value'].parser	 = converter.parseToInt;

config.Capabilities	 = {}
config.Capabilities.code = 19;
config.Capabilities.parser = converter.parseToCapabilities;

