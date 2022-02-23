
// Input listener
timer = 0;
$('.searchRma').on('input', function(){

    if (timer) 
    	clearTimeout(timer);
 
    timer = setTimeout(search, 1000, $(this));
});

function search($this){

	var val = $.trim($this.val());
	var $addRMA = $('#addRMA');
	if(!val){
		if($addRMA.length){
			$addRMA.removeClass('btn-outline-primary');
			$addRMA.addClass('disabled btn-secondary');
		}
		return;
	}

	var attrId = $this.prop('id');
	Cookies.set("rmaSearch", JSON.stringify([attrId, val]), { expires: 7 });
	$('.searchRma').filter(':not(#' + attrId + ')').val('');

	var $accordion = $('#accordion');
	$accordion.load('/rma/search', {id : attrId, value : val}, function(){

		if($accordion.children().length){
			if($addRMA.length){
				$addRMA.removeClass('btn-outline-primary');
				$addRMA.addClass('disabled btn-secondary');
			}
			return;
		}

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

	var comment = $.trim($('#rmaTextarea').val());

	if(!comment)
		return;

	var rmaId = this.value;
	$('#accordion-body').load("/rma/add_comment", {rmaId: rmaId, comment: comment});
	$('#modal').modal('hide');
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
