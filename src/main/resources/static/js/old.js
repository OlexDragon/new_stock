
$('#miOldDB').addClass('active');

$('#pnType').on('change', function(){

	Cookies.set("oldPnType", this.value, { path: '' });
	getPnNames();
});

var cookie = Cookies.get("oldPnType")
if(cookie)
	$('#pnType').val(cookie);

$('#pnName').on('change', function(){
	Cookies.set(cookie + "oldPnName", this.value);
	$('#componentPN').val(this.value);
	getPnFields();
});

getPnNames();

function getPnNames(){

	var $pnType = $('#pnType');
	var typeId = $pnType.val();

	$.post('/create/rest/get_names', { pnTypeId : typeId})
	.done(function(pnNames){

		var $pnName = $('#pnName').empty();
		pnNames.forEach(function(name){
			$('<option>', {value: name.code}).text(name.name).appendTo($pnName);
		});

		var cookie = Cookies.get(typeId + "oldPnName")
		if(cookie)
			$pnName.val(cookie);

		getPnFields();

	})
	.fail(function(error) {
		if(error.statusText!='abort')
			alert(error.responseText);
	});
}

function getPnFields(){

	$('.pn_field').remove();
	$pnName = $('#pnName');
	var nameCode = $pnName.val();

	if(!nameCode){
		$('#componentPN').val("");
		enableCreateButton();
		return;
	}

	$.post('/old/get_fields', { pnNameCode : nameCode})
	.done(function(fields){

		$pnName.parent().after(fields);

		toPartNumber();
	})
	.fail(function(error) {
		if(error.statusText!='abort')
			alert(error.responseText);
	});
}

function toPartNumber(){

	enableCreateButton();

	var $pn = $('#componentPN');
	var pnNameCode = $('#pnName').val();

	if(!pnNameCode)
		return;

// To Part Number
	var val = pnNameCode;

	var pnSubtype1Code = $('#pnSubtype1').val();
	if(pnSubtype1Code)
		val += '-' + pnSubtype1Code

	var pnSubtype2Code = $('#pnSubtype2').val();
	if(pnSubtype2Code)
		val += '-' + pnSubtype2Code;

	val += '-#'

	var pnSubtype3Code = $('#pnSubtype3').val();
	if(pnSubtype3Code)
		val += '-' + pnSubtype3Code;

	$pn.val(val);

	search($pn);
}

var timer = 0;
$('.searchInput').on('input', function(){

	enableCreateButton();

    if (timer) 
    	clearTimeout(timer);

    timer = setTimeout(search, 800, $(this));
});

function search($this){

	var val = $.trim($this.val());
	if(!val)
		return;

	var attrId = $this.prop('id');

	$("#content").load('/old/search', {id: attrId, value: val});
}

function enableCreateButton(){

	var $btn = $('#createBtn');
	if(!$btn.length)
		return;

	var pnNameCode = $('#pnName').val();
	var description = $('#description').val();

	if(pnNameCode && description){
		$btn.removeClass('disabled');
	}else{
		$btn.addClass('disabled');
	}
}

$('#createBtn').click(function(){

	toPartNumber();
	var pnTypeCode = $('#pnType').val();
	var description = $('#description').val();
	var pn = $('#componentPN').val();

	if(!confirm('Create a new part number "' + pn + '"?'))
		return;

	$("#content").load('/create', {pnTypeCode: pnTypeCode, newPartNumber: pn, description : description});
});
