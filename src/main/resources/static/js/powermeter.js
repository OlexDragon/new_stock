
var cookie = Cookies.get("outputPowermeter")
if(cookie){
	try {
		$('#outputPowermeter option').filter(function () { return $(this).text() == cookie; }).prop('selected', true);
	}catch(err) {}
}
$('#outputPowermeter').change(function(){
	var text = $('#outputPowermeter option:selected').text();
	Cookies.set("outputPowermeter", text, { expires: 999 })
});

$('#ouputGet').click(function(){

	var outputComPorts = $('#outputComPorts').val();
	if(!outputComPorts) return;

	var hostName = getHostName();
	if(!hostName) return;

	var copPort = $('#outputComPorts').val();
	var commands = $('#outputPowermeter').val();
	var address = $('#ouputAddress').val();

	if(!(copPort && commands && address)){
		alert('Fill all the fields.');
		return;
	}

	var toSend = {}
	toSend.hostName = hostName;
	toSend.spName = outputComPorts;
	toSend.commands = [];

	$.each(commands.split(','), function(index, command){

		var split = command.split(':');

		var c = {};
		c.getAnswer = split[1] == 'true';
		c.command = split[0];
		toSend.commands.push(c);
	});

	var json = JSON.stringify(toSend);
	var $outputValue = $('#outputValue').text('');

	$.ajax({
		url: '/serial_port/rest/send',
		type: 'POST',
		contentType: "application/json",
		data: json,
        dataType: 'json'
    })
	.done(function(data){

		$.each(data.commands, function(index, command){

			if(!command.getAnswer || !command.answer)
				return;

			var answer = $.trim(String.fromCharCode.apply(String, command.answer));

			var s = answer.split(/\s+/);
			var a;
			if(s.length>1)
				a = parseFloat(s[1]);
			else
				a = parseFloat(answer);

			$outputValue.text(a);
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
});