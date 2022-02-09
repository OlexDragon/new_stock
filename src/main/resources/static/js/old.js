$('#pnName').on('change', function(){
	Cookies.set("oldPnName", this.value);
	fillTypes();
	toPartNumber();
});
var cookie = Cookies.get("oldPnName")
if(cookie){
	$('#pnName').val(cookie);
}

fillTypes();
function fillTypes(){

	var nameCode = parseInt($('#pnName').val());
	var names = pnNames.filter(pnName=> pnName.code == nameCode);

	if(names.length){

		var $pnType = $('#pnType').empty();
		var pnTypes = names[0].pnTypes.sort((a,b)=>a.code-b.code);
		pnTypes.forEach(function(type){
			$('<option>', {value: type.code}).text(type.code + ' - ' + type.type).appendTo($pnType);
		});
	}
}

$('#pnType').on('change', function(){
	toPartNumber();
});

toPartNumber();
function toPartNumber(){
	var pnNameCode = $('#pnName').val();
	var pnTypeCode = $('#pnType').val();
	var $pn = $('#componentPN');
	$pn.val(concatenateNameAndType(pnNameCode, pnTypeCode));
	search($pn);
	enableCreateButton();
}

// Input listener
timer = 0;
$('.searchInput').on('input', function(){

	enableCreateButton();

    if (timer) {
    	clearTimeout(timer);
    }
    timer = setTimeout(search, 800, $(this));
});

function enableCreateButton(){

	var $btn = $('#createBtn');
	if(!$btn.length)
		return;

	var pnNameCode = $('#pnName').val();
	var pnTypeCode = $('#pnType').val();
	var description = $('#description').val();
	var concat = concatenateNameAndType(pnNameCode, pnTypeCode);
	var pn = $('#componentPN').val();

	if(concat==pn && description){
		$btn.removeClass('disabled');
	}else{
		$btn.addClass('disabled');
	}
}

function search($this){

	var val = $.trim($this.val());
	if(!val)
		return;

	var attrId = $this.prop('id');

	$("#content").load('/old/search', {id: attrId, value: val});
}

$('#createBtn').click(function(){

	var pnNameCode = $('#pnName').val();
	var pnTypeCode = $('#pnType').val();
	var description = $('#description').val();

	if(!confirm('Create a new part number "' + concatenateNameAndType(pnNameCode, pnTypeCode) + '"?'))
		return;

	$("#content").load('/create', {pnNameCode: pnNameCode, pnTypeCode: pnTypeCode, description : description});
});

function concatenateNameAndType(name, type){
	return name + '-' + type;
}
