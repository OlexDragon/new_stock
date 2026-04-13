import {parseToIrtValue} from '../service/converter.mjs'
import Measurement  from "./parameters.mjs";

export default class MeasurementOdrc extends Measurement{

	constructor(){
		super(measurement, 'Measurement ODRC');
	}
}

const lnbStatus = ['Not Ready', 'Ready'];
const measurement = {};

// BUC Parameter CODE

measurement.Status = {}
measurement.Status.code		 = 1;
measurement.Status.parser	 = 'do not show';

measurement.Temperature = {}
measurement.Temperature.code		 = 3;
measurement.Temperature.parser		 = bytes=>parseToIrtValue(bytes, 10, ' °C');

measurement['WGS Status'] = {}
measurement['WGS Status'].code		 = 4;
measurement['WGS Status'].parser	 = parseToStatus;

measurement['LNB 1'] = {} // Status
measurement['LNB 1'].code			 = 5;
measurement['LNB 1'].parser			 = bytes=>lnbStatus[bytes[0]];

measurement['LNB 2'] = {} // Status
measurement['LNB 2'].code			 = 6;
measurement['LNB 2'].parser			 = bytes=>lnbStatus[bytes[0]];

measurement['LNB 3'] = {}
measurement['LNB 3'].code			 = 7;
measurement['LNB 3'].parser			 = bytes=>lnbStatus[bytes[0]];

measurement.all = {}
measurement.all.code				 = 255;

const statuses = ['UNKNOWN','LNB 1','LNB 2',,,,,,,,,'DEFAULT','PROTECTION LNB 1','PROTECTION LNB 2'];
function parseToStatus(bytes){
	return statuses[bytes[0]];
}