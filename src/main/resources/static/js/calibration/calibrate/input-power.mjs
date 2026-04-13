import ControllerlLoader from '../controller-loader.mjs';
import {serialNumber, partNumber as pn} from '../cards/onrender-card.mjs'
import {partNumber} from '../cards/header-card.mjs';

export default class IntputPower extends ControllerlLoader{

	constructor(){
		super(serialNumber ? `/calibration/input-power?sn=${serialNumber ?? info['Serial number']}&pn=${partNumber ?? pn ?? info['Part number']}&deviceId=${info['Device ID'].replace(/[{}]/g, '')}` : '/calibration/converter/input-power', 'ip');
	}
}