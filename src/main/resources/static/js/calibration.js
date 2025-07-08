$('#miCalibration').addClass('active');

const $modal = $('#modal').on('shown.bs.modal', e=>$('#sn').focus());
const $btrMeasurement	 = $('#btrMeasurement');
const $serialNumber		 = $('#serialNumber');
const $spServers		 = $('#spServers');
const $menuInputPower	 = $('#menuInputPower');
const $menuGain			 = $('#menuGain');
const $calMode			 = $('#calMode');
const $menuOPAutoByInput = $('#menuOPAutoByInput');
const $nemuOPAutoByGain	 = $('#nemuOPAutoByGain');
const $tool				 = $('.tool');
const $toastContainer = $('#toast-container');

const sn = new URLSearchParams(window.location.search).get('sn');
if(sn && !sn.includes('.'))
	Cookies.set("rmaSearch", JSON.stringify(['rmaSerialNumber', sn]), { expires: 7});

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

const $comPorts = $('.com-ports').on('change', function(){

	comPortSelected(this);
	Cookies.set(this.id, this.value, { expires: 999, path: '' });

	$(this).parents('.accordion-collapse').trigger('show.bs.collapse');
});

$spServers.change(function(){

	$menuInputPower.removeClass('disabled');
	$menuGain.removeClass('disabled');
	$menuOPAutoByInput.removeClass('disabled').text('by Input');
	$nemuOPAutoByGain.removeClass('disabled').text('by Gain');

	const spServers = $(this).val();
	Cookies.set("spServers", spServers, { expires: 7, path: '' });
	gerSerialPorts(setToolsSerialPorts);
});

// Get HTTP Serial Port Server from the cookies
const cookie = Cookies.get("spServers");
if(cookie){
	try {
		const $toSelect = $('option[value=' + cookie + ']');
		if($toSelect.length){
			$toSelect.prop('selected', true);
			$spServers.trigger('change');
		}
	}catch(err) {
		console.error(err);
	}
}

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

	$.each(ports, function(_, portName){
		$('<option>', {value: portName}).text(portName).appendTo($comPorts);
	});

	$.each($comPorts, (_, select)=>{
		var value = Cookies.get(select.id)
		if(value){
			var $option = $(select).children('[value="' + value + '"]');
			$option.prop('selected', true);
			comPortSelected(select);
			if($option?.val() != 'NI GPIB')
				setTimeout(()=>$option.parents('.accordion-body').find('.btn-auto').click(), 1000);
		}
	});
}

function comPortSelected(select){
	let $parent = $(select).parents('.accordion-body');
	setAccordionHeaderText($parent)
}

function setAccordionHeaderText($parent){

	const $port = $parent.find('.com-ports');
	const messageId = $port.attr('data-info-message');
	const $message = $('#' + messageId);
	const $toDisable = $parent.find('.to-disable');
	const $toolBtns = $parent.find('.tool-btn');

	let port = $port.length ? $port.val() : '';
	if(port && !port.startsWith('Select')){
		$toDisable.addClass('disabled');
		if(port==='NI GPIB')
			$toolBtns.removeClass('disabled');
	}else{
		port = '';
		$message.text(' Serial Port is not selected');
		$message.addClass('text-danger');
		$toDisable.addClass('disabled');
		return;
	}

	$message.removeClass('text-danger');

	let $tool = $parent.find('.tool');
	let toolMessage = '';

	if(!$tool.length || $tool.val()){
		if(port!='NI GPIB')
			$toDisable.removeClass('disabled');
		if($tool.length) toolMessage = ', ' + $tool.find('option:selected').text();
	}else
		$toDisable.addClass('disabled');

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
$('.calibrate').click(showCalibrationModal);
function showCalibrationModal(e){
	e.preventDefault();

	const id = e.target.id;

// Load for first time or when the serial number is changed 
	if(calibrateId!=id){

		if(this.href.slice(-1) == '#'){
			alert('Unable to connect to the Unit.');
			return;
		}

		calibrateId = id;
		loadModal(this.href);

	}else
		if(setupModal(true))
			$modal.modal('show');
}

function setupModal(){
	alert("Empty setupModal");
	return false;
}

// Upload the profile
$('.upload').click(function(e){
	upload(e, this);
});

function upload(e, link){
	if(link)
		e.preventDefault();
	else
		link = e;

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

var loginSent;
function login(){
	if(!loginSent){
		var href = $('#unitLogin').prop('href');
		console.log('Log In to ' + href);
		loginWhithHref(href);
		loginSent = true;
		setTimeout(()=>{loginSent = false;}, 6000);
	}else
		console.log('Skipped Log In to ' + href);
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
		console.log(title + ' : ' + data);
		showToast(title, data, 'text-bg-success');
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

		if(postWithParamCount>20)
			return;

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

		postWithParam('/calibration/rest/scan', {ip : ipAddress}, function(homePageInfo){

			if(!homePageInfo || !homePageInfo.sysInfo || !homePageInfo.netInfo)
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
						$('<a>', {href: 'http://' + homePageInfo.netInfo.addr, target: "_blank"}).text(homePageInfo.netInfo.addr)
					)
				)
				.append($text)
			);

			$text.append($('<a>', {href: 'http://' + homePageInfo.sysInfo.sn, target: "_blank"}).text(homePageInfo.sysInfo.sn));
			$row
			.append(
				$('<div>', {class: 'col-auto'}).append($('<a>', { class: 'btn btn-sm btn-outline-dark', target: "_blank", href: '/calibration?sn=' + homePageInfo.netInfo.addr}).text('Calibrate'))
			)
			.append(
				$('<div>', {class: 'col-auto'}).append($('<a>', { class: 'btn btn-sm btn-outline-info', onclick: 'loginFromScan(event, this)', target: "_blank", href: '/calibration/rest/login?sn=' + homePageInfo.netInfo.addr}).text('Login'))
			);
			$row.attr('data-bs-toggle','tooltip').attr('data-bs-placement','top').attr('title', homePageInfo.sysInfo.desc);
		});

		++ip;
	}, 100);

	$modal.on('hide.bs.modal', function () {
		if (document.activeElement) 
	        document.activeElement.blur();
	});

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

$('#dropdownCalibrateButton').on('show.bs.dropdown', function(){

	if($menuGain.attr('href')=='/calibration/gain/converter')
		return;

	$menuGain.addClass('disabled list-group-item-light');

	if(!serialNumber || $calMode.hasClass('disabled'))
		return;

	$.post('/calibration/rest/calibration-mode', { sn: serialNumber })
	.done(function(calMode){

		if(!calMode)
			calibrationModeError('calMode == null');

		var status = calMode["Calibration mode"];
		$calMode.removeClass('text-primary text-success');

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

	$calMode.removeClass('text-primary text-success').text('Calibration Mode');
	alert('Unable to connect to Unit.');
}

$calMode.click(function(e){
	e.preventDefault();

	$.post('/calibration/rest/calibration-mode-toggle', { sn: serialNumber })
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
	$inputs.map((_, v)=>v.value).filter((i, v)=>v).map((_, v)=>parseFloat(v)).sort().each((_, v)=>values.push(v));
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
	calibrateId = e.id;
	loadModal(`/calibration/currents?sn=${serialNumber}`);
});
function loadModal(href){
	setupModal = undefined;
	$modal.modal('hide');
//	$modal.empty();
	$modal.load(href, function(_, error){
		if(error=='error'){
			setTimeout(()=>alert('Unable to connect to the Unit.'), 10);
			console.log(error);
			return;
		}

  		$modal.off('shown.bs.modal').off('hide.bs.modal');

		if(setupModal && setupModal())
			setTimeout(()=>$modal.modal('show'), 600);
	});
}
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
			$calMode.removeClass('text-primary text-success').text('Calibration Mode');
	});
}

$('.profilePath').click((e)=>getPath(e));
$('.profileDir').click((e)=>getPath(e));

 function getPath(e){
  	e.preventDefault();
  
	$.get(e.currentTarget.href)
 	.done((map)=>{

 		const $btnCopy = $('<button>', {type: 'button', class: 'btn col-auto copy', title: 'Copy to clipboard', 'aria-label': 'Copy to clipboard', text: 'Copy'});

 		const $message = $('<div>', { class: "alert alert-warning alert-dismissible fade show row", role: "alert"})
 						.append($('<strong>', {class: 'col'}).text(map.message))	
						.append($btnCopy);

		if(map["serial-exists"]){

			const $btnOpen = $('<button>', {class: 'btn col-auto', title: 'Open Dir', 'aria-label': 'Open Directory', text: 'Open'});
			$message.append($btnOpen)
			$btnOpen.click(()=>
				$.post('/calibration/rest/open', {path: map.message, url: map.remoteAddr})
				.done(open=>{
					if(!open)
						alert('There is no such file.');
				})
				.fail(()=>alert('It is impossible to open the file.')));
		}

 		$message.append($('<button>', {type: 'button', class: 'btn-close col-auto', 'data-bs-dismiss': 'alert', 'aria-label': 'Close'}));
 		$('body').append($message);

 		$btnCopy.click(function(){
 			var strong = $(this).parent().children('strong')[0];
 			selectAndCopy(strong);
 		});

//		$btnOpen.click(()=>{
//			showFilePicker();
//			window.open("file:" + path);
//		});

 		$message.get(0).scrollIntoView({behavior: 'smooth'});
 	})
 	.fail(function(error) {
 		if(conectionFail(error))
 			$calMode.removeClass('text-primary text-success').text('Calibration Mode');
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
	const errorCode = error.getResponseHeader('error-code');
	if(errorCode && errorCode<0){
		alert(error.getResponseHeader('error-line'));
		return true;
	}

	if(error.statusText!='abort'){
		if(error.responseText)
			alert(error.responseText);
		else if(error.statusText)
			alert(error.statusText);
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
	.appendTo($toastContainer)
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
	postObject('/serial_port/rest/send', commands)
	.done(function(data){

		if(data.error){
			alert(data.error);
			return;
		}
		$.each(data.commands, function(_, c){
			responseProcessing(c);
		});
	})
	.fail(function(error) {
		if(error.statusText!='abort'){
			responseProcessing(error);
			var responseText = error.responseText;
			if(responseText)
				alert(error.responseText);
			else
				alert("Server error. Status = " + error.status)
		}
	});
}
function postFormData(url, formData){
	return $.ajax({
		url: url,
		type: 'POST',
		processData: false,
		contentType: false,
		data: formData
	});
}
function postObject(url, object){
	var json = JSON.stringify(object);

	return $.ajax({
		url: url,
		type: 'POST',
		contentType: "application/json",
		data: json,
	    dataType: 'json'
	});
}
let postWithParamCount = 0;
function postWithParam(url, params, f_action, f_error){

	++postWithParamCount;

	$.post(url, params).done(data=>{
		--postWithParamCount;
		if(f_action)
			f_action(data);
	}).fail(err=>{
		--postWithParamCount;
		if(f_error)
			f_error(err);
	});
}
function softSelected(el){
	const url = '/calibration/rest/soft/select/upload'
	const fd = new FormData();
	fd.append('file', el.files[0]);
	if(el.dataset.sn)
		fd.append('sn', el.dataset.sn);
	fd.append('moduleSn', el.dataset.moduleSn);

	postFormData(url, fd)
	.done(data=>{
		alert(data);
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
function packageInPackage(el){

	if(!el.files || !el.files.length)
		return;

	const url = '/calibration/rest/profile/package-package'
	const fd = new FormData();
	fd.append('file', el.files[0]);
	if(el.dataset.sn)
		fd.append('sn', el.dataset.sn);
	fd.append('moduleSn', el.dataset.moduleSn);

	postFormData(url, fd)
	.done(data=>{
		alert(data);
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
const $fixedTop = $('.fixed-top');
const $accordion = $('#accordion');
setMarginTop();
$(window).on('resize', setMarginTop);
function setMarginTop(){
	$accordion.css('margin-top', $fixedTop.height() + 20);
}
function downloadURL(href) {
  var link = document.createElement("a");
  link.href = href;
  link.target = '_blank';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  delete link;
}
if(serialNumber){	

	$.get(`/calibration/rest/sn?sn=${serialNumber}`)
	.done(data=>{

		if(!data)
			return;

		$('#webSn').text(data.serialNumber);
		$('#webId').text(data.id);
		$('#webPn').text(data.partNumber.partNumber);
		$('#webDescr').text(data.partNumber.description);
	});

	const $tunedBy =$('#tunedBy');
	$.get(`/btr/rest/unit-tuning?sn=${serialNumber}`)
	.done(data=>{

		if(!data)
			return;

		if(data.payload.startsWith('<!DOCTYPE html')){
			alert('Unable to connect to the 1C server. (unit-tuning)');
			return;
		}

				const o = JSON.parse(data.payload);
		if(!o.length)
			return;
		if(o[0].TunedBy)
			$tunedBy.text('Tuned by ' + o[0].TunedBy);
		$('#oneCImd').text(o[0].IMD);
		$('#oneCNotes').text(o[0].Notes);
	});

	$.get(`/btr/rest/header?sn=${serialNumber}`)
	.done(data=>{

		if(!data)
			return;

		if(data.payload.startsWith('<!DOCTYPE html')){
			alert('Unable to connect to the 1C server. (header)');
			return;
		}

		const o = JSON.parse(data.payload);
		if(typeof o === "string"){
			console.log(o);
			return;
		}

		$('#oneCHeadDescr').text(o.Description);
		$('#oneCHeadNotes').text(o.Notes);
		$('#oneCHeadPn').text(o.SalesSKU);
		$('#oneCHeadIntern').text(o.Product).click(()=>{
			const split = o.Product.split('-');
			let message = '';

			switch(split[0].charAt(0)){
				case 'A':
					message = "AntBUC\n";
					break;
				case 'P':
					message = "PicoBUC\n";
					break;
				case 'K':
					message = "KiloBUC\n";
					break;
				case 'R':
					message = "Rack Mount\n";
					break;
				case 'F':
					message = "FemtoBUC\n";
					break;
			}

			switch(split[1].charAt(0)){
				case '1':
					message += "S-Band\n";
					break;
				case '2':
					message += "C-Band\n";
					break;
				case '3':
					message += "X-Band\n";
					break;
				case '4':
					message += "Ku-Band\n";
					break;
					case '5':
						message += "Ka-Band\n";
						break;
			}			

			switch(split[1].substring(0,2)){
				case '21':
					message += "LMI 5.725-6.025 GHz\n";
					break;
				case '22':
					message += "Standard 5.85-6.425 GHz\n";
					break;
				case '23':
					message += "Full 5.85-6.75 GHz\n";
					break;
				case '24':
					message += "Russian 5.975-6.475 GHz\n";
					break;
				case '25':
					message += "Txt. 6.425-6.725 GHz\n";
					break;
				case '26':
					message += "Palapa 6.425-6.665 GHz\n";
					break;
				case '27':
					message += "Insat 6.725-7.025 GHz\n";
					break;
				case '28':
					message += "Tropo 4.4-5.0 GHz\n";
					break;
				case '31':
					message += "Std. 7.9-8.1 GHz\n";
					break;
				case '32':
					message += "Ext. 7.9-8.4 GHz\n";
					break;
				case '41':
					message += "Low. 12.75-13.25 GHz\n";
					break;
				case '42':
					message += "Ext. 13.75-14.5 GHz\n";
					break;
				case '43':
					message += "Std. 14.0-14.5 GHz\n";
					break;
				case '44':
					message += "Hifg. 14.5-14.8 GHz\n";
					break;
			}

			message += parseInt(split[2]) + ' W\n';

			if(split[3].charAt(0)==='A')
				message += 'GaAs\n';
			else
				message += 'GaN\n';

			switch(split[3].charAt(1)){
				case '1':
					message += "External\n";
					break;
				case '2':
					message += "Internal\n";
					break;
				case '3':
					message += "Autosense\n";
					break;
			}

			if(split[3].charAt(2)==='A')
				message += 'AC\n';
			else
				message += 'DC\n';

			if(split[3].charAt(4)==='1')
				message += 'Redundant\n';

			alert(message);
		});
	});
}
if(!$calMode.hasClass('disabled')){
	$.post('/calibration/rest/sticker', {sn: serialNumber})
	.done(data=>{
		const $stickers = $('#stickers');
		Object.keys(data).forEach(k=>{
			const val = data[k];
			let toShow = val['sticker'];
			if(toShow)
				toShow = parseInt(toShow);
			else
				toShow = 'N/A'
			$stickers.append($('<div>', {class: 'row', title: val['cpu']}).append($('<div>', {class: 'col', text: k})).append($('<div>', {class: 'col', text: toShow})));
		});
	});
	checkSerialNumbers();
}
function checkSerialNumbers(){
	$.get('/calibration/rest/all-modules', {sn: serialNumber})
	.done(data=>{
		const keys = Object.keys(data);
		for(let i=0;i<keys.length;++i){
			setTimeout(checkForKey, i*1000, data, keys[i]);
		}
	});
}
function checkForKey(data, key){
	
	if(key.startsWith('FCM'))
		getSerialNumber(data[key], 'converter');

	else if(key.startsWith('RCM'))
		getSerialNumber(data[key], 'reference-clock-module');
}
function getSerialNumber(moduleIndex, oneCeGroup){

	$.get('/calibration/rest/one-c/profile', {sn: serialNumber, section: oneCeGroup})
	.done(data=>{

		if(!data)
			return;

		if(typeof data === 'string'){
			console.warn(data);
			return;
		}

		const o = JSON.parse(data);
		for(let i=0;i<o.length;++i){

			const object = o[i];
			if(object.Profile)
				continue;

			$.get('/calibration/rest/module-info', {sn: serialNumber, moduleIndex: moduleIndex})
			.done(moduleInfo=>{

				const line = moduleInfo.split(/\r?\n/).filter(l=>l.includes('Serial number'));
				if(!line.length)
					return;

				object.serialNumber = serialNumber;
				object.section = oneCeGroup;
				object.fieldName = 'Profile';
				object.value = line[0].split(':')[1].trim();

				const exists = o.filter(v=>v.Profile===object.value);
				if(exists.length)
					return;

				postObject('/calibration/rest/one-c/profile', object)
				.done(message=>{
					console.log(message);
				});
			});
			break;
		}
	});
}
if(window.location.search.includes('.') && serialNumber && !serialNumber.includes('.')){
	const newUrl = window.location.origin + window.location.pathname + '?sn=' + serialNumber;
	history.replaceState({replace: true}, "", newUrl);
}