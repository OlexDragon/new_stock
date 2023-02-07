
$('#outputGet').click(()=>outputGet());

function outputGet(){

	let outputComPorts = $('#outputComPorts').val();
	if(!outputComPorts)
		return;

	let hostName = getHostName();
	if(!hostName) return;

	let commands = $('#outputTool').val();
	let address = $('#ouputAddress').val();

	if(!commands){
		alert('Tool not selected.');
		return;
	}

	if(!address){
		alert('Type the Tool Address.');
		return;
	}

	let toSend = {}
	toSend.hostName = hostName;
	toSend.spName = outputComPorts;
	toSend.commands = [];
	toSend.commands.push({command: '++addr ' + address, getAnswer: false})

	$.each(commands.split(','), function(index, command){

		var split = command.split(':');

		var c = {};
		c.getAnswer = split[1] == 'true';
		c.command = split[0];
		toSend.commands.push(c);
	});

	let json = JSON.stringify(toSend);
	let $outputValue = $('#outputValue').text('');

	$.ajax({
		url: '/serial_port/rest/send',
		type: 'POST',
		contentType: "application/json",
		data: json,
        dataType: 'json'
    })
	.done(data=>outputAction(data))
	.fail(function(error) {
		if(error.statusText!='abort'){
		var responseText = error.responseText;
			if(responseText)
				alert(error.responseText);
			else
				alert("Server error. Status = " + error.status)
		}
	});
}

function outputAction(data){

	$.each(data.commands, function(index, command){

		if(!command.getAnswer || !command.answer)
			return;

		var answer = $.trim(String.fromCharCode.apply(String, command.answer));

		var s = answer.split(/\s+/);
		var a;
		if(s.length>1)
			a = parseFloat(s[1]).toFixed(1);

		else
			a = parseFloat(answer).toFixed(1);

		$('#outputValue').text(a);
	});
}