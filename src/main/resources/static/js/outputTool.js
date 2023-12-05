
let $outputComPorts		 = $('#outputComPorts');
let $outputButtons		 = $outputComPorts.parents('.accordion-body').find('button').filter((i,el)=>!el.classList.contains('tool-btn'));
let $outputToolAddress 	 = $('#outputToolAddress');
let $outputTool			 = $('#outputTool');
let $outputToolValue	 = $('#outputToolValue');
let $outputToolAuto		 = $('#outputToolAuto');

prologixElements($outputComPorts, $outputToolAddress, $outputButtons);

let $outputGet = $('#outputGet').click(()=>outputGet());

function outputGet(){

	$outputToolValue.val('');

	let outputComPorts = $outputComPorts.val();
	if(!outputComPorts){
		console.log("The Serial Port is not selected.");
		alert('The Serial Port is not selected.');
		return;
	}

	let toolCommands = $outputTool.val();
	if(!toolCommands){
		console.log("Tool not selected.");
		alert('Tool not selected.');
		return;
	}

	let toolAddress = $outputToolAddress.val();
	if(!toolAddress){
		console.log("Type the Output Tool Address.");
		alert('Type the Tool Address.');
		return;
	}

	let toSend = {}
	toSend.spName = outputComPorts;
	toSend.commands = [];
	toSend.commands.push({command: '++addr ' + toolAddress, getAnswer: false}); // Set Tool Address

	let isAouto = $outputToolAuto[0].dataset.commands.includes('++auto 0');// Prologix is in AUTO mode
	$.each(toolCommands.split(','), function(index, command){
		var c = {};
		c.command = command;
		c.getAnswer = isAouto;
		toSend.commands.push(c);
	});

	if(!isAouto)
		toSend.commands.push({command: '++read eoi', getAnswer: true}); // READ id prologix mode isn't AUTO

	let $outputValue = $('#outputValue').text('');
	sendPrologixCommands(toSend, outputAction);
}
function outputAction(data){

	if(!data.getAnswer){
		return;
	}

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
	console.log(answer);

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

	let toFixed = a.toFixed(1)
	$outputToolValue.val(toFixed);
}