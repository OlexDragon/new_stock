// Prologix

// Mode
$('.prologix-buton').click(function(){

	var prologixComPorts = $('#prologixComPorts').val();

	if(prologixComPorts){


		var hostName = getHostName();
		if(!hostName) return;

		var index = this.dataset.commandIndex;
		var commands = JSON.parse(this.dataset.commands);
		var getAnswer = JSON.parse(this.dataset.getAnswer);

		var toSend = {}
		toSend.hostName = hostName;
		toSend.spName = prologixComPorts;
		toSend.commands = [{command: commands[index], getAnswer: getAnswer[index]}];

		if(!getAnswer[index]){
			$.each(getAnswer, function(i, get){

				if(!get)
					return;

				var c = {};
				c.command = commands[i];
				c.getAnswer = true;
				toSend.commands.push(c);
			});
		}

		sendPrologixCommands(toSend, responseToButton);
	}
});

function responseToButton(response){

	var a = response.answer;
	if(!a) return;

	var command = response.command;
	var $button = $('#' + command.replace('++', ''));

	if(!$button.length)
		return;

	var answer = $.trim(String.fromCharCode.apply(String, a));
	$button[0].dataset.commandIndex = answer;

	var index = parseInt(answer);

	var dataMessage = $button[0].dataset.message;
	var text = JSON.parse(dataMessage)[index];
	$button.text(text);

	var title = JSON.parse($button[0].dataset.title)[index];
	$button.prop('title', title);

	var dataClasses = $button[0].dataset.classes;
	$.each(JSON.parse(dataClasses), function(i, c){
		if(i==index)
			$button.addClass(c);
		else
			$button.removeClass(c);
	});
}

function sendPrologixCommands(commands, responseProcessing){

	var json = JSON.stringify(commands);

	$.ajax({
		url: '/serial_port/rest/send',
		type: 'POST',
		contentType: "application/json",
		data: json,
        dataType: 'json'
    })
	.done(function(data){

		$.each(data.commands, function(index, c){
			responseProcessing(c);
		});
	})
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

$('#prologixSetAddress').click(function(){

	var prologixComPorts = $('#prologixComPorts').val();
	if(!prologixComPorts) return;

	var hostName = getHostName();
	if(!hostName) return;


	var toSend = {};

	var $prologixAddress = $('#prologixAddress');
	var addr = $.trim($prologixAddress.val());

	if(addr){

		toSend = getToSend('["++addr ' + addr + '"]', false);

		var c = {};
		c.command = '++addr';
		c.getAnswer = true;
		toSend.commands.push(c);

	}else
		toSend = getToSend('["++addr"]', true);


	sendPrologixCommands(toSend, setAddress);
});

function setAddress(response){

	var a = response.answer;
	if(!a) return;

	var answer = $.trim(String.fromCharCode.apply(String, a));
	$('#prologixAddress').val(answer).trigger("input");
}

$('#prologixAddress').on('input', function(){

	var value = $.trim(this.value);
	var $prologixSetAddress = $('#prologixSetAddress');

	if(value){
		$prologixSetAddress.text('Set');
	}else{
		$prologixSetAddress.text('Get');
	}
});

$('#prologixGetAll').click(function(){

	var toSend = getToSend(this.dataset.commands, true);
	if(!toSend) return;

	sendPrologixCommands(toSend, function(response){

		var a = response.answer;
		if(!a) return;

		if(response.command=='++addr')
			setAddress(response);
		else
			responseToButton(response);
	});
});

$('#prologixSetDefault').click(function(){

	var toSend = getToSend(this.dataset.commands, false);
	if(!toSend) return;

	sendPrologixCommands(toSend, function(response){});
	$('#prologixGetAll').trigger('click');
});

function getToSend(commands, getAnswer){

	var prologixComPorts = $('#prologixComPorts').val();
	if(!prologixComPorts) return;

	var hostName = getHostName();
	if(!hostName) return;

	var toSend = {};
		toSend.hostName = hostName;
		toSend.spName = prologixComPorts;
		toSend.commands = [];

	var commands = JSON.parse(commands);

	$.each(commands, function(index, command){
		var c = {};
		c.getAnswer = getAnswer;
		c.command = command;
		toSend.commands.push(c);
	});

	return toSend;
}