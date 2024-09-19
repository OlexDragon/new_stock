
const $inputComPorts		 = $('#inputComPorts');
const $inputButtons			 = $inputComPorts.parents('.accordion-body').find('button').filter((i,el)=>!el.classList.contains('tool-btn'));
const $inputToolAddress 	 = $('#inputToolAddress');
const $inputPowerUnit		 = $('#inputPowerUnit');
const $inputToolAuto		 = $('#inputToolAuto');
const $inputFrequency		 = $('#inputFrequency');
const $inputFrequencyUnit	 = $('#inputFrequencyUnit');
const $collapseInput		 = $('#collapseInput');
const $inputPower			 = $('#inputPower');
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

	if(!e.ctrlKey || e.currentTarget.localName != 'input' || $toastContaner.html().trim())
		return;

	showSetupToast(e.currentTarget)
})
.each((i,el)=>{

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
		step = parseFloat(el.dataset.step);

	switch(e.originalEvent.code){

	case 'ArrowRight':
		if(step<0.1 || !e.ctrlKey)
			return;

		el.dataset.step = step / 10;
		showStepValue(e);
		break;

	case 'ArrowLeft':
		if(step>10 || !e.ctrlKey)
			return;

		el.dataset.step = step * 10;
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
		return;
	}else
		showSetupToast(e.currentTarget);
}
prologixElements($inputComPorts, $inputToolAddress, $inputButtons);
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
	.appendTo($toastContaner)
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
