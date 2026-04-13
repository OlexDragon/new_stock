import {inputTool} from '../../calibration.mjs';
import {Payload, Parameter} from '../serial-port/packet/packet.mjs';
import deviceDebug from '../serial-port/packet/parameter/device-debug.mjs';
import Register from '../serial-port/packet/parameter/value/register.mjs';
import Converter from './fcm.mjs';

export default class InputPowerFCM extends Converter{

	constructor($modal){
		super(
			'FCM Input Power',
			$modal,
			'ip',
			'in-power-lut-entry',
			new Payload(new Parameter(deviceDebug.readWrite.code), new Register(10,0).toBytes())
		);
	}
	_cpuTypeChange({currentTarget:{value}}){

		switch(value){

		case '732':
			this._payload = new Payload(new Parameter(deviceDebug.readWrite.code), new Register(10,33554432).toBytes());
			break;

		default:
			this._payload = new Payload(new Parameter(deviceDebug.readWrite.code), new Register(10,0).toBytes());
		}
	}
	_preset(){
		this._stopValue = this._$maxVal.val();
		this._defaultToolVal = this._$toolVal.val();
		inputTool.set('power', this._defaultToolVal);
		console.log('Preset complete. this._stopValue = ', this._stopValue, ' toolVal = ', toolVal);
	}
}
