import * as converter from '../service/converter.mjs'
import Parameter  from "./parameters.mjs";

export default class ControBuc extends Parameter{

	constructor(){
		super(config, 'Control BUC');
	}

	get all(){
		const {gainRange, attenuationRange, frequencyRange, Gain, Attenuation, Frequency, loSet, LO, Mute} = this.parameters;
		return {gainRange, attenuationRange, frequencyRange, LO, Gain, Attenuation, Frequency, loSet, Mute};
	}
}

const config = {};

// BUC Parameter CODE
config.loSet				 = {}
config.loSet.code			 = 1;
config.loSet.parser		 = bytes=>bytes[0];

config.Mute				 = {}
config.Mute.code			 = 2;
config.Mute.parser			 = converter.parseToBoolean;

config.Gain				 = {}
config.Gain.code			 = 3;
config.Gain.parser			 = bytes=>converter.parseToIrtValue(bytes, 10);

config.gainRange			 = {}
config.gainRange.code		 = 5;
config.gainRange.parser	 = converter.parseToShortArray;

config.Attenuation			 = {}
config.Attenuation.code	 = 4;
config.Attenuation.parser	 = bytes=>converter.parseToIrtValue(bytes, 10);

config.attenuationRange	 = {}
config.attenuationRange.code = 6;
config.attenuationRange.parser	 = converter.parseToShortArray;

config.LO					 = {}
config.LO.code				 = 7;
config.LO.parser			 = converter.parseToLoFrequency;

config.Frequency			 = {}
config.Frequency.code		 = 8;
config.Frequency.parser		 = converter.parseToBigInt;

config.frequencyRange		 = {}
config.frequencyRange.code = 9;
config.frequencyRange.parser = converter.parseToBigIntArray;

config.Redundancy			 = {}
config.Redundancy.code		 = 10;
config.Redundancy.parser	 = converter.parseToBoolean;

config.Mode				 = {}	// Redundancy mode
config.Mode.code			 = 11;
config.Mode.parser			 = data=>data.toString();

config.Name				 = {}	// Redundancy name
config.Name.code			 = 12;
config.Name.parser			 = data=>data.toString();

config.Status				 = {}	// Redundancy status
config.Status.code			 = 15;
config.Status.parser		 = converter.parseToInt;

config.Online				 = {}	// Redundancy online
config.Online.code			 = 14;
config.Online.parser		 = data=>data.toString();

config.spectrumInversion	 = {}
config.spectrumInversion.code = 20;
config.spectrumInversion.parser = data=>data.toString();
