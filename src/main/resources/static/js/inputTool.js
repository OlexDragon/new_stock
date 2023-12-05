
let $inputComPorts		 = $('#inputComPorts');
let $inputButtons		 = $inputComPorts.parents('.accordion-body').find('button').filter((i,el)=>!el.classList.contains('tool-btn'));
let $inputToolAddress 	 = $('#inputToolAddress');
let $inputPowerUnit		 = $('#inputPowerUnit');
let $inputToolAuto		 = $('#inputToolAuto');
let $inputFrequencyUnit	 = $('#inputFrequencyUnit');

let $inputTool			 = $('#inputTool')
.change(function(){
	let powerUnit =  $(this).find('option:selected').data("powerUnit");
	$inputPowerUnit.val(powerUnit);
})
.trigger('change');

$('.input-value').on('mouseenter', e=>{

	if(!e.ctrlKey || e.currentTarget.localName != 'input' || $toastContaner.html().trim())
		return;

	sowSetupToast(e.currentTarget)
})
.each((i,el)=>{

	if(el.localName != 'input')
		return;

	let step = Cookies.set(el.id + "Step");
	el.setAttribute("step", step);
});

prologixElements($inputComPorts, $inputToolAddress, $inputButtons);

$('.input-tool-buton').click(function(){

	let inputComPorts = $inputComPorts.val();
	if(!inputComPorts){
		console.log("The Serial Port is not selected.");
		alert('The Serial Port is not selected.');
		return;
	}

	let toolCommands = $inputTool.val();
	if(!toolCommands){
		console.log("Input Tool not selected.");
		alert('Tool not selected.');
		return;
	}

	let address = $inputToolAddress.val();

	if(!address){
		console.log("Type the Input Tool Address.");
		alert('Type the Tool Address.');
		return;
	}

	let command = toolCommands.split(' ')[this.value];
	if(!command){
		let title = 'Not Supported.'
		let message = 'This command is not supported.';
		console.log(title + ' : ' + message);
		showToast(title, message, 'text-bg-danger');
		return;
	}

	let $optionSelected = $inputTool.find('option:selected');
	let valueID = this.id.replace('Btn', '');
	let $valueField = $('#' + valueID);
	let $unit = $('#' + valueID + 'Unit');
	let getAnswer = true;

	let value = $valueField.val();
	if(value){
		value = ' ' + value;
		let unit = $unit.val();
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

	let $button = $(this);
// Prepare the data to send
	let toSend = {}
	toSend.spName = inputComPorts;
	toSend.commands = [];
	toSend.commands.push({command: '++addr ' + address, getAnswer: false})
	let isAouto = $inputToolAuto.attr('data-commands').includes('++auto 0');// Prologix is in AUTO mode
	toSend.commands.push({command: command, getAnswer: isAouto ? getAnswer : false});

	if(!isAouto && getAnswer)
		toSend.commands.push({command: '++read eoi', getAnswer: true}); // READ id prologix mode isn't AUTO

	let divider = 1; 
	if($button.prop('id').includes('Freq')){
		divider = parseInt($inputFrequencyUnit.find('option:selected').attr('data-divider'));
		let factor = $optionSelected.data('freqFactor');
		divider /= factor;
	}

// Send data
	sendPrologixCommands(toSend, data=>inputAction(data, $valueField, divider, $button));
});

function inputAction(data, $valueField, divider, $button){

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
	$valueField.trigger('change');
 	$button.text('Set');
}
function sowSetupToast(target){

	let $input = $('<input>', {type: 'number', class: 'form-control', style: 'text-align:center;', value: target.step ? target.step : 1})
	.on('focusout', e=>{

		let step = e.currentTarget.value;
		if(!step)
			return;

		target.setAttribute("step", step);
		Cookies.set(target.id + "Step", step, { expires: 9999 });
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