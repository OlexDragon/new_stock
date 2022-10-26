// Show active menu item
$('#miWOs').addClass('active');

// Input listener
timer = 0;
$('.searchWO').on('input', function(){

	var $addSerialNumber = $('#addSerialNumber');	// Button "Add" RMA Unit
	if($addSerialNumber.length){
		$addSerialNumber.removeClass('btn-outline-primary');
		$addSerialNumber.addClass('disabled btn-secondary');
	}

    if (timer) 
    	clearTimeout(timer);
 
    timer = setTimeout(search, 1000, $(this));
});

function search($this){

	var tmp = $.trim($this.val());
	if(!tmp)
		return;

	var val;
	var id = $this.prop('id');

// Remove extra whitespaces
	if(id=='btrDescription')
		val = tmp.replace(/\s+/g, ' ');

	else
		val = tmp.replace(/\s+/g, '');


	if(val.length!=tmp.length)
		$this.val(val);

// Save Cookies
	var attrId = $this.prop('id');
	Cookies.set("btrSearch", JSON.stringify([attrId, val]), { expires: 7 });

// Crear other input filds
	$('.searchWO').filter(':not(#' + attrId + ')').val('');

// Load BTRs
		$('#accordion').load('/wo/search', {id : attrId, value : val}, function(){

			var $addSerialNumber = $('#addSerialNumber');	// Button "Add" RMA Unit
			if(!$addSerialNumber.length || attrId != "btrSerialNumber" || !$addSerialNumber.length)
				return;

			$.post('/rma/rest/has_prifile', { serialNumber: val})
			.done(function(hasProfile){

				if(hasProfile){
					$addSerialNumber.removeClass('disabled btn-secondary');
					$addSerialNumber.addClass('btn-outline-primary');
				}
			})
			.fail(function(error) {
				if(error.statusText!='abort')
					alert(error.responseText);
			});
		});
}

$('#addSerialNumber').click(function(e){
	e.preventDefault();

	var val = $.trim($('#btrSerialNumber').val());

	if($(this).hasClass('disabled')){
		alert('The serial number ' + val + ' does not exist.')
		return;
	}

	$('.modal-content').load('/wo/modal_add', {sn: val});

	$('#modal').modal('show');
});

$('#removeModueles').click(function(){
	$('.module').remove();
});

// Get Part Number, Mfr PN or Description from the cookies
var $btrSerialNumber = $('#btrSerialNumber');
if(!$btrSerialNumber.val()){
	var cookie = Cookies.get("btrSearch")
	if(cookie){
		var bomSearch = JSON.parse(cookie);
		var $input = $("#" + bomSearch[0]).val(bomSearch[1]);
		search($input);
	}else
		search($('#btrSerialNumber').val('IRT'));
}else{
	search($btrSerialNumber);
}
var showBtrMouseType;
function showBtrMouse(e){
	showBtrMouseType = e.type
}
$('#accordion')
.on('show.bs.collapse', function(e){
	if(showBtrMouseType=='mouseenter'){
		e.preventDefault();
	}
})
.on('hide.bs.collapse', function(e){
	if(showBtrMouseType=='mouseenter'){
		e.preventDefault();
	}
});
function showMeasurement(serialNumberId){
	$('.modal-content').load('/wo/show_measurement', {snId: serialNumberId}, function(){
		$('#modal').modal('show');
	});
}
