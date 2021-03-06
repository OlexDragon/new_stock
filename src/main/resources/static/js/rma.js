
$('#miRMAs').addClass('active');

// Get RMA filter text from the cookies
var filterCookie = Cookies.get("rmafilter")
if(filterCookie){
	$('#rmaFilter').text(filterCookie);
}

// Get RMA sort by
var rmaSorting = Cookies.get("rmaSorting")
if(rmaSorting){
	$('#' + rmaSorting).prop('checked', true);
}

// Input listener
timer = 0;
$('.searchRma').on('input', function(){

    if (timer) 
    	clearTimeout(timer);
 
    timer = setTimeout(search, 1000, $(this));
});

function search($this){

	var $addRMA = $('#addRMA');	// Button "Add" RMA Unit
	if($addRMA.length){
		$addRMA.removeClass('btn-outline-primary');
		$addRMA.addClass('disabled btn-secondary');
	}

	var tmp = $.trim($this.val());
	if(!tmp)
		return;

	var val;
	var id = $this.prop('id');

// Remove extra whitespaces
	if(id=='rmaDescription')
		val = tmp.replace(/\s+/g, ' ');

	else
		val = tmp.replace(/\s+/g, '');


	if(val.length!=tmp.length)
		$this.val(val);

// Save Cookies
	var attrId = $this.prop('id');
	Cookies.set("rmaSearch", JSON.stringify([attrId, val]), { expires: 7 });
	$('.searchRma').filter(':not(#' + attrId + ')').val('');

// Sort By
	var $radio = $("input[name=sort_by]").filter(':checked');
	if(!$radio.length)
		$radio = $('#rmaOrderBySerialNumber').prop('checked', true);

	var sortBy = $radio.prop('id');
	var rmaFilter = $('#rmaFilter').text();

// Load RMAs
	var $accordion = $('#accordion');
	$accordion.load('/rma/search', {id : attrId, value : val, sortBy: sortBy, rmaFilter: rmaFilter}, function(){

		if(!$addRMA.length || attrId != "rmaSerialNumber")
			return;

		$.post('/rma/rest/has_prifile', { serialNumber: val})
		.done(function(hasProfile){

			if(hasProfile){
				$addRMA.removeClass('disabled btn-secondary');
				$addRMA.addClass('btn-outline-primary');
			}else{
				$addRMA.removeClass('btn-outline-primary');
				$addRMA.addClass('disabled btn-secondary');
			}
		})
		.fail(function(error) {
			if(error.statusText!='abort')
				alert(error.responseText);
		});
	});

// Draw attention to the filter button. ('All', 'WOR_king', 'SHI_pped')
	var $rmaFilter = $('#rmaFilter').removeClass('btn-outline-primary');

	(function pointToFilter(times){
		setTimeout(function(){

			if(!times){
				$rmaFilter.removeClass('btn-outline-danger btn-danger').addClass('btn-outline-primary');
				return;
			}

			if($rmaFilter.hasClass('btn-danger')){
				$rmaFilter.addClass('btn-outline-danger');
				$rmaFilter.removeClass('btn-danger');
			}else{
				$rmaFilter.addClass('btn-danger');
				$rmaFilter.removeClass('btn-outline-danger');
			}

			pointToFilter(--times);

		}, 500);
	})(6);
}

function showModal(rmaId, serialNumber){

	$('#modal_title').text(serialNumber);
	$('#saveComment').val(rmaId);
	$('#modal').modal('show');
}

$('#addRMA').click(function(e){
	e.preventDefault();

	var val = $.trim($('#rmaSerialNumber').val());

	if($(this).hasClass('disabled')){
		alert('The serial number ' + val + ' does not exist.')
		return;
	}

	if(!confirm("Save Unit " + val + ' as RMA?'))
		return;

	$('#accordion').load('/rma/add_rma', { serialNumber: val});
});

$('#saveComment').click(function(e){
	e.preventDefault();

	var $rmaTextarea = $('#rmaTextarea').select();
	document.execCommand("copy");
	var comment = $.trim($rmaTextarea.val());

	if(!comment)
		return;

	var $shipping = $('#shipping');
	var shipped;
	if($shipping)
		shipped = $shipping.prop('checked')
	else
		shipped = false;

	var rmaId = this.value;
	$('#accordion-body').load("/rma/add_comment", {rmaId: rmaId, comment: comment, shipped: shipped});
//	$('#modal').modal('hide');
	 location.reload();
});
$('#rmaTextarea').on('input', function(){

	let $readyToShip = $('#readyToShip');
	if($readyToShip.hasClass('rma-ready'))
	return;

	if($(this).val())
		$readyToShip.addClass('disabled')
	else
		$readyToShip.removeClass('disabled')
});
$('#accordion').on('shown.bs.collapse', function () {

	var $accordionItem = $(this).children().filter(function(index,a){return !$(this).find('button').hasClass('collapsed');});
  	var $accordionBody = $accordionItem.find('.accordion-body');
  	var $children = $accordionBody.children();

	if($children.length)
 		return;

	var id = $accordionItem.attr('id');
	$accordionBody.load('/rma/comments', {rmaId: $accordionItem[0].id});
});

// Get Part Number, Mfr PN or Description from the cookies
var cookie = Cookies.get("rmaSearch")
if(cookie){
	var bomSearch = JSON.parse(cookie);
	var $input = $("#" + bomSearch[0]).val(bomSearch[1]);
	search($input);
}else
	search($('#rmaNumber').val('RMA'));

// Filter RMA units by shipping status
$('#rmaFilter').click(function(e){

	var $this = $(this);
	var text = $this.text();

	switch(text){

	case 'ALL':
		if(e.ctrlKey){
			text = 'SHI';
			$this.prop('title', 'Shipped\nPress to show All units.')
		}else{
			text = 'WOR';
			$this.prop('title', 'In Work\nPress to show READY to ship units.')
		}
		break;

	case 'WOR':
		if(e.ctrlKey){
			text = 'ALL';
			$this.prop('title', 'Press to show RMA units in work.')
		}else{
			text = 'REA';
			$this.prop('title', 'Ready to ship\nPress to show SHIPPED units.')
		}
		break;

	case 'REA':
		if(e.ctrlKey){
			text = 'WOR';
			$this.prop('title', 'In Work\nPress to show READY to ship units.')
		}else{
			text = 'SHI';
			$this.prop('title', 'Shipped\nPress to show All units.')
		}
		break;

	default:
		if(e.ctrlKey){
			text = 'REA';
			$this.prop('title', 'Ready to ship\nPress to show SHIPPED units.')
		}else{
			text = 'ALL';
			$this.prop('title', 'Press to show RMA units in work.')
	}
	}

	Cookies.set("rmafilter", text, { expires: 999 });
	$this.text(text);

	var cookie = Cookies.get("rmaSearch")
	if(cookie){
		var bomSearch = JSON.parse(cookie);
		var $input = $("#" + bomSearch[0]);
	}

    clearTimeout(timer);
    timer = setTimeout(search, 500, $input);

});
var timer;

$('#shipping').change(function(){

	if(this.checked)
		$('#rmaTextarea').val('Shipped');
	else
		$('#rmaTextarea').val('');
});

function addToRma(rmaNumber){

	var serialNumber = prompt('Enter the Unit Serial Number');
	if (serialNumber == null || serialNumber == "")
		return;

	$.post('/rma/rest/add_to_rma', { rmaNumber: rmaNumber, serialNumber: serialNumber})
	.done(function(message){

		if(message)
			alert(message);

		else{
			Cookies.set("rmaSearch", JSON.stringify(['rmaNumber', rmaNumber]), { expires: 7 });
			location.reload();
		}
	})
	.fail(function(error) {
		if(error.statusText!='abort')
			alert(error.responseText);
	});
}

$('input[name=sort_by]').change(function(){
	var id = $(this).prop('id');
	Cookies.set("rmaSorting", id, { expires: 7 });

	var cookie = Cookies.get("rmaSearch")
	if(cookie){
		var bomSearch = JSON.parse(cookie);
		var $input = $("#" + bomSearch[0]);
		search($input);
	}
});

$('#readyToShip').click(function(e){
	e.preventDefault();

	let rmaId = $('#saveComment').val();

	$.post('/rma/rest/ready_to_ship', { rmaId: rmaId})
	.fail(function(error) {
		if(error.statusText!='abort')
			alert(error.responseText);
	});
	location.reload();
});