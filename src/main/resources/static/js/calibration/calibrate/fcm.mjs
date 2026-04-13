import {inputTool} from '../../calibration.mjs';
import Packet, {Header} from '../serial-port/packet/packet.mjs';
import packetType from '../serial-port/packet/packet-properties/packet-type.mjs';
import packetGroupId from '../serial-port/packet/packet-properties/group-id.mjs';
import deviceInfo, {parser as deviceInfoParser} from '../serial-port/packet/parameter/device-info.mjs';
import Register from '../serial-port/packet/parameter/value/register.mjs';
import {sendPacket} from '../fetch/json-sender.mjs';
import {show as showToast} from '../../toast-worker.mjs';
import {ajaxJSON} from '../fetch/fetch.mjs';
import DestroyableComponent from '../destroyable-component.mjs';
import Chart from '../chart.mjs';

export const INTERVAL = 2500;

const BTN_START = true;
const BTN_STOP = !BTN_START;
const OUTPUT = {on:1, off:0};

export default class Converter extends DestroyableComponent{

	constructor(tableName, $modal, prefix, propName, payload){
		super(tableName);
		this._tableName = tableName;
		this._$modal = $modal;
		this._prefix = prefix;
		this._payload = payload;
		this._$serialNumber = $('#serialNumber');
		this._$profilePath = $('.profilePath');
		this._$propName = $modal.find('#propName').val(propName);
		this._$calResult = $modal.find('#calResult');
		this._$unitSerialPort = this.on(
								$modal.find('#unitSerialPort')
								.append($('option', {selected: 'selected', disabled: 'disabled', hidden: 'hidden', value: ''}))
								.append(inputTool.portsOptions.not(':checked').clone()),
								'change', this._spChange.bind(this));
		this._$btnInfo = this.on($modal.find('#btnInfo'), 'click', this._btnInfoClick.bind(this));
		const spCookies = Cookies.get('unitSerialPort');
		if(spCookies){
			const $option = this._$unitSerialPort.find('option').filter((_,el)=>el.value===spCookies);
			if($option.length){
				$option.prop('selected', true);
				this._port = $option.val();
				this._$btnInfo.prop('disabled', false).click();
			}else{
				console.log("Converter's serial port is not selected.");
				alert("Select the converter's serial port.");
			}
		}else{
			console.log("Converter's serial port is not selected.")
			alert("Select the converter's serial port.");
		}
		this._$toolVal = this.on($modal.find('#toolVal'), 'keypress', this._toolValPress.bind(this)); // .change(toolValChange).focusout(e=>e.preventDefault());
		this._$unitVal = $modal.find('#unitVal');
		this._$stepVal = this.on($modal.find('#stepVal'), 'change', ({currentTarget:{value}})=>value && Cookies.set(`${prefix}-stepVal`, value, { expires: 99, path: '/calibration'}));
		setFromCookies(this._$stepVal, `${prefix}-stepVal`);
		this._$maxVal = $modal.find('#maxVal').change(({currentTarget:{value}})=>value && Cookies.set(`${prefix}-maxVal`, value, { expires: 99, path: '/calibration'}));
		setFromCookies(this._$maxVal, `${prefix}-maxVal`);
		this._$numberOfEntries = $modal.find('#numberOfEntries');
		this._$btnStart = this.on($modal.find('#btnStart'), 'click', this._btnStartClick.bind(this));
		this._calChart = new Chart(this._$modal.find('#ipChart'), tableName);
		this.on($modal.find('#cpuType'), 'change', this._cpuTypeChange.bind(this));
		this._$btnCopy = $modal.find('#btnCopy');
		// store bound callback reference so unsubscribe can remove it later
		this._boundToolCallBack = this._toolCallBack.bind(this);
		inputTool.subscribe('inputPower', this._boundToolCallBack);
	}
	
	get port(){
		return this._port;
	}
	onHide(){
		this._stop();
	}
	onShow(){
	}
	_start(){
		inputTool.set('output', OUTPUT.on);
		v_stop = false;
		this.clearInterval(this._calIntervalID);
		this._calIntervalID = this.setInterval(this.#run.bind(this), INTERVAL);
		startButtonText(this._$btnStart, BTN_STOP);
	}
	_stop(){
	inputTool.set('output', OUTPUT.off);
		v_stop = true;
		this._calIntervalID = this.clearInterval(this._calIntervalID);
		this._$toolVal.prop('readonly', false);
		this._$stepVal.prop('readonly', false);
		this._$btnInfo.prop('disabled', false);
		this._powerChanges = false;
		this._readingPacket = false;
		startButtonText(this._$btnStart, BTN_START);
	}
	_childRun(){
		return true;
	}
	#run(){
		if(!this._childRun())
			return

		if(this._stack>9){
			this._stack = 0;
			this._stop();
			console.warn('Stackover Flow');
			alert('Stackover Flow');
			return;
		}
		++this._stack;

		if(this._powerChanges || this._readingPacket){
			console.log('Calibration is busy.. this._powerChanges = ', this._powerChanges, ' this._readingPacket = ', this._readingPacket)
			showToast('Busy', 'Calibration is busy.. - ' + this._stack, 'text-bg-warning');
			return;
		}

		const data = this._calChart.data;
		const xLength = data.x.length;
		const yLength = data.y[0] ? data.y[0].length : 0;

		if(xLength == yLength){
			const toolV = inputTool.value();

			if(xLength && data.x[xLength-1] == toolV){
				this._nextStep(toolV);
				return;
			}

			const tmp = this._valueToShow(toolV);
			this._calChart.appendPoint(tmp, undefined); // Add new X value to the chart
			this._$numberOfEntries.text(data.x.length);
		}

		this._stack = 0;

		if(this._readingPacket){
			console.warn('this._readingPacket = ', this._readingPacket);
			return;
		}
		this._readingPacket = true;
		const packet = new Packet(new Header(packetType.request, undefined, packetGroupId.deviceDebug), this._payload);
		sendPacket(this.port, packet, this._packetValue.bind(this));
	}
	_spChange({currentTarget:{id, value}}){
		if(value){
			this._$btnStart.prop('disabled', true);
			Cookies.set(`${this._prefix}-${id}`, value, { expires: 99, path: '/calibration'});
			this._$btnInfo.prop('disabled', false);
			this._port = value;
		}
	}
	_valueToShow(toolV){
		return parseFloat(toolV).toFixed(1);
	}
	_toolValPress(e){
		if(e.which == 13){
			e.preventDefault();
			const value = e.currentTarget.value;
			inputTool.set('power', value);
			this._clear();
			this._$btnStart.text('Start');
		}
 	}
	_btnStartClick({currentTarget:{innerText}}){

		switch(innerText){

				// Stop Button
		case 'Stop':
			this._stop();
			this._$btnInfo.prop('disabled', false);
			return;;

		case 'Reset':
			this._calChart.reset();
			this._$numberOfEntries.text(this._calChart.data.x.length);
			this._$btnStart.text('Restart');
			this._$btnInfo.text('Optimize');
			this._$calResult.val('');
			return;

		case 'Restart':
			this._clear();
			inputTool.set('power', this._defaultToolVal);
			this._$btnInfo.text('Clear');
			break;

		case 'Continue':
			// Fall through to default case
			break;

		default:
			if(!this._confirmStart())
				return;
		}

		this._$btnInfo.prop('disabled', true);
		this._$toolVal.prop('readonly', true);
		this._$stepVal.prop('readonly', true);

				// Check Serial Port, Tool Address
		if(!inputTool.isReady){
			this._$modal.modal('hide');
			inputTool.show();
			return;
		}
		this.setTimeout(this._start.bind(this), 100); // Delay to update button state before starting calibration
	}
	_cpuTypeChange(){
		throw new Error('_cpuTypeChange() is not implemented.');
	}
	_btnInfoClick({currentTarget:{innerText}}){
		if(!this.port)
			return;
		if(innerText=='Clear'){
			this._clear();
			this._$btnStart.text('Restart');
			this._$btnInfo.text('Info');
			return;
		}
		if(innerText=='Optimize'){
			this._calChart.optimize();
			this._$btnStart.text('Reset');
			this._$btnInfo.text('Save');
			this._$numberOfEntries.text(this._calChart.data.x.length);
			this._showTable();
			this._$btnCopy.prop('disabled', false);
			return;
		}
		if(innerText=='Save'){
			this._saveToProfile();
			return;
		}
		sendPacket(this.port, new Packet(), this._showInfo.bind(this));
	}
	_toolCallBack(name, value){
		if(name === 'inputPower'){
			this._$toolVal.val(value);
			this._powerChanges = false;
		}
	}
	_showInfo(packet){
		if(packet.error || !packet.payloads?.length){
			console.warn(packet);
			showToast('Error', packet.error, 'text-bg-warning');
			this._$btnStart.prop('disabled', true);
			return;
		}
		packet.payloads.forEach(pl=>{

			const parameterCode = pl.parameter.code;

			switch(parameterCode){

			case deviceInfo.serialNumber:{
				const parser = deviceInfoParser(parameterCode);
				const val = parser(pl.data);
				this._$serialNumber.text(val);
				this._$btnStart.prop('disabled', false);
				$('.modal-title').text(this._tableName + ': ' + val);
				this._$btnInfo.text('Clear');
				this._$profilePath.prop('href', '/calibration/rest/profile/path?sn=' + val);
				inputTool.get();
				break;
			}

			case deviceInfo.description:{
				const parser = deviceInfoParser(parameterCode);
				const val = parser(pl.data);
				$('#unit_description').children('h4').text(val);
				break;
			}}
		});
	}
	_confirmStart(){

		this._preset();
		if(!this._stopValue){
			console.log('Input Tool Value is not selected.');
			alert('Input Tool Value needs to be set.');
			return false;
		}

		this._stopValue = parseFloat(this._stopValue);
		return true;
	}
	_preset(){
		throw new Error('_preset() is not implemented.');
	}
	_packetValue(packet){
		this._readingPacket = false;
		if(packet.error){
			console.log(packet);
			showToast('Packet Error', packet.error, 'text-bg-warning');
			return;
		}
		if(!packet.payloads?.length)
			return;
//		console.log(packet);
		const register =  Register.parseRegister(packet.payloads[0].data);

		const data = this._calChart.data;
		const xLength = data.x.length;
		const yLength = data.y[0] ? data.y[0].length : 0;
		
		if(xLength > yLength){
			const index = xLength-1;
			this._calChart.replacePoint(index, data.x[index], register.value); // Add new Y value to the chart
			this._$unitVal.val(register.value);
			this._calChart.update();
		}
		this._nextStep();
	}
	_nextStep(toSet){
		const toolV = toSet ?? parseFloat(inputTool.value());
		if(!this._startToolValue)
			this._startToolValue = toolV;
		if(toolV == this._stopValue){
			this._stop();
			this._$btnStart.text('Restart')
			this._$btnInfo.prop('disabled', false).text('Optimize');
			this._reset();
			console.info('Calibration completes.');
			alert('Calibration completes.');
			return;
		}
		const tmp = this._$stepVal.val();
		let step;
		if(tmp)
			step = parseFloat(tmp);
		else{
			step = 1;
			this._$stepVal.val(step);
		}

		let newValue = (toolV + step).toFixed(1);
		if(newValue>this._stopValue)
			newValue = this._stopValue;

		this._powerChanges = true;
		inputTool.set('power', newValue);
	}
	_saveToProfile(){
		const data = this._calChart.data;
		if(!data.x.length){
			console.log('There is nothing to save.');
			alert('There is nothing to save.');
		    return;
		}

		this._$btnInfo.addClass('disabled');

		const table = {serialNumber: this._$serialNumber.text(), name: this._tableName};
		const array = [];
		for(let i=0; i<data.x.length; i++)
			array.push({input: data.y[0][i], output: data.x[i]});

		table.values = array;
		ajaxJSON('/calibration/rest/profile/save', table)
		.done((data)=>{
			console.log(data.content);
			alert(data.content);
			this._$btnInfo.removeClass('disabled').text('Info');
			this._$modal.modal('hide');
		});
//		.fail(connectionFail);
	 }
 	_showTable(){
 		const data = this._calChart.data;
 		this._$calResult.empty();
 		const propName = this._$propName.val();

 		for(let i=0; i<data.x.length; i++){
 			const line = propName + ' ' + data.x[i] + ' ' + data.y[0][i];
 			this._$calResult
 			.append(
 				$('<div>', {class: 'row', text: line})
 			);
 		}
 	}
	_clear(){
		this._calChart.clear();
		this._calChart.update();
		this._powerChanges = false;
		this._readingPacket = false;

	}
	_reset(){
		if(this._startToolValue)
			inputTool.set('power', this._startToolValue);
		else
			inputTool.set('power', this._defaultToolVal);
	}

	destroy(){
		try{ if(this._boundToolCallBack) inputTool.unsubscribe('inputPower', this._boundToolCallBack); }catch(e){}
		this._boundToolCallBack = null;
		// Call parent destroy to remove namespaced event listeners and timers
		super.destroy();
		
		// Destroy chart instance
		if(this._calChart){
			this._calChart.destroy();
			this._calChart = null;
		}
		
		// Clean jQuery references
		this._$serialNumber = null;
		this._$profilePath = null;
		this._$propName = null;
		this._$calResult = null;
		this._$unitSerialPort = null;
		this._$btnInfo = null;
		this._$toolVal = null;
		this._$unitVal = null;
		this._$stepVal = null;
		this._$maxVal = null;
		this._$numberOfEntries = null;
		this._$btnStart = null;
		this._$btnCopy = null;
		this._$modal = null;

		// Clean object references
		this._payload = null;

		// Clean properties
		this._tableName = null;
		this._prefix = null;
		this._port = null;
		this._calIntervalID = null;
		this._stack = null;
		this._powerChanges = null;
		this._readingPacket = null;
		this._startToolValue = null;
		this._stopValue = null;
		this._defaultToolVal = null;

		console.log('Converter instance destroyed');
	}
}
function startButtonText($btn, start){
	if(start)
		$btn.text('Continue').removeClass('btn-outline-warning').addClass('btn-outline-success')
	else
		$btn.text('Stop').removeClass('btn-outline-success').addClass('btn-outline-warning')
}
export function setFromCookies($input, id){
	const spCookies = Cookies.get(id);
	if(spCookies){
		$input.val(spCookies);
	}
}
