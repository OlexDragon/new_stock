import {parseToIrtValue, parseToInt} from '../service/converter.mjs'
import FcmStatus from './value/rcm-status.js'
import Measurement  from "./parameters.mjs";

export default class MeasurementRcm extends Measurement{

	constructor(){
		super(measurement, 'Measurement RCM');
	}
}

const measurement = {};

measurement.Source = {};
measurement.Source.code	 = 1;
measurement.Source.parser = source;

measurement['CPU Temperature'] = {};
measurement['CPU Temperature'].code	 = 10;
measurement['CPU Temperature'].parser = bytes=>parseToIrtValue(bytes, 10, ' °C');

measurement['MCU Temperature'] = {}
measurement['MCU Temperature'].code		 = 11;
measurement['MCU Temperature'].parser =bytes=>parseToIrtValue(bytes, 10, ' °C');

measurement['Reference Level'] = {}
measurement['Reference Level'].code		 = 12;
measurement['Reference Level'].parser =bytes=>parseToInt(bytes);

measurement['Reference Status'] = {}
measurement['Reference Status'].code		 = 13;
measurement['Reference Status'].parser =bytes=>new FcmStatus(parseToInt(bytes)).all;;

measurement.Attenuation = {};
measurement.Attenuation.code		 = 20;
measurement.Attenuation.parser = bytes=>parseToInt(bytes)/10 + ' dB';

measurement.all = {}
measurement.all.code				 = 255;

function source(bytes){

	switch(bytes[0]){

	case 1:
		return 'Internal'

	case 2:
		return 'External'

	case 3:
		return 'Autosense'

	default:
		return 'undefined'
	}
}