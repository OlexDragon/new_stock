import Tool from './tool.mjs'
//import {eventBus} from '../../calibration.mjs'

export default class OutputTool extends Tool{

	constructor($parents){
		super($parents);
		this._timeout = 3000;
	}
	subscribe(callback) {
		super.subscribe('outputToolValue', callback);
	}
	_spChange(e){
		const sp = super._spChange(e);
//		console.log(sp);
	}
	get(){
		this._$toolButons.click();
	}
}