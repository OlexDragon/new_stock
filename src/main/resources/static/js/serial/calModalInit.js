
let $btnStart;
let $btnInfo;
let $toolVal;
let $stepVal;
let $maxVal;
let $unitSerialPort;
let $unitVal;
let $calResult;
let $propName
let $btnCopy;

let calChart;

let prefix;
let commandIndex;
let defaultToolVal;

const BTN_START = true;
const BTN_STOP = !BTN_START;

let calIntervalID;
let startToolValue;
let f_toRun = function(){
			f_stop();
			console.log('The f_toRun function is not defined;');
			alert('The f_toRun function is not defined;');
		 };
let confirmStart = ()=>true;
function setConfirmStart(cs){
	if(cs)
		confirmStart = cs;
	else
		confirmStart = ()=>true;
}
let f_init;
function init(subInit){

	if(subInit)
		f_init = subInit;
	if(f_init)
		f_init();
	$btnCopy	 = $('#btnCopy').click(()=>selectAndCopy($calResult[0]));
	$propName	 = $('#propName');
	$calResult	 = $('#calResult');
	$btnStart	 = $('#btnStart');
	$btnInfo	 = $('#btnInfo');
	$toolVal	 = $('#toolVal');
	$stepVal	 = $('#stepVal');
	$maxVal		 = $('#maxVal');
	$unitSerialPort =  $('#unitSerialPort');
	$unitVal	 = $('#unitVal');
	calChart	 = getChart();
	prefix		 = $('.modal-dialog').data('prefix');
	commandIndex = $('.modal-dialog').data('commandIndex');
	defaultToolVal = $('.modal-dialog').data('defaultToolVal');
	startToolValue = undefined;

	if($unitSerialPort.children().length>1)
		return;

	gerSerialPorts(ports=>{
		if(!ports){
			console.warn('It looks like the Serial Port Server is down.');
			alert('It looks like the Serial Port Server is down.');
			return;
		}
		$('<option>', {selected: 'selected', disabled: 'disabled', hidden: 'hidden', title:'Remote Serial Port.', text:'Select Remote Serial Port.'}).appendTo($unitSerialPort);
		$.each(ports, (i, portName)=>{
			$('<option>', {value: portName, text: portName}).appendTo($unitSerialPort);
		});
		setFromCookies();
	});
	$btnInfo.click(getInfo);
	$unitSerialPort.change(e=>{
		if(e.currentTarget.value && e.currentTarget.value != 'Select Remote Serial Port.'){
			$btnInfo.removeClass('disabled');
			getInfo();
			setCookies(e.currentTarget);
		}else
			$btnInfo.addClass('disabled');
	});
	$btnStart.click(()=>{

		// Stop Button
		if($btnStart.text()==='Stop'){
			f_stop();
			$btnInfo.removeClass('disabled');
			return;
		}
		if($btnStart.text()=='Reset'){
			calChart.reset();
			$numberOfEntries.text(x.length);
			$btnStart.text('Restart');
			$btnInfo.text('Optimize');
			return;
		}
		if($btnStart.text()==='Restart'){
			clear();
			$toolVal.val(startToolValue);
			$btnInfo.text('Clear')
		}

		if(!confirmStart())
			return;

		$btnInfo.addClass('disabled');
		$toolVal.prop('readonly', true);
		$stepVal.prop('readonly', true);

		// Check Serial Port, Tool Addreass and Input Tool Type
		const toSend = getToSendIT(commandIndex);
		if(!checkInputTool(toSend)){
			$modal.modal('hide');
			$collapseInput.show();
			return;
		}

		f_toolValue();
		if($btnStart.text()==='Start')
			startToolValue = $toolVal.val();
		startButtonText(BTN_STOP);
		f_run();
	});
	$toolVal.keypress(e=>{
		if(e.which == 13){  // the enter key code
			f_toolValue();
			setTimeout(()=>$toolVal.val($inputPower.val()), 300);
		}
	})
	.change(()=>$btnStart.text('Start'));
	$modal.on('hidden.bs.modal', f_stop);
	$stepVal.on('focusout', e=>setCookies(e.currentTarget));
	$maxVal.on('focusout', e=>setCookies(e.currentTarget));

	$modal.modal('show');
}
function setRun(run){
	if(run)
		f_toRun = run;
	else
		f_toRun = function(){
			f_stop();
			console.log('The f_run function is not defined;');
			alert('The f_run function is not defined;');
		 };
}
function clear(){
	x.length = y.length = 0;
	calChart.update();
	if(f_init)
		f_init();
}
function setFromCookies(){
	const comPort = $unitSerialPort[0];
	fromCookies(comPort);
	comPort.dispatchEvent(new Event('change'));
	const step = $stepVal[0]
	fromCookies(step);
	const max = $maxVal[0];
	fromCookies(max);
}
function setCookies(element){
	if(element.value)
		Cookies.set(prefix + element.id, element.value, { expires: 365, path: '' });
}
function fromCookies(element){
	const val = Cookies.get(prefix + element.id);
	if(val)
		element.value = val;
}
function getInfo(){
	if($btnInfo.text()=='Clear'){
		clear();
		$btnStart.text('Restart');
		$btnInfo.text('Info');
		$toolVal.val(startToolValue);
		return;
	}
	if($btnInfo.text()=='Optimize'){
		const parse = parseFloat($toolVal.val());
		calChart.optimize(parse);
		$btnStart.text('Reset');
		$btnInfo.text('Save');
		$numberOfEntries.text(x.length);
		showTable();
		$btnCopy.prop('disabled', false);
		return;
	}
	if($btnInfo.text()=='Save'){
		saveToProfile();
		return;
	}
	const command = getCommand(new Packet());
	sendCommand(command, showInfo);
}
function showTable(){

	$calResult.empty();
	const propName = $propName.val();

	for(let i=0;i<x.length; i++){
		const line = propName + ' ' + x[i] + ' ' + y[i];
		$calResult
		.append(
			$('<dive>', {class: 'row', text: line})
		);
	}
}
 function saveToProfile(){
   	if(!x.length){
   		console.log('There is nothing to save.');
   		alert('There is nothing to save.');
	    return;
	}

	$btnInfo.addClass('disabled');

	const table = {serialNumber: serialNumber, name: tableName};
	const array = [];
	for(let i=0; i<x.length; i++)
		array.push({input: y[i], output: x[i]});

	table.values = array;
	postObject('/calibration/rest/profile/save', table)
	.done(function(data){
		console.log(data.content);
		alert(data.content);
		$btnInfo.removeClass('disabled').text('Info');
		$modal.modal('hide');
	})
	.fail(conectionFail);
}
function getCommand(packet, getAnswer){

	const hostName = getHostName();
	if(!hostName){
		console.log("Unable to get HTTP Serial Port Hostname.");
		alert('Unable to get HTTP Serial Port Hostname.');
		return;
	}

	const command = {};
	command.hostName = hostName;

	const spName = $unitSerialPort.val();
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
	command.getAnswer = typeof getAnswer === "undefined" || getAnswer;
	return command;
}
function showInfo(command){

	if(command.errorMessage){
		console.warn(command.errorMessage + '; ' + command);
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
	command = getCommand(acknowledgement, false);
	sendCommand(command, ()=>{});

	const sn = packets[0].getData(deviceInfo.serialNumber);
	if(sn.length){
		$serialNumber.text(sn[0]);
		$btnStart.removeClass('disabled');
		$('.modal-title').text(tableName + 'r: ' + sn[0]);
		$btnInfo.text('Clear');
		serialNumber = sn[0];
		$profilePath.prop('href', '/calibration/rest/profile/path?sn=' + sn);
	}else
		$btnStart.addClass('disabled');

	const dscr = packets[0].getData(deviceInfo.description);
	if(sn)
		$('#unit_description').children('h4').text(dscr);
}
var sendCommandCount = 0;
function sendCommand(command, action){

	++sendCommandCount;

	const url = command.toSend ? '/serial_port/rest/send-bytes' : '/serial_port/rest/read-bytes';
	postObject(url, command)
	.done(data=>{

		--sendCommandCount;

		action(data);
	})
	.fail(function(error) {

		--sendCommandCount;

		if(error.statusText!='abort'){
			var responseText = error.responseText;
			if(responseText)
				alert(error.responseText);
			else
				alert("Server error. Status = " + error.status)
		}
	});
}
let v_stop = true;;
let runTimeout = 1000;
function f_run(run){
	v_stop = false;
	if(run) setRun(run);
	clearInterval(calIntervalID);
	calIntervalID = setInterval(f_toRun, runTimeout);
}
function f_stop(){
	v_stop = true;;
	console.warn('f_stop()');
	clearInterval(calIntervalID);
	$toolVal.prop('readonly', false);
	$stepVal.prop('readonly', false);
	$btnInfo.removeClass('disabled');
	startButtonText(BTN_START);
	calIntervalID = null;
}
function startButtonText(start){
	if(start)
		$btnStart.text('Continue').removeClass('btn-outline-warning').addClass('btn-outline-success')
	else
		$btnStart.text('Stop').removeClass('btn-outline-success').addClass('btn-outline-warning')
}
function f_toolValue(){
	
	if($toolVal.val())
		$inputPower.val($toolVal.val());
	else
		$inputPower.val('');

	$inputPowerBtn.click();
}
function reset(){
	if(startToolValue)
		$toolVal.val(startToolValue);
	else
		$toolVal.val(defaultToolVal);
	f_toolValue();
}
function read(action){
  const command = getCommand();
  sendCommand(command, action);
}
function base64ToBytes(command){

	if(!command){
		console.warn('command is undefined');
		return 0;
	}
 	if(command.errorMessage){
 		f_stop();
		console.warn(command.errorMessage + '; ' + JSON.stringify(command));
		alert(command.errorMessage);
		return;
	}

	const answer = typeof command == 'string' ? command : command.answer;
   	if(answer){
 		const bytes = [];
  		const a  = atob(answer);
  		for (var i = 0; i < a.length; i++) {
  			bytes.push(a.charCodeAt(i));
  		}
  		return bytes;
  	}else{
  		console.warn("There is nothing to decode.");
  		return 0;
  	}
}
function commandToRegister(command){
	if(!command){
		console.warn('The parameter command is undefined.');
		return 0;
	}
	const bytes = base64ToBytes(command);
	if(!bytes){
		console.warn('base64ToBytes returns 0 bytes.');
		return 0;
	}
	const sentPacket = parsePackets(base64ToBytes(command.toSend));
	if(!sentPacket){
		console.warn('parsePackets returns 0 packets.');
		return 0;
	}
	return toIrtRegister(bytes, sentPacket[0].header.packetId);
}
function sendPacket(action, register, value){
	let type;
	let timeout;
	if(typeof value === 'undefined'){
		type = packetType.request;
		timeout = 100;
	}else{
		type = packetType.command;
		register.value = value;
		timeout = 500;
	}

  	const packet = new Packet(new Header(type, undefined, packetGroupId.deviceDebug), new Payload(new Parameter(PARAMETER_READ_WRITE), register.toBytes()));
  	const command = getCommand(packet);
  	command.timeout = timeout;
  	sendCommand(command, action);
}