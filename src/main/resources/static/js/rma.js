let tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]')
let tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl))

let $accordion = $('#accordion');
let $sortBy = $('input[name=sort_by]');
let $rmaFilter = $('#rmaFilter');

// Used in rmaComments.js script on rma.html
let clicked = false;

// Get RMA filter text from the cookies
let filterCookie = Cookies.get("rmafilter")
if(filterCookie){
	$rmaFilter.text(filterCookie);
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
	if(id=='rmaDescription' || id=='rmaComments')
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
	var $radio = $sortBy.filter(':checked');
	if(!$radio.length)	// If no one checked.
		$radio = $('#rmaOrderByRmaNumber').prop('checked', true);

	var sortBy = $radio.prop('id');
	var rmaFilter = $rmaFilter.text();

// Load RMAs

	$accordion.load('/rma/search', {id : attrId, value : val, sortBy: sortBy, rmaFilter: rmaFilter}, function(responseText, textStatus, req){

		$('.tooltip').remove();
		tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]')
		tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl))

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
	$rmaFilter.removeClass('btn-outline-primary');

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

	$accordion.load('/rma/add_rma', { serialNumber: val});
});

$('#saveComment').click(function(e){
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

	var $rmaTextarea = $('#rmaTextarea');
	var comment = $.trim($rmaTextarea.val());

	if(comment){
		fd.append("comment", comment);
		$rmaTextarea.select();
		document.execCommand("copy");
	}

	let shipped = $('#shipping').prop('checked');
	if(shipped)
		fd.append("shipped", true);
	else{
		let ready = $('#ready').prop('checked');
		if(ready)
			fd.append("ready", true);
	}

// Return if values are not set.
	if(fd.keys().next().done)
		return;

	fd.append('rmaId', this.value);

	$.ajax({
    	url: '/rma/rest/add_comment',
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
$accordion.on('shown.bs.collapse', function () {

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
$rmaFilter.click(function(e){

	var $this = $(this);
	var text = $this.text();

	switch(text){

	case 'ALL':
		if(e.ctrlKey){
			text = 'SHI';
			$this.prop('title', 'Shipped<br>Click to show All units.<br>Press CTRL to change direction.')
		}else{
			text = 'WOR';
			$this.prop('title', 'In Work<br>Click to show READY to ship units.<br>Press CTRL to change direction. ')
		}
		break;

	case 'WOR':
		if(e.ctrlKey){
			text = 'ALL';
			$this.prop('title', 'Click to show RMA units in work.<br>Press CTRL to change direction.')
		}else{
			text = 'REA';
			$this.prop('title', 'Ready to ship<br>Click to show SHIPPED units.<br>Press CTRL to change direction.')
		}
		break;

	case 'REA':
		if(e.ctrlKey){
			text = 'WOR';
			$this.prop('title', 'In Work<br>Click to show READY to ship units.<br>Press CTRL to change direction.')
		}else{
			text = 'SHI';
			$this.prop('title', 'Shipped<br>Click to show All units.<br>Press CTRL to change direction.')
		}
		break;

	default:
		if(e.ctrlKey){
			text = 'REA';
			$this.prop('title', 'Ready to ship<br>Click to show SHIPPED units.<br>Press CTRL to change direction.')
		}else{
			text = 'ALL';
			$this.prop('title', 'Click to show RMA units in work.<br>Press CTRL to change direction.')
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

	$('.tooltip').remove();
});
var timer;

$('#shipping').change(function(){

	let $ready = $('#ready').prop('disabled', this.checked);

	if(this.checked){
		$('#rmaTextarea').val('Shipped');
		$ready.prop('checked', false);
	}else
		$('#rmaTextarea').val('');
});

$('#ready').change(function(){

	const readyToShip = 'Ready to Ship.';
	let $rmaTextarea = $('#rmaTextarea');
	let text = $.trim($rmaTextarea.val());
	let includs = text.includes(readyToShip);

	if(this.checked){
		if(!includs)
			if(text.length)
				$rmaTextarea.val(text + '\n\n' + readyToShip);
			else
				$rmaTextarea.val(text + readyToShip);
	}else{
		if(includs){
			let t = text.split('\n').filter(l=>l != readyToShip).join("\n");
			$rmaTextarea.val(t);
		}
	}
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

$sortBy.change(function(){
	var id = $(this).prop('id');
	Cookies.set("rmaSorting", id, { expires: 7 });

	var cookie = Cookies.get("rmaSearch")
	if(cookie){
		var bomSearch = JSON.parse(cookie);
		var $input = $("#" + bomSearch[0]);
		search($input);
	}
	$('.tooltip').remove();
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

/*
$(window)
.on('dragenter',function(e) {
	e.preventDefault();
	e.stopPropagation();
})
.on('dragleave',function(e) {0
	e.preventDefault();
	e.stopPropagation();
})
.on('dragover',function(e) {
	e.preventDefault();
	e.stopPropagation();
})
.on('drop',function(e){
	e.preventDefault();
	e.stopPropagation();
});
$('#accordion')
.on('dragenter',function(e) {
	alert('You must be logged in to add files.');
});
*/

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
var thumbnailsTimeout;
function thumbnailsClick(e, index, commentId){

	if (e.detail === 1){
		e.preventDefault();
		thumbnailsTimeout = setTimeout(showThumbnails, 400, index, commentId);
	}else
		clearTimeout(thumbnailsTimeout);
};

function showThumbnails(index, commentId){
	$imgModal = $('#imgModal');
	$imgModal.load('/rma/show_img', {commentID: commentId, imgIndex: index}, function(){$imgModal.modal('show');})
}

// Copy content to the clipboard
$('.accordion').click(e=>{

	if(!e.ctrlKey && e.target.localName=='strong')
		return;
	let input = document.createElement('input');
    input.setAttribute('value', e.target.innerText);
    document.body.appendChild(input);
    input.select();
	document.execCommand('copy');
    document.body.removeChild(input);
	showToast("Copy to Clipboard", `The text '${e.target.innerText}' has been copied to the clipboard.`, 'text-bg-success');
});

let $toastContaner = $('#toast-container');
function showToast(title, message, headerClass){

	let $toast = $('<div>', {class: 'toast', role: 'alert', 'aria-live': 'assertive', 'aria-atomic': true})
		.append(
			$('<div>', {class: 'toast-header'})
			.append(
				$('<strong>', {class: 'me-auto', text: title})
			)
			.append(
				$('<button>', {class: 'btn-close', type: 'button', 'data-bs-dismiss': 'toast', 'aria-label': 'Close'})
			)
		)
		.append(
			$('<div>', {class: 'toast-body', text: message})
		)
	.appendTo($toastContaner)
	.on('hide.bs.toast', function(){this.remove();});

	if(headerClass)
		$toast.find('.toast-header').addClass(headerClass);

	new bootstrap.Toast($toast).show();
}
