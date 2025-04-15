
let $outputComPorts		 = $('#outputComPorts');
let $outputButtons		 = $outputComPorts.parents('.accordion-body').find('button').filter((i,el)=>!el.classList.contains('tool-btn'));
let $outputToolAddress 	 = $('#outputToolAddress');
let $outputTool			 = $('#outputTool');
let $outputToolValue	 = $('#outputToolValue');
let $outputToolAuto		 = $('#outputToolAuto');

prologixElements($outputComPorts, $outputToolAddress, $outputButtons);

let $outputGet = $('#outputGet').click(()=>outputGet());

function outputGet(action){

	$outputToolValue.val('');

	const outputComPorts = $outputComPorts.val();
	if(!outputComPorts){
		if(action){
			var data = {};
			data.error = 'The Serial Port is not selected.';
			action(data);
		}else{
			console.log("The Serial Port is not selected.");
			alert('The Serial Port is not selected.');
		}
		return;
	}

	const toolCommands = $outputTool.val();
	if(!toolCommands){
		if(action){
			var data = {};
			data.error = 'Tool not selected.';
			action(data);
		}else{
			console.log("Tool not selected.");
			alert('Tool not selected.');
		}
		return;
	}

	const toolAddress = $outputToolAddress.val();
	if(!toolAddress){
		if(action){
			var data = {};
			data.error = 'Type the Tool Address.';
			action(data);
		}else{
			console.log("Type the Output Tool Address.");
			alert('Type the Tool Address.');
		}
		return;
	}

	const toSend = {}
	toSend.spName = outputComPorts;
	toSend.commands = [];
	toSend.addr = toolAddress;
	const notNiGPIB = toSend.spName!='NI GPIB';
	if(notNiGPIB)
		toSend.commands.push({command: '++addr ' + toSend.addr, getAnswer: false});

	const isAouto = $outputToolAuto[0].dataset.commands.includes('++auto 0');// Prologix is in AUTO mode
	$.each(toolCommands.split(','), (i, command)=>{
		var c = {};
		c.command = command;
		c.getAnswer = isAouto;
		toSend.commands.push(c);
	});

	if(notNiGPIB && !isAouto)
		toSend.commands.push({command: '++read eoi', getAnswer: true}); // READ id prologix mode isn't AUTO

	toSend.timeout = 10000;

	toSend.commands[toSend.commands.length-1].getAnswer = true;
	console.log(toSend);
	if(action)
		sendPrologixCommands(toSend, action);
	else{
		$('#outputValue').text('');
		sendPrologixCommands(toSend, outputAction);
	}
}
function outputAction(data){

	if(!data.getAnswer){
		return;
	}

	const toFixed = dataToValue(data);
	if(toFixed)
		$outputToolValue.val(toFixed);
}
function dataToValue(data){

	if(!data.answer){
		let title = 'Communication problem.'
		let message = 'The Output Tool did not respond..';
		console.log(title + ' : ' + message);
		showToast(title, message, 'text-bg-danger');
		return;
	}

	if(typeof dataProcessing !== 'undefined')
		dataProcessing(data);

	var answer = $.trim(String.fromCharCode.apply(String, data.answer));

	var s = answer.split(/\s+/);
	var a;
	switch(s.length){
	case 1:
		a = parseFloat(answer);
		break;
	case 2:
		a = parseFloat(s[1]);
		break;
	default:
		a = parseFloat(s[s.length-2]);
	}

	if(!a){
		let title = 'Parser Error.'
		let message = 'The value "' + a + '" cannot be parsed, or is out of range.';
		console.log(title + ' : ' + message);
		showToast(title, message, 'text-bg-danger');
		return;
	}

	return a.toFixed(1);
}