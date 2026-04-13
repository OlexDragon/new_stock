import {unitSerialNumber} from '../../calibration.mjs';
import ModalLoader from '../modal-loader.mjs';
import getPrincipal from '../../principal.mjs';
import {serialNumber} from '../cards/onrender-card.mjs'


export default class PowerTable extends ModalLoader{

	static $pdPmDetValue;

	#$pdAuto;
//	#$pdClear;
//	#$btnSavePD;
	#$rows;

	constructor(){
		super(`/calibration/btr/pd?sn=${unitSerialNumber}`);
	}
	_onLoad(){
		this.#$pdAuto = this._$modal.find('#pdAuto');
//		this.#$pdClear = 
		this._$modal.find('#pdClear').click(this.#pdClearClick.bind(this));
//		this.#$btnSavePD = 
		this._$modal.find('#btnSavePD').click(btnSavePDClick);
		this.#$rows = this._$modal.find('.modal-body .row');

		PowerTable.$pdPmDetValue = this._$modal.find('#pdPmDetValue').click(pdPmDetValueClick);
		$inputs = this.#$rows.find('input').keyup(onKeyUp).focus(onInputFocus);

		fillFromCookies(this._cookiesName);
	}
	#pdClearClick(){
		if(!confirm('Are you sure you want to delete all data?'))
			return;
		this.#$rows.find('input').val('');
	}
	_onHide(){

		let toCookies = {};
		$inputs.filter((_,el)=>el.value).each((_,el)=>{
			toCookies[el.id] = el.value;
		});
		if(Object.keys(toCookies).length){
			var json = JSON.stringify(toCookies);
			Cookies.set(this._cookiesName, json, { expires: 7, path: '' });
		}
	}
}
let $inputs;
let notShift;
function onKeyUp({originalEvent:{code}, currentTarget:{id}}){

	if(notShift)
		return;

	const array = id.split('.');

	switch(code){

	case "ArrowDown":
		++array[0];
		$inputs.filter((_,el)=>el.id===array.join('.')).focus();
		break;

	case "ArrowUp":
		--array[0];
		$inputs.filter((_,el)=>el.id===array.join('.')).focus();
		break;

	case "ArrowRight":
		++array[2];
		$inputs.filter((_,el)=>el.id===array.join('.')).focus();
		break;

	case "ArrowLeft":
		--array[2];
		$inputs.filter((_,el)=>el.id===array.join('.')).focus();
		break;

	case "ControlLeft":
	case "ControlRight":
		notShift = true;
		setTimeout(()=>notShift=false, 10000);
		break;

	case "Enter":
	case "NumpadEnter":
		PowerTable.$pdPmDetValue.click();
	}
}

let selectedField;
function onInputFocus({currentTarget:el}){
	selectedField && (selectedField.style.backgroundColor = "");
	selectedField = el;
	selectedField.style.backgroundColor = "yellow";
}
function pdPmDetValueClick(){

	if(selectedField)
		selectedField.focus();

	$.get('/calibration/rest/monitorInfo', {sn: serialNumber})
	.done(data=>{
		if(!data.data.outpower){
			alert('Something went wrong.');
			return;
		}
		if(!selectedField){
			alert('Select one of the text fields.');
			return;
		}
		selectedField.value = data.data.outpower;
		const array = selectedField.id.split('.');
		++array[0];
		$inputs.filter((_,el)=>el.id===array.join('.')).focus();
	});

}
async function btnSavePDClick(e) {

    const principal = await getPrincipal();
    if (typeof principal === "string") {
        alert("You must log in.");
        return;
    }
    if (!confirm("Are you sure you want to save the data to the database?"))
        return;

    e.currentTarget.disabled = true;

    const powerDetector = {};
    powerDetector.serialNumberId = parseInt(serialNumber.replace(/\D/g, ''));
    powerDetector.userId = principal.principal.user.id
    powerDetector.measurement = {};

    $('.modal-body').find('input').each((_, el) => {
        if (!powerDetector.measurement[el.dataset.power])
            powerDetector.measurement[el.dataset.power] = {};
        powerDetector.measurement[el.dataset.power][el.id] = el.value;
    });
    postObject('/btr/rest/pd/save', powerDetector)
        .done(function(data) {

            if (data.error) {
                alert(data.error);
                return;
            }
            console.log(data)
        })
        .fail(function(error) {
            if (error.statusText != 'abort') {
                var responseText = error.responseText;
                if (responseText)
                    alert(error.responseText);
                else
                    alert("Server error. Status = " + error.status)
            }
        });
}
function fillFromCookies(cookiesName){

		if(!$inputs.length)
		return;

	if(!$inputs.filter((_,el)=>el.value).length){
		let cookies = Cookies.get(cookiesName);
		if(cookies){
			const o = JSON.parse(cookies);
			Object.keys(o).forEach(k=>{
				$inputs.filter((_,el)=>el.id===k).val(o[k]);
			});
			setTimeout(()=>alert('Note: Values ​​are taken from cookies.\nTo save the values ​​to the database, click the "Save" button.'),100);
		}
	}

}