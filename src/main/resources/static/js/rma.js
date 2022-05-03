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

$('#miRMAs').addClass('active');

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

	var val = $.trim($this.val());
	if(!val)
		return;

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
}

function showModal(rmaId, serialNumber){

	$('#modal_title').text(serialNumber);
	$('#saveComment').val(rmaId);
	$('#modal').modal('show');
}

$('#addRMA').click(function(e){
	e.preventDefault();

	var val = $.trim($('#rmaSerialNumber').val());
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
}

// Filter RMA units by shipping status
$('#rmaFilter').click(function(){

	var $this = $(this);
	var text = $this.text();

	switch(text){

	case 'ALL':
		text = 'WOR';
		$this.prop('title', 'Press to show SHIPPED units.')
		break;

	case 'WOR':
		text = 'SHI';
		$this.prop('title', 'Press to show All units.')
		break;

	default:
		text = 'ALL';
		$this.prop('title', 'Press to show RMA units in work.')
	}

	Cookies.set("rmafilter", text, { expires: 999 });
	$this.text(text);

	var cookie = Cookies.get("rmaSearch")
	if(cookie){
		var bomSearch = JSON.parse(cookie);
		var $input = $("#" + bomSearch[0]);
		search($input);
	}
});

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
