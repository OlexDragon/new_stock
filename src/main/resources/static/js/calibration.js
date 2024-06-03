$('#miCalibration').addClass('active');

const $modal = $('#modal').on('shown.bs.modal', e=>$('#sn').focus());
const $btrMeasurement	 = $('#btrMeasurement');
const $serialNumber		 = $('#serialNumber');
const $spServers		 = $('#spServers');
const $menuInputPower	 = $('#menuInputPower');

const sn = new URLSearchParams(window.location.search).get('sn');
if(sn && !sn.includes('.'))
	Cookies.set("rmaSearch", JSON.stringify(['rmaSerialNumber', sn]), { expires: 7});

const serialNumber = $serialNumber.text();

// Variables Used in modals
const urlCalibrationInfo = '/calibration/rest/calibrationInfo';
let loFrequencty;
let err;
let interval;
let table;
let cookies;
let testBy;
let buzy = false;
let $btnSave;
let $btnAddRow;
let $btnCalc;
let $step;
let $name;
let $input;
let $modalBody;
let $addRow;
let $menuPowerOffset = $('#menuPowerOffset');
let $base;
let $minValue;
let $out;
let $currentAlarm;
// X - axel
const options = {

						hour: "numeric",
						minute: "numeric",
						month: "short",
  						day: "numeric",
  						second: "numeric",
						hour12: false,
					};
const formater = new Intl.DateTimeFormat("en-US", options)
let xLabels = [];
let yPower = [];
let yTemperature = [];
let timestamp;
let chart;
let $lastPoint;
let chartRun;
let $st10;
let $REG_DIG_STARTUP;
let $REG_REF_STARTUP;
let $REG_RF_STARTUP;
let $REG_VCO_STARTUP;
let $REG_VCO_4V5_STARTUP;
let $REG_DIG_OCP;
let $REG_REF_OCP;
let $REG_RF_OCP;
let $REG_VCO_OCP;
let $REG_VCO_4V5_OCP;
let $LOCK_DET;
let $VCO_SEL_ST10;
let $WORD;
let $st3;
let $DBR_ST3;
let $PD;
let $CP_LEAK_x2;
let $CP_LEAK_x2_for;
let $CP_LEAK;
let $CP_LEAK_DIR;
let $DNSPLIT_EN;
let $PFD_DEL_MODE;
let $REF_PATH_SEL;
let $R;
let $st4;
let $CALB_3V3_MODE1;
let $RF_OUT_3V3;
let $EXT_VCO_EN;
let $VCO_AMP;
let $CALB_3V3_MODE0;
let $VCALB_MODE;
let $KVCO_COMP_DIS;
let $PFD_POL;
let $REF_BUFF_MODE;
let $MUTE_LOCK_EN;
let $LD_ACTIVELOW;
let $LD_PREC;
let $LD_COUNT;

// Get HTTP Serial Port Server from the cookies
const cookie = Cookies.get("spServers");
if(cookie){
	try {
		const $toSelect = $('option[value=' + cookie + ']');
		if($toSelect.length){
			$toSelect.prop('selected', true);
			$menuInputPower.removeClass('disabled');
		}
		gerSerialPorts(setToolsSerialPorts);
	}catch(err) {}
}

$spServers.change(function(){

	const spServers = $(this).val();

	if(!spServers){
		$menuInputPower.addClass('disabled');
		return;
	}

	$menuInputPower.removeClass('disabled');

	Cookies.set("spServers", spServers, { expires: 7, path: '' });
	gerSerialPorts(setToolsSerialPorts);
});

let $saveToCookies = $('.save-to-cookies')
.change(function(){
	Cookies.set(this.id, $(this).find('option:selected').text(), { expires: 7, path: '' });
});
$.each($saveToCookies, function(i, tool){
	let cookie = Cookies.get(tool.id)
	if(cookie){
		let $tool = $(tool);
		$tool.children().filter(function () { return $(this).text() == cookie; }).prop('selected', true);
	}
});
$('.tool').change(function(){
	let $parent = $(this).parent();
	setAccordionHeaderText($parent);
});

function gerSerialPorts(setSerialPorts){

	var spHost = $spServers.val();
	if(!spHost)
		return;

	$.post('/serial_port/rest/serial-ports', {hostName: spHost})
	.done(setSerialPorts)
	.fail(conectionFail);
}
function setToolsSerialPorts(ports){

	if(!ports){
		alert('It looks like the Serial Port Server is down.');
		return;
	}

	$('<option>', {selected: 'selected', disabled: 'disabled', hidden: 'hidden', title:'Remote Serial Port.'}).text('Select Remote Serial Port.').appendTo($comPorts);

	$.each(ports, function(i, portName){
		$('<option>', {value: portName}).text(portName).appendTo($comPorts);
	});

	$.each($comPorts, (i, select)=>{
		var value = Cookies.get(select.id)
		if(value){
			var $option = $(select).children('[value=' + value + ']');
			$option.prop('selected', true);
			comPortSelected(select);
			disableMenuItens();
		}
	});
}
const $comPorts = $('.com-ports').on('change', function(){

	comPortSelected(this);
	Cookies.set(this.id, this.value, { expires: 999, path: '' });

	disableMenuItens();
	$(this).parents('.accordion-collapse').trigger('show.bs.collapse');
});

function disableMenuItens(){

	let inputComPortValue = $('#inputComPorts').val();
	let isinputSelected = inputComPortValue && !inputComPortValue.startsWith('Select');

	let outputComPortValue = $('#outputComPorts').val();
	let outputIsSelected = outputComPortValue && !outputComPortValue.startsWith('Select');

	if(outputIsSelected)
		$('#menuAutoByInput').removeClass('disabled')
	else
		$('#menuAutoByInput').addClass('disabled')

	if(isinputSelected && outputIsSelected)
		$('#nemuAutoByGain').removeClass('disabled')
	else
		$('#nemuAutoByGain').addClass('disabled')
}

function comPortSelected(select){

	let $parent = $(select).parents('.accordion-body');
	setAccordionHeaderText($parent)
}

function setAccordionHeaderText($parent){

	let $port = $parent.find('.com-ports');
	let messageId = $port.attr('data-info-message');
	let $message = $('#' + messageId);

	let port = '';
	if($port.length && $port.val() && !$port.val().startsWith('Select'))
		port = $port.val();
	else{
		$message.text(' Serial Port is not selected');
		$message.addClass('text-danger');
		$parent.find('.to-disable').addClass('disabled');
		return;
	}

	$message.removeClass('text-danger');

	let $tool = $parent.find('.tool');
	let toolMessage = '';

	if(!$tool.length || $tool.val()){
		$parent.find('.to-disable').removeClass('disabled');
		if($tool.length) toolMessage = ', ' + $tool.find('option:selected').text();
	}else
		$parent.find('.to-disable').addClass('disabled');

	let $address = $parent.find('.address');
	let toolAdderss = '';
	if($address.length && $address.val())
		toolAdderss = ', Tool Address: ' + $address.val()

	$message.text(port + toolMessage + toolAdderss);
}
if(!serialNumber)
	new bootstrap.Modal('#modal').show();

var calibrateId = undefined;

// Show Calibration message
$('.calibrate').click(function(e){
	e.preventDefault();

	const id = e.target.id;

// Load for first time or when the serial number is changed 
	if(calibrateId!=id){

		if(this.href.slice(-1) == '#'){
			alert('Unable to connect to the Unit.');
			return;
		}

		$modal.off('.bs.modal');
		calibrateId = id;
		$modal.load(this.href, function(body,error){
			if(error=='error')
				alert('Unable to connect to the Unit.');
		});
	}else
		$modal.modal('show');
});

// Upload the profile
$('.upload').click(function(e){
	e.preventDefault();
	upload(this);
});

function upload(link){

	$.post(link.href)
	.done(function(data){
		alert(data);
	})
	.fail(conectionFail);
}

function moduleUpload(e, link){
	e.preventDefault();

	$.post(link.href)
	.done(function(data){
		alert(data);
	})
	.fail(conectionFail);
}

// Login to the unit
$('.unitLogin').click(function(e){
	e.preventDefault();
	var href = $(this).prop('href');
	loginWhithHref(href);
});

function login(){
	var href = $('#unitLogin').prop('href');
	loginWhithHref(href);
}

function loginFromScan(e, t){
	e.preventDefault();
	var href = $(t).prop('href');
	loginWhithHref(href);
}

function loginWhithHref(href){
	$.post(href)
	.done(function(data){
		let title = 'Login.'
		let message = data.content;
		console.log(title + ' : ' + message);
		showToast(title, message, 'text-bg-success');
	})
	.fail(conectionFail);
}

// Scan IP Addresses
var scanIpInterval;
const $scan = $('#scan');
$scan.click(function(e){
	e.preventDefault();
	calibrateId = 'Network Scan';

	$modal.empty();
	$modalBody = $('<div>', {class:'modal-body'});
	let $scanBtn = $('<button>', {id: 'scanBtn', type:'button', class: 'btn btn-primary'}).text('Stop');
	$modal
	.append(
		$('<div>', {class:'modal-dialog modal-lg'})
		.append(
			$('<div>', {class:'modal-content'})
			.append(
				$('<div>', {class:'modal-header'})
				.append($('<h5>', {id:'modal-header', class: 'modal-title ml-3 text-primary col'}).text('Scaning for online units.'))
				.append($('<input>', {type:'number', id: 'start-from', class: 'col-1', title: 'Start scan from value.'}))
				.append($('<button>', {type:'button', class: 'btn-close', 'data-bs-dismiss': 'modal', 'aria-label': 'Close'}))
			)
			.append($modalBody)
			.append(
				$('<div>', {class:'modal-footer'})
				.append($scanBtn)
				.append($('<button>', {type:'button', class: 'btn btn-secondary', 'data-bs-dismiss': 'modal'}).text('Close'))
			)
		)
	);

	$scanBtn.click(function(e){
		e.preventDefault();

		let $this = $(this);
		let text = $this.text();

		switch(text){

			case 'Stop':	ip = 250;break;

			case 'Restart':	$modal.modal('hide');
							setTimeout(()=>$scan.click(), 500);
		}
	});

	new bootstrap.Modal('#modal').show();

	if(typeof scanIpInterval !=='undefined')
		clearInterval(scanIpInterval);

	var $modalHeader = $('#modal-header');
	let $startFrom = $('#start-from');
	$startFrom.focusout(function(){
		let val = $startFrom.val();
		if(val)
			Cookies.set("startFrom",  val, { path: '' });
		else
			Cookies.set("startFrom",  '', { path: '' });
	});


	var ip = Cookies.get("startFrom")
	if(ip)
		$startFrom.val(ip);
	else{
		ip = $startFrom.val();
		if(!ip){
			ip = 2;
			$startFrom.val(ip);
		}
	}
	var index = 0;
	var maxIP = 240;

	scanIpInterval = setInterval(function() {

		if(ip>maxIP){
			// Stop IP scan
			clearInterval(scanIpInterval);

			var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
			tooltipTriggerList.map(function (tooltipTriggerEl) { return new bootstrap.Tooltip(tooltipTriggerEl)});
			$modalHeader.text('Scan completed.');
			$('#scanBtn').removeClass('btn-primary').addClass('btn-success').text('Restart');
			return;
		}

		var ipAddress = '192.168.30.' + ip;
		$modalHeader.text('Send Request for ' + ipAddress);

		$.post('/calibration/rest/scan', {ip : ipAddress})
		.done(function(hostname){

			if(hostname!=ipAddress)
				return;

			var $text = $('<div>', {class: 'col-sm'});
			var $row = $('<div>', {class: 'row'})

			$modalBody
			.append(
				$row
				.append(
					$('<div>', {class: 'col-auto'}).text(++index))
				.append(
					$('<div>', {class: 'col-sm'})
					.append(
						$('<a>', {href: 'http://' + ipAddress, target: "_blank"}).text(ipAddress)
					)
				)
				.append($text)
			);

			$.post('/calibration/rest/info', {ip : ipAddress})
			.done(function(info){

				if(!info)
					return;

				$text.append($('<a>', {href: 'http://' + ipAddress, target: "_blank"}).text(info["Serial number"]));
				$row
				.append(
					$('<div>', {class: 'col-auto'}).append($('<a>', { class: 'btn btn-sm btn-outline-dark', target: "_blank", href: '/calibration?sn=' + info["Serial number"]}).text('Calibrate'))
				)
				.append(
					$('<div>', {class: 'col-auto'}).append($('<a>', { class: 'btn btn-sm btn-outline-info', onclick: 'loginFromScan(event, this)', target: "_blank", href: '/calibration/rest/login?sn=' + info["Serial number"]}).text('Login'))
				);
				$row.attr('data-bs-toggle','tooltip').attr('data-bs-placement','top').attr('title', info["Product name"]);
			});
		});

		++ip;
	}, 700);

	$modal.on('hidden.bs.modal', function () {
		clearInterval(scanIpInterval);
	});
});

function getHostName(){

	var spServers = $spServers.val();
	if(spServers)
		return spServers;
	else
		alert('The Serial Port Server is not selected.');

	return null;
}

let $menuGain = $('#menuGain');
$('#dropdownCalibrateButton').on('show.bs.dropdown', function(){

	$menuGain.addClass('disabled list-group-item-light');

	if(!serialNumber)
		return;

	$.post('/calibration/rest/calibration_mode', { ip: serialNumber })
	.done(function(calMode){

		if(!calMode)
			calibrationModeError('calMode == null');

		var status = calMode["Calibration mode"];
		var $calMode = $('#calMode').removeClass('text-primary text-success');

		switch(status){

		case 'OFF':
			$calMode.addClass('text-primary').text('Calibration Mode: ' + status);
			$menuGain.text('Gain - Cal.Mode must be ON');
			break;

		case 'ON':
			$calMode.addClass('text-success').text('Calibration Mode: ' + status);
			$menuGain.removeClass('disabled list-group-item-light').text('Gain');
			break;

		default:
			$calMode.text('Calibration Mode');
		}
	})
	.fail(calibrationModeError);
});
function calibrationModeError(error) {

	if(error)
		console.error(error);

	$('#calMode').removeClass('text-primary text-success').text('Calibration Mode');
	alert('Unable to connect to Unit.');
}

$('#calMode').click(function(e){
	e.preventDefault();

	$.post('/calibration/rest/calibration_mode_toggle', { ip: serialNumber })
	.fail(conectionFail);
});


function hasValue(inputs){

	for(let s of inputs){
		if(s.value)
			return true;
	}

	return false;
}

function toArray($inputs){
	var values = [];
	$inputs.map((i, v)=>v.value).filter((i, v)=>v).map((i, v)=>parseFloat(v)).sort().each((i, v)=>values.push(v));
	return values;
}
const typeVersion = $('#typeVersion').val();
//Created for TROPOSCAT to see currents of output devices
$('#currents').click(function(e){
	e.preventDefault();

	if(!typeVersion){
		alert('Unoun type Device ID Vertion');
		return;
	}
	let href = `/calibration/currents?sn=${serialNumber}`;
	$modal.load(href);
})

$('#profile').click(function(e){
	getProfile(e, this);
 });

function getProfile(e, link){
	e.preventDefault();

	$.get(link.href)
	.done(function(profile){
		let $message = $('<div>', { class: "alert alert-light alert-dismissible fade show row", role: "alert"})
						.append($('<strong>', {class: 'col pre-line'}).text(profile))			
						.append($('<button>', {type: 'button', class: 'btn-close col-auto', 'data-bs-dismiss': 'alert', 'aria-label': 'Close'}));

		$('body').append($message);

		$message.get(0).scrollIntoView({behavior: 'smooth'});
	})
	.fail(function(error) {
		if(conectionFail(error))
			$('#calMode').removeClass('text-primary text-success').text('Calibration Mode');
	});
}

$('.profilePath').click(function(e){
	getProfilePath(e, this);
 });
 
 function getProfilePath(e, link){
 	e.preventDefault();
 
 	$.get(link.href)
	.done(function(path){

		let $message = $('<div>', { class: "alert alert-warning alert-dismissible fade show row", role: "alert"})
						.append($('<strong>', {class: 'col'}).text(path))			
						.append($('<button>', {type: 'button', class: 'btn col-auto copy', title: 'Copy to clipboard', 'aria-label': 'Copy to clipboard'}).text('Copy'))
						.append($('<button>', {type: 'button', class: 'btn-close col-auto', 'data-bs-dismiss': 'alert', 'aria-label': 'Close'}));
		$('body').append($message);

		$('.copy').click(function(){
			var strong = $(this).parent().children('strong')[0];
			selectAndCopy(strong);
		});

		$message.get(0).scrollIntoView({behavior: 'smooth'});
	})
	.fail(function(error) {
		if(conectionFail(error))
			$('#calMode').removeClass('text-primary text-success').text('Calibration Mode');
	});
 }
function selectAndCopy(element) {
    if (document.body.createTextRange) {
        var range = document.body.createTextRange();
        range.moveToElementText(element);
        range.select();
    } else if (window.getSelection) {
        var selection = window.getSelection();
        var range = document.createRange();
        range.selectNodeContents(element);
        selection.removeAllRanges();
        selection.addRange(range);
    } else {
        alert("Could not select text in node: Unsupported browser.");
        return;
    }
	document.execCommand('copy');
}
 
 function conectionFail(error) {
	if(error.statusText!='abort'){
		let responseText = error.responseText;
		if(responseText)
			alert(error.responseText);
		else{
			let status;
			switch(error.status){
			case 0: status = 'Connection Refused.';
					break;
			default: status = error.status;
		}
		alert("Server error. Status = " + status);
		}
		return true;
	}
	return false;
}
$('#gain_from_cookies').click(function(){

	let cName = this.dataset.sn;
	let gainTable = Cookies.get(cName);
	if(!gainTable || gainTable === undefined){
		alert('There are no gain tables saved on this computer.');
		return;
	}

	$modal.empty();
	var $tbody = $('<tbody>');
	let $copyBtn = $('<button>', {type:'button', class: 'btn btn-primary'}).text('Copy').click(function(){selectAndCopy($tbody[0]);});
	$modal
	.append(
		$('<div>', {class:'modal-dialog'})
		.append(
			$('<div>', {class:'modal-content'})
			.append(
				$('<div>', {class:'modal-header'})
				.append($('<h5>', {id:'modal-header', class: 'modal-title ml-3 text-primary col'}).text('Table stored on this computer.'))
				.append($('<button>', {type:'button', class: 'btn-close', 'data-bs-dismiss': 'modal', 'aria-label': 'Close'}))
			)
			.append($('<div>', {class:'modal-body'}).append($('<table>', {class:'table table-striped'}).append($tbody)))
			.append(
				$('<div>', {class:'modal-footer'})
				.append($copyBtn)
				.append($('<button>', {type:'button', class: 'btn btn-secondary', 'data-bs-dismiss': 'modal'}).text('Close'))
			)
		)
	);

	var array = JSON.parse(gainTable);
	array.forEach( row => {

		$tbody
		.append(
			$('<tr>')
			.append($('<td>').text(row.input))
			.append($('<td>').text(row.output))
		);
	});
	$modal.modal('show');
});

$('.submenu').click(function(e){
	e.preventDefault();
	e.stopPropagation();
});

$('.modules').click(function(){

	let $menu = $(this).parent().children('.dropdown-menu')
	let length = $menu.children().length;

	if(length>1) return;

	$.get('/calibration/modules_menu', {sn: serialNumber, fragment: this.id})
	.done(function(data){
		$menu.append($(data));
	})
	.fail(conectionFail);
});
$('.dump_devices').click(function(e){
	e.preventDefault();

	let href = this.href;
	$.get(href)
	.done(function(data){

		let $message = $('<div>', { class: "alert alert-success alert-dismissible fade show row", role: "alert"})
						.append($('<strong>', {class: 'col pre-line', text: data}))			
						.append($('<button>', {type: 'button', class: 'btn-close col-auto', 'data-bs-dismiss': 'alert', 'aria-label': 'Close'}));

		$('body').append($message);

		$message.get(0).scrollIntoView({behavior: 'smooth'});
	})
	.fail(conectionFail);
});
$('#btn-http-comport').click(()=>{
	let httpServer = $spServers.val();
	if(httpServer){
		let url = 'http://' + httpServer + ':8088'
		let win = window.open(url, '_blank');
		if(win) 
			win.focus();
		else
			alert('Please allow popups for this website');
	}
});
let $toastContaner = $('#toast-container');
function showToast(title, message, headerClass){

	let $toast = $('<div>', {class: 'toast', role: 'alert', 'aria-live': 'assertive', 'aria-atomic': true})
		.append(
			$('<div>', {class: 'toast-header'})
			.append(
				$('<strong>', {class: 'me-auto', text: title})
			)
			.append(
				$('<button>', {class: 'btn-close', type: 'button', 'data-bs-dismiss': 'toast', 'aria-label': 'Close'})
			)
		)
		.append(
			$('<div>', {class: 'toast-body', text: message})
		)
	.appendTo($toastContaner)
	.on('hide.bs.toast', function(){this.remove();});

	if(headerClass)
		$toast.find('.toast-header').addClass(headerClass);

	new bootstrap.Toast($toast).show();
}

function sendPrologixCommands(commands, responseProcessing){

	if(!commands){
		alert('There is no command to send.');
		return;
	}

	let hostName = getHostName();
	if(!hostName){
		console.log("Unable to get hostname.");
		alert('Unable to get hostname.');
		return;
	}

	commands.hostName = hostName;

	var json = JSON.stringify(commands);

	$.ajax({
		url: '/serial_port/rest/send',
		type: 'POST',
		contentType: "application/json",
		data: json,
        dataType: 'json'
    })
	.done(function(data){

		$.each(data.commands, function(i, c){
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
