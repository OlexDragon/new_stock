// Prologix

function prologixElements($comPorts, $toolAddress, $buttons){

	$buttons.click(e=>{

		let prologixComPorts = $comPorts.val();
		let toSend = getToSend(prologixComPorts, e.currentTarget.dataset.commands, e.currentTarget.dataset.getAnswer);

		if(!toSend)
			return;

		sendPrologixCommands(toSend, function(response){

			if(!response.getAnswer){
				if(response.command.startsWith('++')){
					let split = response.command.split(' ');
					if(split.length==2){
						let toSend = getToSend(prologixComPorts, '["' + split[0] + '"]', true);
						sendPrologixCommands(toSend, response=>responseToButton(e.currentTarget, response));
					}
				}
				return;
			}

			var answer = response.answer;

			if(!answer){
				console.log("No Answer.");
				alert('No Answer.');
				return;
			}

			if(response.command=='++addr')
				setAddress($toolAddress, response);
			else
				responseToButton(e.currentTarget, response);
		});
	});
	
}
function setAddress($address, response){

	let answer = getAnswer(response);
	if(answer){
		$address.val(answer).trigger("input");

		let $parent = $address.parents('.accordion-body');
		setAccordionHeaderText($parent);
	}
}
function responseToButton(button, response){

	let $parent = $(button).parent();
	let $toolBtns = $parent.parent().find('.tool-btn').addClass('disabled');

	let answer = getAnswer(response);
	if(!answer)
		return;

	let command = response.command;
	if(!command){
		let title = 'Wrong Response.'
		let message = JSON.stringify(response);
		console.log(title + ' : ' + message);
		showToast(title, message, 'text-bg-danger');
		return;
	}
	let $button = $parent.find(command.replace('++', '.btn-'));

	if(!$button.length)
		return;

	let index = parseInt(answer);
	if(index>2){
		let title = 'Wrong Index.'
		let message = 'index=' + index;
		console.log(title + ' : ' + message);
		showToast(title, message, 'text-bg-danger');
		return;
	}

	let dataMessage = $button.data('message');
	$button.text(dataMessage[index]);

	var title = $button.data('title')[index];
	$button.prop('title', title);

	let t = $button.data();
	let commandsToChoose = $button.data('commandsToChoose');
	let c = commandsToChoose[index];
	$button.attr('data-commands', JSON.stringify([c])).attr('data-get-answer', false);

	let dataClasses = $button.data('classes');
	$button.removeClass('btn-outline-secondary').removeClass(dataClasses).addClass(dataClasses[index]);

	let $btns = $parent.find('.btn-prologix');
	let $filter = $btns.filter('.btn-success');
	if($btns.length == $filter.length)
		$toolBtns.removeClass('disabled');
}
function getAnswer(response){

	if(!response.getAnswer)
		return null;

	let answer = response.answer;

	if(!answer){
		let title = 'No Answer.'
		let message = 'No Answer from the Output Tool.';
		console.log(title + ' : ' + message);
		showToast(title, message, 'text-bg-danger');
		return null;
	}

	answer = $.trim(String.fromCharCode.apply(String, answer));

	if(!answer || answer.split('\n').length>1){
		let title = 'Wrong Answer.'
		console.log(title + ' : ' + answer);
		showToast(title, answer, 'text-bg-danger');
		return;
	}

	if(answer == 'Unrecognized command'){

		let title = 'Unrecognized command.'
		let message = 'The Prologix MODE must be CONTROLLER.';
		console.log(title + ' : ' + message);
		showToast(title, message, 'text-bg-info');
		return null;
	}

	return answer;
}

let $address = $('.address').on('input', function(){

	let value = $.trim(this.value);
	let $toolBtns = $(this).parents('.accordion-body').find('.tool-btn');
	
	let $button = $(this).parent().children('button');
	let d = $button.data('getAnswer');

	if(value){
		$button.text('Set').attr('data-get-answer', false).attr('data-commands', `["++addr ${value}"]`);
		$toolBtns.removeClass('disabled');
	}else{
		$button.text('Get').attr('data-get-answer', true).attr('data-commands', '["++addr"]');
		$toolBtns.addClass('disabled');
	}
})
.focusout(function(){
	if(this.value)
		Cookies.set(this.id, this.value, { path: '' });

	let $parent = $(this).parents('.accordion-body');
	setAccordionHeaderText($parent);
});
$.each($address, function(index, addr){
	let cookie = Cookies.get(addr.id)
	if(cookie){
		addr.value = cookie;
		$button = $(addr).parent().children('button').attr('data-get-answer', false).attr('data-commands', `["++addr ${cookie}"]`);
	}
});

function getToSend(prologixComPorts, commands, getAnswer){

	if(!prologixComPorts){
		console.log('Serial Port is not selected.');
		alert('Serial Port is not selected.');
		return;
	}

	if(!commands){
		console.log("The GPIB Command is not set.");
		alert('The GPIB Command is not set.');
		return;
	}

	var hostName = getHostName();
	if(!hostName){
		console.log("Unable to get hostname.");
		alert('Unable to get hostname.');
		return;
	}

	var toSend = {};
		toSend.hostName = hostName;
		toSend.spName = prologixComPorts;
		toSend.commands = [];

	var commands = JSON.parse(commands);

	$.each(commands, function(index, command){
		let c = {};
		c.getAnswer = getAnswer;
		c.command = command;
		toSend.commands.push(c);
	});

	return toSend;
}
$('.accordion-collapse').on('show.bs.collapse', e=>{
	let $parent = $(e.currentTarget);
	let comPort = $parent.find('.com-ports').val();
	if(comPort){
		let $btns = $parent.find('.btn-prologix');
		$btns.not('.btn-outline-secondary')
		.each((i,el)=>{
			let commands = JSON.parse(el.dataset.commands);
			let c = [];
			$.each(commands, function(index, command){
				let t = command.split(' ')[0];
				c.push(t);
			});
			$(el).data('commands', JSON.stringify(c)).data('getAnswer', true);
		});
		$btns.click();
	}
});
