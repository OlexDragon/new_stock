import {unitSerialNumber, $menuPowerOffset} from '../../calibration.mjs';
import calInfo from '../fetch/calibration-info.mjs';
import {signIn} from '../sign-in.mjs';
import {show as showToast} from '../../toast-worker.mjs';
import PowerLUT from './calculate/power-lut.mjs';
import TableValidator from './calculate/table-validater.mjs';
import {ajaxJSON, connectionFail} from '../fetch/fetch.mjs';
import SaveConfig from './save-config.mjs';

export default class OutputPowerBUC extends SaveConfig{

	#interval;
	#busy;
	#lut;
	#noCalInfoCount = 0;

	constructor($modal){
		super($modal.find('#dropdownSettingsButton'), 'outputpower');
		this._$modal = $modal;
		this._$modalBody = $modal.find('#modal-body');
		this._$name		 = $modal.find('#name');
		this._$input	 = $modal.find('#opInputValue');
		this._$out		 = this.on($modal.find('#op_outputValue'), 'keypress', ({which})=>{if(which === 13) this.#onAddRow();});
		this._$minValue	 = this.on($modal.find('#minValue'), 'blur', ({currentTarget:{value}})=>this._$out.val(value));
		this._$maxValue	 = $modal.find('#maxValue');
		this._$btnAddRow = this.on($modal.find('#addRow'), "click", this.#onAddRow.bind(this));
		this._$btnSave	 = this.on($modal.find('#save'), "click", this.#btnSaveClick.bind(this));
		this._$btnCalc	 = this.on($modal.find('#calc'), "click", ()=>this.#lut.addNextRow((input, output)=>{this._addRow(input, output);this.#disableButtons();}));
		this._$btnClear	 = this.on($modal.find('#clear'), "click", this.#clear.bind(this));
		this._$step		 = this.on($modal.find('#step'), "focusout", stepFocusou);

		this.#lut		 = new PowerLUT(this._$modalBody, this._$out, this._$maxValue);

		const table = Cookies.get(unitSerialNumber + "_opTable");
		if(table){
			var array = JSON.parse(table);
			array.forEach( row => this._addRow(row.input, row.output));
			this.#disableButtons();
		}
		const ops = Cookies.get("outputPowerSteps");
		if(ops)
			this._$step.val(ops);

	}
	_addRow(inputValue, outputValue){
		const $row = $('<div>', {class: 'row'})
		.append($('<div>', {class: 'col input'}).text(inputValue))
		.append($('<div>', {class: 'col output'}).text(outputValue))
		.append($('<div>', {class: 'col-auto'}).append($('<button>', {class: 'btn btn-link'}).text('Remove').click(()=>this.#removeRow($row))))
		.appendTo(this._$modalBody);
	}
	#disableButtons(){

		const length = this._$modalBody.children().length;

		this._$btnClear.prop('disabled', length===0);

		const max = this._$maxValue.val();
		const out = this._$modalBody.children().last().find('.output').text();

		this._$btnCalc.prop('disabled', out>=max || length<2);
		if(length>1)
			this._$btnSave.prop('disabled', out<max);
		return length;
	}
	start(){
		clearInterval(this.#interval);
		this.#busy = false;
		this.#run();
		this.#interval = interval = setInterval(this.#run.bind(this), 2000);
	}
	stop(){
		this.#interval = clearInterval(this.#interval);
	}
	onShow(){
		this.start();
	}
	onHide(){
		this.stop();
	}
	destroy() {
	   	this.stop();

		if (this.#lut?.destroy)
			this.#lut.destroy();

		super.destroy();

	        // Null references
		this._$modal = null;
		this._$modalBody = null;
		this._$name = null;
		this._$input = null;
		this._$out = null;
		this._$minValue = null;
		this._$maxValue = null;
		this._$btnAddRow = null;
		this._$btnSave = null;
		this._$btnCalc = null;
		this._$btnClear = null;
		this._$step = null;

		this.#lut = null;
	}
	#run(){
		if(this.#busy){
			console.log(this.constructor.name, ' is busy');
			showToast('Busy, ', 'It takes longer than usual to retrieve data from the unit.', 'text-bg-warning')
			return;
		}
		this.#busy = true;

		calInfo.get(this.#callBack.bind(this));
	}
	#callBack(calInfo){
		this.#busy = false;
		if(!calInfo){
			this.#noCalInfoCount++;
			console.warn('No calibration info retrieved');
			if(this.#noCalInfoCount>=5){
				showToast('No calibration info', 'Unable to retrieve calibration info from the unit.', 'text-bg-danger');
				this._$modal.modal('hide');
			}
			return;
		}
		this.#noCalInfoCount = 0;
		if(calInfo.error){
			console.want(calInfo.error);
			return;
		}
		if(!calInfo.bias){
			signIn(unitSerialNumber);
			this._$btnAddRow.prop('disabled', true);
			return;
		}

		const name = this._$name.val();
		if(calInfo.bias[name]){
			const value = calInfo.bias[name].value;
			const text = this._$input.text();
			if(!text || +text!==value)
				this._$input.text(value);
			this._$btnAddRow.prop('disabled', false);
		}else
			this._$btnAddRow.prop('disabled', true);
	}
	#onAddRow(){

		const inputValue = this._$input.text();
		const outputValue = this._$out.val();

		if(!(inputValue && outputValue)){
			alert('The input field cannot be empty.');
			return;
		}

		this._addRow(inputValue, outputValue);
		this.#disableButtons();

		const max = parseInt(this._$maxValue.val());
		const out = parseInt(outputValue);

		if(out>max){
			alert('The value in the table cannot be greater than the maximum set value.');
			return;
		}

		var step = parseInt(this._$step.val());
		var newVal = out + step;

		if(newVal >= max){
			newVal = max;
			alert('The value in the table has reached its maximum value.');
		}

		this._$out.val(newVal);

	}
//	#sortModalBody(){
//		const items = this._$modalBody.children().detach().sort((a, b) => {
//			const vA = Number(a.querySelector('.output').textContent);
//			const vB = Number(b.querySelector('.output').textContent);
//			return vA - vB;
//		});
//		this._$modalBody.append(items);
//	}
	#removeRow(row){
		row.remove();
		if(!this.#disableButtons())
	    	this._$out.val(this._$minValue.val())
	}
	#clear(){
		this._$modalBody.empty();
		this._$out.val(this._$minValue.val())
		this.#disableButtons();
	}
	#btnSaveClick(){
		const validator = new TableValidator(this._$modalBody);
		const result = validator.validate();

		if (!result.ok) {
			console.warn("There is an error in this table.", result);
		    alert("There is an error in this table.");
			return;
		}
		const table = { serialNumber: unitSerialNumber, name: 'Output Power', values: result.rows };
		ajaxJSON('/calibration/rest/profile/save', table)
		.done((data)=>{
			this._$modal.modal('hide');
			if(confirm(data.content + '\nDo you want to calibrate Power Offset by frequencies?'))
				$menuPowerOffset.click();
		})
		.fail(connectionFail);
	}
}
function stepFocusou({currentTarget:el}){

	if(el.value)
		Cookies.set("outputPowerSteps", el.value, { expires: 99999, path: '/calibration' });
	else{
		Cookies.remove("outputPowerSteps", { path: '/calibration' });
		el.value = 3;
	}

}
