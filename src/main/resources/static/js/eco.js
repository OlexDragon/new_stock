// Input listener
timer = 0;
$('.search').on('input', function(){
    if (timer) {
    	clearTimeout(timer);
    }
    timer = setTimeout(search, 600, $(this));
});

var page;
var $lastSearch;
function search($this){

	page = 1;
	$lastSearch = $this;
	var val = $.trim($this.val());
	if(!val)
		return;

	var attrId = $this.prop('id');
	Cookies.set("ecoSearch", JSON.stringify([attrId, val]), { expires: 7, path: '' });
	$('.search').filter(':not(#' + attrId + ')').val('');

	if($this.hasClass('searchComponent'))
		$("#content").load('/components', {id: attrId, value: val});
	else
		$("#content").empty().append('<div><strong class="c-blue">To get information about ECOs, enter some information into one of the search fields.</strong></div><div id="searchEnd"></div>');

	if($this.hasClass('searchEco')){
		let showAll = $('#show_all_eco').prop('checked');
		$("#accordion").load('/eco', {id: attrId, value: val, showAll: showAll})
	}else
		$("#accordion").empty();
	
}
var thumbnailsTimeout;
function thumbnailsClick(e, index, ecoID){

	if (e.detail === 1){
		e.preventDefault();
		thumbnailsTimeout = setTimeout(showThumbnails, 50, index, ecoID);
	}else
		clearTimeout(thumbnailsTimeout);
};

function showThumbnails(index, ecoID){
	$imgModal = $('#imgModal');
	$imgModal.load('/eco/show_img', {ecoID: ecoID, imgIndex: index}, function(){$imgModal.modal('show');})
}

// Get Part Number, Mfr PN or Description from the cookies
var cookie = Cookies.get("ecoSearch")
if(cookie){
	var ecoSearch = JSON.parse(cookie);
	var $input = $("#" + ecoSearch[0]).val(ecoSearch[1]);
	search($input);
}

var lastScroll = 0;
var postComponents;
window.onscroll =  function (e) {

	var l = e.currentTarget.lastScroll;
	var st = window.pageYOffset || document.documentElement.scrollTop;
	var scrollUp = st <= lastScroll;
	lastScroll = st <= 0 ? 0 : st;

	if(scrollUp || $('#searchEnd').length || postComponents)
		return; //Scroll Up

    if ((window.innerHeight + window.pageYOffset) >= document.body.offsetHeight) {

		if(!$lastSearch)
			return;

		var attrId = $lastSearch.prop('id');
		var val = $.trim($lastSearch.val());
		postComponents = $.post('/components', {id: attrId, value: val, page: page})
							.done(function(page){
								$('#content').append(page);
								postComponents = 0;
							})
							.fail(function(error) {
								postComponents = 0;
								if(error.statusText!='abort')
									alert(error.responseText);
							});
		page += 1;
    }
}
function searchEco($this){
	if(!$this.hasClass('searchComponent'))
		$("#content").append('<div id="searchEnd"></div>')
}
function btnUseClick(row){

	let partNumber = $(row).find('.partNumber').text();
	if(!partNumber){
		alert('This component does not have a Part Number.');
		return;
	}
	
	$('#eco_modal_title').text('Create ECO for p/n: ' + partNumber);
	$('#saveECO').val(partNumber);
	$('#ecoModal').modal('show');
	}

$('#saveECO').click(function(e){
	e.preventDefault();

	// return if file size is to large.
	let $maxSize = $('#maxSize');
	if($maxSize.length){
		alert($maxSize.text());
		return;
	}

	let fd = new FormData();

	let files = $('#attachFiles').prop('files');
	for(const f of files)
		fd.append("fileToAttach[]", f);

	let $ecoCause = $('#ecoCause');
	let ecoCause = $.trim($ecoCause.val());

	if(ecoCause)
		fd.append("ecoCause", ecoCause);
	else{
		alert('All fields must be filled.');
		return;
	}

	let $ecoTextarea = $('#ecoTextarea');
	let ecoBody = $.trim($ecoTextarea.val());

	if(ecoBody){
		fd.append("ecoBody", ecoBody);
		$ecoTextarea.select();
		document.execCommand("copy");
	}else{
		alert('All fields must be filled.');
		return;
	}

	let url;
	let title = $('#eco_modal_title').text();
	if(title.startsWith('Edit')){
		url = '/eco/rest/edit_eco'
		fd.append('ecoID', this.value);
	}else{
		url = '/eco/rest/add_eco';
		fd.append('partNumber', this.value);
	}

	$.ajax({
    	url: url,
    	data: fd,
    	cache: false,
    	contentType: false,
    	processData: false,
    	method: 'POST',
    	type: 'POST', // For jQuery < 1.9
    	success: function(data){
    		console.log(data);
 	 		location.reload();
    	},
        error: function(error) {
			if(error.statusText!='abort')
				alert(error.responseText);
        }
	});
});

$('#attachFiles').on('input', function(){

	const maxFilesSize = 104857600;
	let $fileNames = $('#fileNames').empty();
	let files = $(this).prop('files');
	let totalSize = 0;

	for(const f of files){
		let name = f.name;
		$fileNames.append($('<div><strong>' + name + '<\strong></div>'));
		totalSize += f.size;
	};

	if(totalSize)
		$fileNames.append($('<div>Total size: ' + new Intl.NumberFormat().format(totalSize) + ' bytes.</div>'));

	if(totalSize>maxFilesSize)
		$fileNames.append($('<div id="maxSize">The maximum allowed file size is <strong style="color:red;">' + new Intl.NumberFormat().format(maxFilesSize) + '<\strong> bytes.</div>'));
});
$('.eco-field').on('input', function(){

	let ecoDescription = $('#ecoCause').val().trim();
	let ecoTextarea = $('#ecoTextarea').val().trim();

	if(ecoDescription && ecoTextarea)
		$("#saveECO").removeClass('disabled');
	else
		$("#saveECO").addClass('disabled');
});
$('#show_all_eco').click(function(){
	let i = $('.search').filter((index, input)=>input.value);
	if(i.length)
		search(i);
});
function editECO(button){

	let $accordionItem 	= $(button).closest('.accordion-item');
	let $parent 		= $(button.parentNode);

	let ecoID 		= $accordionItem.prop('id');
	let ecoNumber 	= $parent.find('.eco_number').text();
	let ecoDescr 	= $parent.find('.description').text();
	let ecoBody 	= $accordionItem.find('.eco_body').text();
	
	$('#eco_modal_title').text('Edit ' + $parent.find('.eco_number').text());
	$('#ecoCause').val(ecoDescr);
	$('#ecoTextarea').val(ecoBody);
	$('#saveECO').val(ecoID);
	$('#ecoModal').modal('show');
};
