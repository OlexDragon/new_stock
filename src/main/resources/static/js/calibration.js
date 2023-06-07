$('#miCalibration').addClass('active');

let $modal = $('#modal');

// Get HTTP Serial Port Server from the cookies
var cookie = Cookies.get("spServers");
if(cookie){
	try {
		$('option[value=' + cookie + ']').prop('selected', true);
		gerSerialPorts();
	}catch(err) {}
}

$('#spServers').change(function(){

	var spServers = $(this).val();

	if(!spServers)
		return;

	Cookies.set("spServers", spServers, { expires: 7 });
	gerSerialPorts();
});

$.each($('.save-to-cookies'), function(index, tool){
	let cookie = Cookies.get(tool.id)
	if(cookie)
		$(tool).children().filter(function () { return $(this).text() == cookie; }).prop('selected', true);
});
$('.save-to-cookies').change(function(){
	Cookies.set(this.id, $(this).find('option:selected').text());
});
$('.tool').change(function(){
	let $parent = $(this).parent();
	setAccordionHeaderText($parent);
});
$.each($('.address'), function(index, addr){
	let cookie = Cookies.get(addr.id)
	if(cookie)
		$(addr).val(cookie);
});
$('.address').focusout(function(){
	if(this.value)
		Cookies.set(this.id, this.value);
});

function gerSerialPorts(){

	var spHost = $('#spServers').val();
	if(!spHost)
		return;

	$.post('/serial_port/rest/serial-ports', {hostName: spHost})
	.done(function(ports){

		var $comPorts = $('.com-ports').empty();
		if(!ports){
			alert('It looks like the Serial Port Server is down.');
			return;
		}

		$('<option>', {selected: 'selected', disabled: 'disabled', hidden: 'hidden', title:'Remote Serial Port.'}).text('Select Remote Serial Port.').appendTo($comPorts);

		$.each(ports, function(index, portName){
			$('<option>', {value: portName}).text(portName).appendTo($comPorts);
		});

		$.each($comPorts, function(index, select){
			var value = Cookies.get(select.id)
			if(value){
				var $option = $(select).children('[value=' + value + ']');
				$option.prop('selected', true);
				comPortSelected(select);
				disableMenuItens();
			}
		});
	})
	.fail(conectionFail);
}

$('.com-ports').on('change', function(){

	comPortSelected(this);
	Cookies.set(this.id, this.value, { expires: 999 });

	disableMenuItens();
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

	let $parent = $(select).parent();
	setAccordionHeaderText($parent)
}

function setAccordionHeaderText($parent){

	let $port = $parent.children('.com-ports');
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

	let $tool = $parent.children('.tool');
	let toolMessage = '';

	if(!$tool.length || $tool.val()){
		$parent.find('.to-disable').removeClass('disabled');
		if($tool.length) toolMessage = ', ' + $tool.find('option:selected').text();
	}else
		$parent.find('.to-disable').addClass('disabled');

	let $address = $parent.children('.address');
	let toolAdderss = '';
	if($address.length && $address.val())
		toolAdderss = ', Tool Address: ' + $address.val()

	$message.text(port + toolMessage + toolAdderss);
}
if(!$('#serialNumber').text())
	new bootstrap.Modal('#modal').show();

var calibrateId = undefined;

// Show Calibration message
$('.calibrate').click(function(e){
	e.preventDefault();

	let id = e.target.id;

// Load for first time or when the serial number is changed 
	if(calibrateId!=id){
			calibrateId = id;
			$modal.load(this.href, function(body,error,c){
				if(error=='error')
					alert('Unable to connect to Unit.');
			});
	}else
		$modal.modal('show');
});

// Uplode the profile
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
		alert(data);
	})
	.fail(conectionFail);
}

// Scan IP Addresses
var scanIpInterval;
var $scan = $('#scan');
$scan.click(function(e){
	e.preventDefault();

	$modal.empty();
	var $modalBody = $('<div>', {class:'modal-body'});
	let $scanBtn = $('<button>', {id: 'scanBtn', type:'button', class: 'btn btn-primary'}).text('Stop');
	$modal
	.append(
		$('<div>', {class:'modal-dialog modal-lg'})
		.append(
			$('<div>', {class:'modal-content'})
			.append(
				$('<div>', {class:'modal-header'})
				.append($('<h5>', {id:'modal-header', class: 'modal-title ml-3 text-primary col'}).text('Scaning for online units.'))
				.append($('<input>', {type:'number', id: 'start-from', class: 'col-1', title: 'Start scan from this value.'}))
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
			Cookies.set("startFrom",  val);
		else
			Cookies.set("startFrom",  '');
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

	var spServers = $('#spServers').val();
	if(spServers)
		return spServers;
	else
		alert('The Serial Port Server is not selected.');

	return null;
}

$('#dropdownCalibrateButton').on('show.bs.dropdown', function(){

	var serialNumber = $('#serialNumber').text();

	if(!serialNumber)
		return;

	$.post('/calibration/rest/calibration_mode', { ip: serialNumber })
	.done(function(calMode){

		var status = calMode["Calibration mode"];
		var $calMode = $('#calMode').removeClass('text-primary text-success');
		var text;

		switch(status){

		case 'OFF':
			$calMode.addClass('text-primary').text('Calibration Mode: ' + status);
			$('#menuGain').addClass('disabled list-group-item-light').text('Gain - Cal.Mode must be ON');
			break;

		case 'ON':
			$calMode.addClass('text-success').text('Calibration Mode: ' + status);
			$('#menuGain').removeClass('disabled list-group-item-light').text('Gain');
			break;

		default:
			$calMode.text('Calibration Mode');
			$('#menuGain').addClass('disabled list-group-item-light');
		}
	})
	.fail(function(error) {

		$('#calMode').removeClass('text-primary text-success').text('Calibration Mode');
		alert('Unable to connect to Unit.');
	});
});

$('#calMode').click(function(e){
	e.preventDefault();

	var serialNumber = $('#serialNumber').text();

	$.post('/calibration/rest/calibration_mode_toggle', { ip: serialNumber })
	.fail(conectionFail);
});


function hasValue(inputs){

	for(let s of inputs){
		if(s.value){
			return true;
			break;
						}
	}

	return false;
}

function toArray($inputs){
	var values = [];
	$inputs.map((i, v)=>v.value).filter((i, v)=>v).map((i, v)=>parseFloat(v)).sort().each((i, v)=>values.push(v));
	return values;
}

//Created for TROPOSCAT to see currents of output devices
$('#currents').click(function(e){
	e.preventDefault();

	let serialNumber = $('#serialNumber').text();
	let href = '/calibration/currents?sn=' + serialNumber;
	let modal = $modal.load(href);
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

// Control Input 
$('.input-value').on('input', function(){
	let btnID = this.dataset.for;
	if(this.value)
		$(btnID).text('Set');
	else
		$(btnID).text('Get');
});
$('#menuUploadModule').click(function(){

	let $menu = $(this).parent().children('.dropdown-menu')
	let length = $menu.children().length;

	if(length>1) return;

	var serialNumber = $('#serialNumber').text();

	$.get('/calibration/upload_modules_menu', {sn: serialNumber})
	.done(function(data){
		$menu.append($(data));
	})
	.fail(conectionFail);
});
$('#menuModuleProfilePath').click(function(){

	let $menu = $(this).parent().children('.dropdown-menu')
	let length = $menu.children().length;

	if(length>1) return;

	var serialNumber = $('#serialNumber').text();

	$.get('/calibration/modules_profile_path_menu', {sn: serialNumber})
	.done(function(data){
		$menu.append($(data));
	})
	.fail(conectionFail);
});
$('#menuModuleProfile').click(function(у){

	let $menu = $(this).parent().children('.dropdown-menu')
	let length = $menu.children().length;

	if(length>1) return;

	var serialNumber = $('#serialNumber').text();

	$.get('/calibration/modules_profile_menu', {sn: serialNumber})
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