
const $inputComPorts		 = $('#inputComPorts');
const $inputButtons			 = $inputComPorts.parents('.accordion-body').find('button').filter((i,el)=>!el.classList.contains('tool-btn'));
const $inputToolAddress 	 = $('#inputToolAddress');
const $inputPowerUnit		 = $('#inputPowerUnit');
const $inputToolAuto		 = $('#inputToolAuto');
const $inputFrequency		 = $('#inputFrequency');
const $inputFrequencyUnit	 = $('#inputFrequencyUnit');
const $collapseInput		 = $('#collapseInput');
const $inputPower			 = $('#inputPower');
const $inputOutputBtn		 = $('#inputOutputBtn');
const $inputPowerBtn		 = $('#inputPowerBtn');

let $inputTool			 = $('#inputTool')
.change(function(){
	let powerUnit =  $(this).find('option:selected').data("powerUnit");
	$inputPowerUnit.val(powerUnit);
})
.trigger('change');

// Control Input 
$('.input-value').on('input', e=>{
	let btnID = e.currentTarget.dataset.for;
	if(e.currentTarget.value)
		$(btnID).text('Set');
	else
		$(btnID).text('Get');
})
.change(e=>$(`#${e.currentTarget.id}Btn`).click())
.on('mouseenter', e=>{

	if(!e.ctrlKey || e.currentTarget.localName != 'input' || $toastContainer.html().trim())
		return;

	showSetupToast(e.currentTarget)
})
.on( "focusin", _=>{
	const $toast = $('.toast');
	if($toast.length)
		$toast.remove();
})
.each((_,el)=>{

	if(el.localName != 'input')
		return;

	let step = Cookies.get(el.id + "Step");
	el.dataset.step = step;
})
.keydown(e=>{

	const el = e.currentTarget;

	if(e.originalEvent.code === 'Space'){
		e.preventDefault();
		if(e.ctrlKey){
			let another = getAnother(el);
			if(another)
				another.focus();
		}
		return;
	}

	if(el.localName != 'input')
		return;

	let step;
	if(el.dataset.step === 'undefined')
		step = 1;
	else
		step = el.dataset.step ? parseFloat(el.dataset.step) : 1;

	switch(e.originalEvent.code){

	case 'ArrowRight':

		if(!e.ctrlKey)
			return;

		if(showStepValue(e))
			break;

		if(step<0.1)
			return;

		el.dataset.step = step / 10;
		showStepValue(e);
		break;

		case 'ArrowLeft':

			if(!e.ctrlKey)
				return;

			if(showStepValue(e))
				break;

			if(step>10)
				return;

			el.dataset.step = step * 10;
			showStepValue(e);
			break;

		case 'NumpadAdd':
		case 'Equal':

			if(!(e.ctrlKey || e.shiftKey))
				brack;

			if(showStepValue(e))
				break;

			if(e.ctrlKey)
				el.dataset.step = step * 2;

			else
				el.dataset.step = newStep(step, true);
				

			showStepValue(e);
		break;

		case 'NumpadSubtract':
		case 'Minus':

			if(!(e.ctrlKey || e.shiftKey))
				brack;

			if(showStepValue(e))
				break;

			if(e.ctrlKey)
				el.dataset.step = (step / 2).toFixed(2);

			else
				el.dataset.step = newStep(step).toFixed(2);

			if(el.dataset.step < 0.01)
				el.dataset.step = 0.1;

			showStepValue(e);
		break;

	case 'Backspace':

		if(!e.ctrlKey)
			return;

		if(showStepValue(e))
			break;

		if(el.dataset.step.length > 1)
			el.dataset.step = parseFloat(el.dataset.step.slice(0,-1));

		if(el.dataset.step < 0.01)
			el.dataset.step = 0.1;

		showStepValue(e);
		break;

	case 'Numpad0':
	case 'Numpad1':
	case 'Numpad2':
	case 'Numpad3':
	case 'Numpad4':
	case 'Numpad5':
	case 'Numpad6':
	case 'Numpad7':
	case 'Numpad8':
	case 'Numpad9':
	case 'Digit0':
	case 'Digit1':
	case 'Digit2':
	case 'Digit3':
	case 'Digit4':
	case 'Digit5':
	case 'Digit6':
	case 'Digit7':
	case 'Digit8':
	case 'Digit9':

		if(!e.ctrlKey)
			return;

		if(showStepValue(e))
			break;

		el.dataset.step = el.dataset.step + e.originalEvent.key;

		showStepValue(e);
		break;

	case 'ArrowUp':
		if(el.value=='')
			break;

		if(e.shiftKey && e.ctrlKey){
			e.preventDefault();
			changeAnother(el, 'ArrowUp');
			return;
		}

		doStep(el, 'ArrowUp');
		break;

	case 'ArrowDown':
		if(el.value=='')
			break;

		if(e.shiftKey && e.ctrlKey){
			e.preventDefault();
			changeAnother(el, 'ArrowDown');
			return;
		}

		doStep(el, 'ArrowDown');
		break;

	default:
		return;
	}

	e.preventDefault();
});

function newStep(step, add){

	const sing = add ? 1 : -1;
	let diminutive;

	if(compareStep(step, 100, add))
		diminutive = 100 * sing;

	else if(compareStep(step, 10, add))
		diminutive = 10 * sing;

	else if(compareStep(step, 1, add))
		diminutive = 1 * sing;

	else if(compareStep(step, 0.1, add))
		diminutive = 0.1 * sing;

	else
		diminutive = 0.01 * sing;

	return step + diminutive;
}
function compareStep(step, toCompare, add){

	if(add)
		return step >= toCompare;
	else
		return step > toCompare
}
function doStep(el, arrow){
	switch(arrow){

		case 'ArrowUp':
			el.value = (parseFloat(el.value) + parseFloat(el.dataset.step)).toFixed(2);
			$(`#${el.id}Btn`).click()
			break;

		case 'ArrowDown':
			el.value = (parseFloat(el.value) - parseFloat(el.dataset.step)).toFixed(2);
			$(`#${el.id}Btn`).click()
	}
}
function changeAnother(el, arrow){

	let another = getAnother(el);
	if(!another)
		return;

	switch(arrow){

		case 'ArrowUp':
			doStep(another[0], 'ArrowUp');
			break;

		case 'ArrowDown':
			doStep(another[0], 'ArrowDown');
	}	
}
function getAnother(el){
	return el.id == 'inputPower' ? $inputFrequency : el.id == 'inputFrequency' ? $inputPower : null;
}
function showStepValue(e){

	let $toast = $('.toast');
	if($toast.length){
		$toast.find('input').val(e.currentTarget.dataset.step);
		return false;	// Been open
	}

	showSetupToast(e.currentTarget);
	return true; 	// Open
}
prologixElements($inputComPorts, $inputToolAddress, $inputButtons);
let externalInputAction;
function inputAction(data, $valueField, divider){

	if(!data.getAnswer)
		return;

	let answer = String.fromCharCode.apply(String, data.answer).trim();
	if(!answer){
		let title = 'No Answer.'
		let message = 'No Answer from the Input Tool.';
		console.log(title + ' : ' + message);
		showToast(title, message, 'text-bg-danger');
		return;
	}
	let fl = parseFloat(answer);
	$valueField.val(fl/divider);
	$valueField.trigger('input');
	if(externalInputAction)
		externalInputAction($valueField);
}
function showSetupToast(target){

	let $input = $('<input>', {type: 'number', class: 'form-control', style: 'text-align:center;', value: target.dataset.step !== 'undefined' ? target.dataset.step : 1})
	.on('focusout', e=>{

		let step = e.currentTarget.value;
		if(!step)
			return;

		target.dataset.step = step;
		Cookies.set(target.id + "Step", step, { expires: 9999, path: '' });
	});

	let $toast = $('<div>', {class: 'toast', role: 'alert', 'aria-live': 'assertive', 'aria-atomic': true, 'data-bs-delay': 5*60*1000})
		.append(
			$('<div>', {class: 'toast-header'})
			.append(
				$('<strong>', {class: 'me-auto', text: target.title + ' Step'})
			)
			.append(
				$('<button>', {class: 'btn-close', type: 'button', 'data-bs-dismiss': 'toast', 'aria-label': 'Close'})
			)
		)
		.append(
			$('<div>', {class: 'toast-body'})
			.append($input)
		)
	.appendTo($toastContainer)
	.on('hide.bs.toast', function(){this.remove();});

	new bootstrap.Toast($toast).show();
}

$('.input-tool-buton').click(e=>{

	if(!$inputTool.val()){
		alert('Input Tool Is not Selected.');
		return;
	}
	const valueID = e.currentTarget.id.replace('Btn', '');
	const $valueField = $('#' + valueID);
	const value = $valueField.val();
	const $unit = $('#' + valueID + 'Unit');
	const unit = $unit.val();

	const toSend = getToSendIT(e.currentTarget.value, value, unit);
	if(!checkInputTool(toSend))
		return;

	let divider = 1;
	if(valueID.includes('Freq')){
		const $optionSelected = $inputTool.find('option:selected');
		divider = parseInt($inputFrequencyUnit.find('option:selected').attr('data-divider'));
		let factor = $optionSelected.data('freqFactor');
		divider /= factor;
	}

// Send data
	sendPrologixCommands(toSend, data=>inputAction(data, $valueField, divider));	// See calibration.js
});
function getToSendIT(commandIndex, value, unit){

	if(!commandIndex)
		throw new Error("The command index must be set..");

	// Prepare the data to send
	const toSend = {}
	toSend.spName = $inputComPorts.val();
	toSend.commands = [];
	toSend.addr = $inputToolAddress.val();
	const notNiGPIB = toSend.spName!='NI GPIB';
	if(notNiGPIB)
		toSend.commands.push({command: '++addr ' + toSend.addr, getAnswer: false});

	const $optionSelected = $inputTool.find('option:selected');
	const toolCommands = $inputTool.val();
	let getAnswer = true;
	let command = toolCommands.split(' ')[commandIndex];
	if(value){
		value = ' ' + value;
		getAnswer = false;
		if(unit){
			let tName = $optionSelected.text();
			if(tName=='ANRITSU68047C')
				unit = unit.replace('Z', '');
			value += ' ' + unit;
		}
		command += value;
	}else{
		let format = $optionSelected.attr('data-read-format');
		command = format.replace('{V}', command);
	}
	const isAouto = !notNiGPIB || $inputToolAuto.attr('data-commands').includes('++auto 0');// Prologix is in AUTO mode
	toSend.commands.push({command: command, getAnswer: isAouto ? getAnswer : isAouto});

	if(!isAouto && getAnswer)
		toSend.commands.push({command: '++read eoi', getAnswer: true}); // READ id prologix mode isn't AUTO

	return toSend;
}
function checkInputTool(toSend){

	if(!toSend.spName){
		console.log("The Serial Port is not selected.");
		alert('The Serial Port is not selected.');
		return false;
	}

	let toolCommands = $inputTool.val();
	if(!toolCommands){
		console.log("Input Tool not selected.");
		alert('Tool not selected.');
		return false;
	}

	if(!toSend.addr){
		console.log("Type the Input Tool Address.");
		alert('Type the Tool Address.');
		return false;
	}
	return true;
}
function toolOutputPower(action, value){

	if(value === undefined)
		value = '';
	else if(typeof value === 'number')
		value = value.toFixed(1);

	const unit = $inputPowerUnit.val();
	const toSend = getToSendIT($inputPowerBtn.val(), value, unit);
	sendPrologixCommands(toSend, data=>{

		if(!data.getAnswer)
			return;

		let answer = String.fromCharCode.apply(String, data.answer).trim();
		action(parseFloat(answer));
	});
}
