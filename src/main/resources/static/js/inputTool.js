$('.input-buton').click(function(){

	let inputComPorts = $('#inputComPorts').val();
	if(!inputComPorts)
		return;

	let hostName = getHostName();
	if(!hostName) return;

	let address = $('#inputAddress').val();

	if(!address){
		alert('Type the Tool Address.');
		return;
	}

// Get Command
	let commands = $('#inputTool').val();

	if(!commands){
		alert('Tool not selected.');
		return;
	}

	let command = commands.split(' ')[this.value];
	let valueID = this.id.replace('Btn', '')
	let $valueField = $('#' + valueID);
	let $unit = $('#' + valueID + 'Unit');
	let divider = $unit.find('option:selected').attr('data-divider');
	let getAnswer = true;

	let value = $valueField.val();
	if(value){
		value = ' ' + value;
		let unit = $unit.val()
		getAnswer = false;
		if(unit)
			value += ' ' + unit;
	}else
		value = '?';

	command += value;

	let $button = $(this);
// Prepare the data to send
	let toSend = {}
	toSend.hostName = hostName;
	toSend.spName = inputComPorts;
	toSend.commands = [];
	toSend.commands.push({command: '++addr ' + address, getAnswer: false})
	toSend.commands.push({command: command, getAnswer: getAnswer});

// Send data
	let json = JSON.stringify(toSend);
	$.ajax({
		url: '/serial_port/rest/send',
		type: 'POST',
		contentType: "application/json",
		data: json,
        dataType: 'json'
    })
	.done(data=>inputAction(data, $valueField, divider, $button))
	.fail(function(error) {
		if(error.statusText!='abort'){
		var responseText = error.responseText;
			if(responseText)
				alert(error.responseText);
			else
				alert("Server error. Status = " + error.status)
		}
	});
});

function inputAction(data, $valueField, divider, $button){
	let commands = data.commands.filter(c=>c.getAnswer);

	if(!commands.length)
		return;

	if(!divider)
		 divider = 1;
	else
		divider = parseInt(divider);

	let ansver = String.fromCharCode.apply(null, commands[0].answer);
	let fl = parseFloat(ansver);
	$valueField.val(fl/divider);
	$valueField.trigger('change');
 	$button.text('Set');
}