
$('#miCalibration').addClass('active');

// Get HTTP Serial Port from the cookies
var cookie = Cookies.get("spServers")
if(cookie){
	try {
		$('option[value=' + cookie + ']').prop('selected', true);
		gerSerialPorts();
	}catch(err) {}
}

$('#spServers').change(function(){
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
				$(select).val(value);
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
	$message.text(select.value);
	$message.removeClass('text-danger');
	$(select).parent().find('.disabled').removeClass('disabled');
}

if(!$('#serialNumber').text())
	new bootstrap.Modal('#modal').show();

var calibrateId = undefined;

// Show Calibration message
$('.calibrate').click(function(e){
	e.preventDefault();

	var id = e.target.id;
	var $modal = $('#modal');

// Load for first time or when changed the serial number 
	if(typeof unitSerialNumber==='undefined' || !$('#serialNumber').text().match('^' + unitSerialNumber) || calibrateId!=id){
		calibrateId = id;
		$modal.load(this.href);
	}

	$modal.modal('show');
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
	login(e, this);
});

function login(e, _this){
	e.preventDefault();

	$.post(_this.href)
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
$('#scan').click(function(e){
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
				.append($('<h5>', {id:'modal-header', class: 'modal-title ml-3 text-primary'}).text('Scaning for online units.'))
				.append($('<button>', {type:'button', class: 'btn-close', 'data-bs-dismiss': 'modal', 'aria-label': 'Close'}))
			)
			.append($modalBody)
			.append(
				$('<div>', {class:'modal-footer'})
				.append($('<button>', {type:'button', class: 'btn btn-secondary', 'data-bs-dismiss': 'modal'}).text('Close'))
			)
		)
	);

	new bootstrap.Modal('#modal').show();

	if(typeof scanIpInterval !=='undefined')
		clearInterval(scanIpInterval);

	var ip = 10;
	var index = 0;
	scanIpInterval = setInterval(function() {

		var ipAddress = '192.168.2.' + ip;

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
					$('<div>', {class: 'col-auto'}).append($('<a>', { class: 'btn btn-sm btn-outline-info', onclick: 'login(event, this)', target: "_blank", href: '/calibration/rest/login?sn=' + info["Serial number"]}).text('Login'))
				);
				$row.attr('data-bs-toggle','tooltip').attr('data-bs-placement','top').attr('title', info["Product name"]);
			});
		});

		++ip;
		if(ip>240){
			// Stop IP scan
			clearInterval(scanIpInterval);

			var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
			var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) { return new bootstrap.Tooltip(tooltipTriggerEl)});
			$('#modal-header').text('Scan completed.');
		}
	}, 200);

	$modal.on('hidden.bs.modal', function () {
		clearInterval(scanIpInterval);
	});
});

$('#spServers').change(function(){
	var spServers = $(this).val();
	if(spServers)
		Cookies.set("spServers", spServers, { expires: 7 });
});

function getHostName(){

	var spServers = $('#spServers').val();
	if(spServers)
		return spServers;
	else
		alert('The Serial Port Server is not selected.');

	return null;
}
