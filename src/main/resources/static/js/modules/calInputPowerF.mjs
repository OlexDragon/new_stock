import {$ipStart, $ipComPorts, $ipTool, $ipStep, $ipMax, $ipInfo, reset, ipStop} from './calInputPowerE.mjs';
import {ipChart, x, y, clear} from './calInputPower.mjs';
const $ipUnit = $('#ipUnit');

function fromCookies(element){
	const val = Cookies.get(element.id);
	if(val)
		element.value = val;
}
function setCookies(element){
	if(element.value)
		Cookies.set(element.id, element.value);
}
function getInfo(){
	if($ipInfo.text()=='Clear'){
		clear();
		$ipStart.text('Restart');
		return;
	}
	if($ipInfo.text()=='Optimize'){
		optimize();
		return;
	}
	if($ipInfo.text()=='Save'){
		saveToProfile();
		return;
	}
	const command = getCommand(new Packet());
	sendCommand(command, showInfo);
}
function read(action){
	const command = getCommand();
	sendCommand(command, action ? action : showInfo)
}
function getCommand(packet){

	const hostName = getHostName();
	if(!hostName){
		console.log("Unable to get HTTP Serial Port Hostname.");
		alert('Unable to get HTTP Serial Port Hostname.');
		return;
	}

	const command = {};
	command.hostName = hostName;

	const spName = $ipComPorts.val();
	if(!hostName){
		console.log("The serial port is not selected.");
		alert('The serial port is not selected.');
		return;
	}
	command.spName = spName;
	command.baudrate = 'BAUDRATE_115200';
	if(packet){
		const bytes = packetToSend(packet);
		command.toSend = bytes;
	}
	command.termination = 126;
	command.getAnswer = true;
	return command;
}
let oldX, oldY;
function optimize(){
	oldX = [...x];
	oldY = [...y];
	let start = $ipTool.val();
	if(start)
		start = parseFloat(start);
	else
		start = -60;
	x.length = y.length = 0;
	for(let i=0; i<oldX.length; i++){
		if(oldX[i]<start)
			continue;
		const index = y.length-1;
		if(!y.length || y[index]<oldY[i]){
			if(y.length>2){
				const firstX = x[y.length-2];
				const midleX = x[index];
				const lastX = oldX[i];
				const timesX1 = midleX - firstX;
				const timesX2 = lastX - firstX;

				const firstY = y[y.length-2];
				const midleY = y[index];
				const lastY = oldY[i];
				let timesY1 = ((midleY - firstY)/timesX1);
				let timesY2 = ((lastY - firstY)/timesX2);
				if(timesY1>10 || timesY2>10){
					timesY1 = timesY1.toFixed(0);
					timesY2 = timesY2.toFixed(0);
				}else if(timesY1>1 || timesY2>1){
					timesY1 = timesY1.toFixed(1);
					timesY2 = timesY2.toFixed(1);
				}else{
					timesY1 = timesY1.toFixed(2);
					timesY2 = timesY2.toFixed(2);
				}
				if(timesY1 == timesY2){
					x[index] = lastX;
					y[index] = lastY;
					continue;
				}
			}
			x.push(oldX[i]);
			y.push(oldY[i]);
		}
	}
	$ipStart.text('Reset');
	$ipInfo.text('Save');
	ipChart.update();
}
 function chartReset(){
	x.length = y.length = 0;
	x.push(...oldX);
	y.push(...oldY);
	$ipStart.text('Restart');
	$ipInfo.text('Optimize');
	ipChart.update();
 }
function sendCommand(command, action){

	const url = command.toSend ? '/serial_port/rest/send-bytes' : '/serial_port/rest/read-bytes';
	const json = JSON.stringify(command);
	$.ajax({
		url: url,
		type: 'POST',
		contentType: "application/json",
		data: json,
        dataType: 'json'
    })
	.done(action)
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
function showInfo(command){

	if(command.errorMessage){
		alert(command.errorMessage);
		return;
	}
	if(!command.getAnswer){
		alert("The command has been sent.");
		return;
	}
	const answer = command.answer;
	const bytes = [];
	if(answer){
		const a  = atob(answer);
		for (var i = 0; i < a.length; i++) {
			bytes.push(a.charCodeAt(i));
	    }
	}else{
		alert("It's impossible to get an answer.");
		return;
	}

	const packets = parsePackets(bytes);
	if(!packets.length){
		console.warn('Something went wrong. Answer: ' + answer);
		alert('Something went wrong.');
		return;
	}
	if(packets.length && packets[0].header.type==packetType.acknowledgement)
		packets.shift();

	if(!packets.length){
		read();
		return;
	}
	// Send Acknowledgement
	const acknowledgement = packets[0].getAcknowledgement();
	command = getCommand(acknowledgement);
	sendCommand(command, ()=>{});

	const sn = packets[0].getData(deviceInfo.serialNumber);
	if(sn.length){
		$serialNumber.text(sn[0]);
		$ipStart.removeClass('disabled');
		$('.modal-title').text('Input Power: ' + sn[0]);
		$ipInfo.text('Clear');
		unitSerialNumber = sn[0];
	}else
		$ipStart.addClass('disabled');

	const dscr = packets[0].getData(deviceInfo.description);
	if(sn)
		$('#unit_description').children('h4').text(dscr);
}
function setFromCookies(){
	const comPort = document.getElementById('ipComPorts');
	fromCookies(comPort);
	comPort.dispatchEvent(new Event('change'));
	fromCookies(document.getElementById('ipStep'));
	fromCookies(document.getElementById('ipMax'));
}
const BTN_START = true;
const BTN_STOP = !BTN_START;
function startButtonText(start){
	if(start)
		$ipStart.text('Start').removeClass('btn-outline-warning').addClass('btn-outline-success')
	else
		$ipStart.text('Stop').removeClass('btn-outline-success').addClass('btn-outline-warning')
}
function readSetToolValue(){
	
	if($ipTool.val())
		$inputPower.val($ipTool.val());
	else
		$inputPower.val('');

	$inputPowerBtn.click();

}
function ipRun(){
	return setInterval(run, 1000);
}
function run(){
	const toolV = $inputPower.val();
	if(!toolV){
		console.warn('No Input Tool Value.');
		return;
	}
	if($ipTool.val())
		$ipTool.val(toolV);
	if(x.length && x[x.length-1] == toolV){
		nextStep();
		return;
	}
	x.push(parseFloat(toolV).toFixed(1));
	const packet = new Packet(new Header(packetType.request, undefined, packetGroupId.deviceDebug), new Payload(new Parameter(PARAMETER_READ_WRITE), DATA_FCM_ADC_INPUT_POWER));
	const command = getCommand(packet);
	sendCommand(command, inputPower);
	nextStep();
}
function nextStep(){
	let tmp = $ipMax.val();
	let max;
	if(tmp)
		max = parseFloat(tmp);
	else
		max = 0;
	const toolV = parseFloat($inputPower.val());
	if(toolV == max){
		ipStop();
		$ipStart.text('Restart')
		$ipInfo.removeClass('disabled').text('Optimize');
		reset();
		alert('Calibration completes.');
		return;
	}
	tmp = $ipStep.val();
	let step;
	if(tmp)
		step = parseFloat(tmp);
	else{
		step = 1;
		$ipStep.val(step);
	}

	let newValue = (toolV + step).toFixed(1);
	if(newValue>max)
		newValue = max;

	$ipTool.val(newValue);
	readSetToolValue();
}
function inputPower(command){

	if(command.errorMessage){
		ipStop();
		alert(command.errorMessage);
		return;
	}
	const answer = command.answer;
	const bytes = [];
	if(answer){
		const a  = atob(answer);
		for (var i = 0; i < a.length; i++) {
			bytes.push(a.charCodeAt(i));
	    }
	}else{
		ipStop();
		alert("It's impossible to get an answer.");
		return;
	}

	const packets = parsePackets(bytes);
	if(!packets.length){
		console.warn('Something went wrong. Answer: ' + answer);
		alert('Something went wrong.');
		return;
	}
	if(packets.length && packets[0].header.type==packetType.acknowledgement)
		packets.shift();

	if(!packets.length){
		read(inputPower);
		return;
	}
	// Send Acknowledgement
	const acknowledgement = packets[0].getAcknowledgement();
	command = getCommand(acknowledgement);
	sendCommand(command, ()=>{});

	if(packets[0].header.error){
		ipStop();
		alert(PACKET_ERROR[packets[0].header.error]);
		return;
	}
	if(x.length>y.length){
		const register =  packets[0].getData();
		y.push(register.value);
		$ipUnit.val(register.value);
		ipChart.update();
	}
 }
 function saveToProfile(){
   	if(!x.length){
   		alert('There is nothing to save.');
	    return;
	}

	$ipInfo.addClass('disabled');

	const table = {serialNumber: unitSerialNumber, name: 'FCM Input Power'};
	const array = [];
	for(let i=0; i<x.length; i++)
		array.push({input: y[i], output: x[i]});

	table.values = array;
	const json = JSON.stringify(table);
   	$.ajax({
		url: '/calibration/rest/to_profile',
		type: 'POST',
		contentType: "application/json",
		data: json,
		dataType: 'json'
	})
	.done(function(data){
		alert(data.content);
		$ipInfo.removeClass('disabled').text('Info');
		$modal.modal('hide');
	})
	.fail(conectionFail);
}
export {setFromCookies, setCookies, getInfo, ipRun, startButtonText, readSetToolValue, chartReset, BTN_START, BTN_STOP};