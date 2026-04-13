import {inputTool} from '../../calibration.mjs';
import {Payload, Parameter} from '../serial-port/packet/packet.mjs';
import deviceDebug from '../serial-port/packet/parameter/device-debug.mjs';
import Register from '../serial-port/packet/parameter/value/register.mjs';
import {show as showToast} from '../../toast-worker.mjs';
import Converter, {INTERVAL, setFromCookies} from './fcm.mjs';

const SKIP_RUN = 2;

export default class OouputPowerFCM extends Converter{

	#skipRun = SKIP_RUN;
	#offset;
	#$minVal;

	constructor($modal){
		super(
			'Output Power',
			$modal,
			'op',
			'out-power-lut-entry',
			new Payload(new Parameter(deviceDebug.readWrite.code), new Register(10,1).toBytes())
		);
		this.#$minVal = this.on($modal.find('#minVal'), 'change', ({currentTarget:{value}})=>value && Cookies.set(`${prefix}-minVal`, value, { expires: 99, path: '/calibration'}));
		setFromCookies(this.#$minVal, `${prefix}-minVal`);
	}
	_childRun(){
//		console.log('RUN', 'this.#skipRun', this.#skipRun, 'this.#calRunning', this.#calRunning)
		if(this.#skipRun>0){
			if(this.#skipRun===SKIP_RUN)
			showToast('Delay', 'Delay to stabilize power. - ' + (this.#skipRun * INTERVAL / 1000) + ' sec.', 'text-bg-info');
			--this.#skipRun; // Delay 
			return false;
		}
		return true;
	}
	_valueToShow(toolV){
		return (parseFloat(toolV) + this.#offset).toFixed(1);
	}
	_cpuTypeChange({currentTarget:{value}}){

		switch(value){

		case '732':
			this._payload = new Payload(new Parameter(deviceDebug.readWrite.code), new Register(10,36700161).toBytes());
			break;

		default:
			this._payload = new Payload(new Parameter(deviceDebug.readWrite.code), new Register(10,1).toBytes());
		}
	}
	_confirmStart(){

		if(!confirm('Does the power meter reading match the maximum value?'))
			return false;
		if(super._confirmStart()){
			this.#skipRun = SKIP_RUN;
			return true;
		}
		return false;
	}
	_preset(){
		this._stopValue = this._$toolVal.val();
		const max = this._$maxVal.val();
		const drop = max - this.#$minVal.val();
		this._defaultToolVal = this._stopValue - drop;
		inputTool.set('power', this._defaultToolVal);
		this.#offset = max - this._stopValue;
	}
	_btnStartClick(e){
		const {currentTarget:{innerText}} = e;
		switch(innerText){

		case 'Restart':
			this.#skipRun = SKIP_RUN;
		}
		super._btnStartClick(e);
	}
	destroy(){
		// Clean child class properties
		this.#$minVal = null;
		this.#offset = null;
		this.#skipRun = SKIP_RUN;
		
		// Call parent destroy to clean all Converter properties
		super.destroy();
		
		console.log('OouputPowerFCM instance destroyed');
	}
}
