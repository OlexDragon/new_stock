import ControllerlLoader from '../controller-loader.mjs';
import {serialNumber, partNumber as pn} from '../cards/onrender-card.mjs'
import {partNumber} from '../cards/header-card.mjs';

export default class OutputPower extends ControllerlLoader{

	constructor(){
		super(serialNumber ? `/calibration/op?sn=${serialNumber ?? info['Serial number']}&pn=${partNumber ?? pn ?? info['Part number']}` : '/calibration/converter/output-power', 'op');
	}
}