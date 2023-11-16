
var $outputComPorts = $('#outputComPorts');
var $outputTool 	= $('#outputTool');
var $ouputAddress 	= $('#ouputAddress');
var outputComPorts;
var hostName;
var outputTool
var address;

$('#outputGet').click(()=>outputGet());

function outputGet(){

	outputComPorts = $outputComPorts.val();
	if(!outputComPorts)
		return;

	hostName = getHostName();
	if(!hostName) return;

	outputTool = $outputTool.val();
	if(!outputTool){
		alert('Tool not selected.');
		return;
	}

	toolAddress = $ouputAddress.val();
	if(!toolAddress){
		alert('Type the Tool Address.');
		return;
	}

	let toSend = {}
	toSend.hostName = hostName;
	toSend.spName = outputComPorts;
	toSend.commands = [];
	toSend.commands.push({command: '++addr ' + toolAddress, getAnswer: false})

	$.each(outputTool.split(','), function(index, command){

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

		if(typeof dataProcessing !== 'undefined')
			dataProcessing(data);

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