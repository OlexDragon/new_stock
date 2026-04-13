import {parseToIrtValue, parseToInt} from '../service/converter.mjs'
import FcmStatus from './value/fcm-status.js'
import Measurement  from "./parameters.mjs";

export default class MeasurementFcm extends Measurement{

	constructor(){
		super(measurement, 'Measurement FCM');
	}
}

const measurement = {};

//	FCM Parameter CODE
measurement.None = {};
measurement.None.code			 = 0;
measurement.parser = data=>data;

measurement['Summary Alarm'] = {};
measurement['Summary Alarm'].code	 = 1;
measurement['Summary Alarm'].parser = bytes=>bytes.toString();

measurement.Status = {};
measurement.Status.code			 = 2;
measurement.Status.parser = bytes=>new FcmStatus(parseToInt(bytes)).all;

measurement['Input Power'] = {};
measurement['Input Power'].code	 = 4;
measurement['Input Power'].parser = bytes=>parseToIrtValue(bytes, 10, ' dBm');

measurement['Output Power'] = {};
measurement['Output Power'].code		 = 5;
measurement['Output Power'].parser = bytes=>parseToIrtValue(bytes, 10, ' dBm');

measurement.Temperature = {};
measurement.Temperature.code	 = 3;
measurement.Temperature.parser = bytes=>parseToIrtValue(bytes, 10, ' °C');

measurement['5.5V'] = {};
measurement['5.5V'].code			 = 6;
measurement['5.5V'].parser = bytes=>parseToInt(bytes)/1000 + ' V';

measurement['13.2V'] = {};
measurement['13.2V'].code			 = 7;
measurement['13.2V'].parser = bytes=>parseToInt(bytes)/1000 + ' V';

measurement['-13.2V'] = {};
measurement['-13.2V'].code		 = 8;
measurement['-13.2V'].parser = bytes=>parseToInt(bytes)/1000 + ' V';

measurement.Current = {};
measurement.Current.code			 = 9;
measurement.Current.parser = bytes=>parseToInt(bytes)/1000 + ' mA';

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

measurement.Reference = {};
measurement.Reference.code	 = 21;
measurement.Reference.parser = bytes=>bytes[0]

measurement.all = {}
measurement.all.code				 = 255;
