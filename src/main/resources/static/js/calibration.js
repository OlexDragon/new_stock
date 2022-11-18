
$('#miCalibration').addClass('active');

// Get HTTP Serial Port from the cookies
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

function gerSerialPorts(){

	var spHost = $('#spServers').val();
	if(!spHost)
		return;

	$.post('/serial_port/rest/serial-ports', {hostName: spHost})
	.done(function(ports){

		var $comPorts = $('.com-ports').empty();

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
			}
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

$('.com-ports').on('change', function(){

	comPortSelected(this);
	Cookies.set(this.id, this.value, { expires: 999 });
});

function comPortSelected(select){

	var messageId = select.dataset.infoMessage;
	var $message = $('#' + messageId);
	var value = select.value;

	if(!value || value.startsWith('Select')){
		$message.text(' Serial Port is not selected');
		$message.addClass('text-danger');
		$(select).parent().find('.to-disable').addClass('disabled');
		return;
	}

	$message.text(select.value);
	$message.removeClass('text-danger');
	$(select).parent().find('.to-disable').removeClass('disabled');
}

if(!$('#serialNumber').text())
	new bootstrap.Modal('#modal').show();

var calibrateId = undefined;

// Show Calibration message
$('.calibrate').click(function(e){
	e.preventDefault();

	var id = e.target.id;
	var $modal = $('#modal');

// Load for first time or when the serial number is changed 
	if(typeof unitSerialNumber==='undefined' || !$('#serialNumber').text().match('^' + unitSerialNumber) || calibrateId!=id){
		calibrateId = id;
		$modal.load(this.href);
	}
});

// Uplode the profile
$('#upload').click(function(e){
	e.preventDefault();

	$.post(this.href)
	.done(function(data){
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
});

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

// Scan IP Addresses
var scanIpInterval;
var $scan = $('#scan');
$scan.click(function(e){
	e.preventDefault();

	var $modalBody = $('<div>', {class:'modal-body'});
	$modal = $('#modal');
	$modal.empty();
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
				.append($('<button>', {id: 'scanBtn', type:'button', class: 'btn btn-primary'}).text('Stop'))
				.append($('<button>', {type:'button', class: 'btn btn-secondary', 'data-bs-dismiss': 'modal'}).text('Close'))
			)
		)
	);

	$('#scanBtn').click(function(e){
		e.preventDefault();

		let $this = $(this);
		let text = $this.text();

		switch(text){

			case 'Stop':	ip = 250;break;

			case 'Restart':	$('#modal').modal('hide');
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
			ip = 10;
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

		if(error.statusText!='abort'){
			var responseText = error.responseText;
			if(responseText)
				alert(error.responseText);
			else
				alert("Server error. Status = " + error.status)

			$('#calMode').removeClass('text-primary text-success').text('Calibration Mode');
		}
	});
});

$('#calMode').click(function(e){
	e.preventDefault();

	var serialNumber = $('#serialNumber').text();

	$.post('/calibration/rest/calibration_mode_toggle', { ip: serialNumber })
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
	let modal = $('#modal').load(href);
})

$('#profilePath').click(function(e){
	e.preventDefault();

	$.get(this.href)
	.done(function(path){
		$('body')
		.append(
			$('<div>', { class: "alert alert-warning alert-dismissible fade show row", role: "alert"})
			.append($('<strong>', {class: 'col'}).text(path))			
			.append($('<button>', {type: 'button', class: 'btn col-auto copy', title: 'Copy to clipboard', 'aria-label': 'Copy to clipboard'}).text('Copy'))
			.append($('<button>', {type: 'button', class: 'btn-close col-auto', 'data-bs-dismiss': 'alert', 'aria-label': 'Close'}))
		);

		$('.copy').click(function(){
			var strong = $(this).parent().children('strong')[0];
			selectAndCopy(strong);
		});
	})
	.fail(function(error) {

		if(error.statusText!='abort'){
			var responseText = error.responseText;
			if(responseText)
				alert(error.responseText);
			else
				alert("Server error. Status = " + error.status)

			$('#calMode').removeClass('text-primary text-success').text('Calibration Mode');
		}
	});
 });
 
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
	document.execCommand("copy");
}
 