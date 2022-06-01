// Show active menu item
$('#miBTRs').addClass('active');

// Input listener
timer = 0;
$('.searchBTR').on('input', function(){

	var $addBTR = $('#addBTR');	// Button "Add" RMA Unit
	if($addBTR.length){
		$addBTR.removeClass('btn-outline-primary');
		$addBTR.addClass('disabled btn-secondary');
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
	$('.searchBTR').filter(':not(#' + attrId + ')').val('');

// Load BTRs
		$('#accordion').load('/btr/search', {id : attrId, value : val}, function(){

			var $addBTR = $('#addBTR');	// Button "Add" RMA Unit
			if(!$addBTR.length || attrId != "btrSerialNumber" || !$addBTR.length)
				return;

			$.post('/rma/rest/has_prifile', { serialNumber: val})
			.done(function(hasProfile){

				if(hasProfile){
					$addBTR.removeClass('disabled btn-secondary');
					$addBTR.addClass('btn-outline-primary');
				}
			})
			.fail(function(error) {
				if(error.statusText!='abort')
					alert(error.responseText);
			});
		});
}

$('#addBTR').click(function(e){
	e.preventDefault();

	var val = $.trim($('#btrSerialNumber').val());

	if($(this).hasClass('disabled')){
		alert('The serial number ' + val + ' does not exist.')
		return;
	}

	$('.modal-content').load('/btr/modal_add', {sn: val});

	$('#modal').modal('show');
});

$('#removeModueles').click(function(){
	$('.module').remove();
});

// Get Part Number, Mfr PN or Description from the cookies
var cookie = Cookies.get("btrSearch")
if(cookie){
	var bomSearch = JSON.parse(cookie);
	var $input = $("#" + bomSearch[0]).val(bomSearch[1]);
	search($input);
}else
	search($('#btrSerialNumber').val('IRT'));
