let tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]')
let tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl))

const clientIP = Cookies.get('clientIP');
if(!clientIP)
	$.get('https://api.ipify.org', data=>Cookies.set('clientIP', data, { path: '' }));

const $accordion = $('#accordion');
const $sortBy = $('input[name=sort_by]');
const $rmaFilter = $('#rmaFilter');
const $rmaTextarea = $('#rmaTextarea');
const $saveComment = $('#saveComment');

const urlParams = new URLSearchParams(window.location.search);

// RMA Filter
let rmaFilter = urlParams.get('rmaFilter');
if(!rmaFilter)
	// Get RMA filter text from the cookies
	rmaFilter = Cookies.get('rmaFilter')
if(rmaFilter)
	$rmaFilter.text(rmaFilter);

// Get RMA sort by
let sortBy = urlParams.get('sortBy');
if(!sortBy)
	sortBy = Cookies.get('sortBy')
if(sortBy)
	$('#' + sortBy).prop('checked', true);

// Input listener
timer = 0;
const $searchRma = $('.searchRma').on('input', function(){

    if (timer) 
    	clearTimeout(timer);
 
    timer = setTimeout(search, 1000, $(this));
});

$(window).on('popstate',()=>{

	if(!history.state)
		return;

	$searchRma.val('');

	// RMA Filter
	let rmaFilter = urlParams.get('rmaFilter');
	if(rmaFilter)
		$rmaFilter.text(rmaFilter);

	// Get RMA sort by
	let sortBy = urlParams.get('sortBy');
	if(sortBy)
		$('#' + sortBy).prop('checked', true);

	let searchName = urlParams.get('searchName');
	let searchValue = urlParams.get('searchValue');
	let $input;

	if(searchName && searchValue){
		$input = $('#' + searchName).val(searchValue);
		search($input, false);
	}else{
		if(history.state.field_id && history.state.field_value)
			$input = $('#' + history.state.field_id).val(history.state.field_value);
	}

	if($input){
		clearTimeout(timer);
		timer = setTimeout(search, 500, $input);
	}
});

function search($this, saveCookies){

	if($addRMA.length)
		$addRMA.removeClass('btn-outline-primary').addClass('btn-secondary').prop('disabled', true);

	var val = tmp = $.trim($this.val());
	if(!tmp)
		return;

	let id = $this.prop('id');

// Remove extra whitespaces
	if(id!='rmaDescription' && id!='rmaComments')
		val = tmp.replace(/\s+/g, '');


	if(val.length!=tmp.length)
		$this.val(val);

	let attrId = $this.prop('id');

	if(saveCookies || typeof saveCookies === 'undefined'){

// Save Cookies
		let json = JSON.stringify([attrId, val]);
		Cookies.set('rmaSearch', json, { expires: 7, path: '' });
		urlParams.set('searchName', attrId);
		urlParams.set('searchValue', val);
		$searchRma.filter(':not(#' + attrId + ')').val('');

// Add to history
		let state = { field_id: id, field_value: val };
		let url = new URL(`${window.location.origin}${window.location.pathname}?${urlParams}`);
		history.pushState(state, '', url);
	}

// Sort By
	var $radio = $sortBy.filter(':checked');
	if(!$radio.length)	// If no one checked.
		$radio = $('#rmaOrderByRmaNumber').prop('checked', true);

	const sortBy = $radio.prop('id');
	const rmaFilter = $rmaFilter.text();

// Load RMAs
	$accordion.load('/rma/search', {id : attrId, value : val, sortBy: sortBy, rmaFilter: rmaFilter}, function(responseText){

		$('.tooltip').remove();
		tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]')
		tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl))

		if(!$addRMA.length || attrId != 'rmaSerialNumber' || $(responseText).filter('.accordion-item').length>1 || val.replace(/\D+/g, '').length!=7)
			return;

		$.get('/rma/rest/ready-to-add', { sn: val})
		.done(readyToAdd=>{
			if(readyToAdd)
				$addRMA.removeClass('btn-secondary').addClass('btn-outline-primary').prop('disabled', false);
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
	$saveComment.val(rmaId);
	$('#modal').modal('show');
}

const $addRMA = $('#addRMA').click(function(e){		// Button "Add" RMA Unit
	e.preventDefault();

	var val = $.trim($('#rmaSerialNumber').val());

	if($(this).hasClass('disabled')){
		alert('The serial number ' + val + ' does not exist.')
		return;
	}

	confirmAddRmaModal(val);
});

$saveComment.click(e=>{

	// return if file size is to large.
	let $maxSize = $('#maxSize');
	if($maxSize.length){
		alert($maxSize.text());
		return;
	}

	$saveComment.prop('disabled', true);

	let fd = new FormData();

	let files = $('#attachFiles').prop('files');
	for(const f of files)
		fd.append('fileToAttach[]', f);

	var comment = $.trim($rmaTextarea.val());

	if(comment){
		fd.append('comment', comment);
		$rmaTextarea.select();
		document.execCommand('copy');
	}

	const $checked = $rmaStatus.filter((i,el)=>el.checked);
	if($checked.length)
		fd.append('status', $checked.prop('id'));

// Return if values are not set.
	if(fd.keys().next().done)
		return;

	fd.append('rmaId', e.currentTarget.value);

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

const height = $('nav').height();
$accordion.on('shown.bs.collapse', function() {

	const $accordionItem = $(this).children().filter(function(){
		const $button = $(this).find('button');
		return $button.length && !$button.hasClass('collapsed');
	});
  	const $accordionBody = $accordionItem.find('.content');
  	const $children = $accordionBody.children();

	if($children.length){

		const offset = $accordionItem.offset();
		window.scrollTo({ top: offset.top - height , behavior: 'smooth'});
 		return;
 	}

	const id = $accordionItem.attr('id');
	$accordionBody.load('/rma/comments', {rmaId: id}, ()=>{

		const offset = $accordionItem.offset();
		window.scrollTo({ top: offset.top - height , behavior: 'smooth'});
	});
});

let searchName = urlParams.get('searchName');
let searchValue = urlParams.get('searchValue');
if(!(searchName && searchValue)){
	// Get Part Number, Mfr PN or Description from the cookies
	const rmaSearch = Cookies.get('rmaSearch');
	if(rmaSearch){
		const s = JSON.parse(rmaSearch);
		searchName = s[0];
		searchValue = s[1]
	}
}
if(searchName && searchValue){
	let $input = $('#' + searchName).val(searchValue);
	search($input, false);
}else
	search($('#rmaNumber').val('RMA'), false);	

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

	Cookies.set('rmaFilter', text, { expires: 999, path: '' });
	urlParams.set('rmaFilter', text);
	$this.text(text);

	// Add to history
	const url = new URL(`${window.location.origin}${window.location.pathname}?${urlParams}`);
	history.pushState({}, '', url);

	let $input = $searchRma.filter((i,el)=>el.value.length>0);
	if($input.length){
 	   clearTimeout(timer);
 	   timer = setTimeout(search, 500, $input);
	}

	$('.tooltip').remove();
});
var timer;

const $rmaStatus = $('input[name=rmaStatus]').change(e=>{

	if(!e.currentTarget.checked)
		return;

	const text = $rmaTextarea.val() + '\n\n' + e.currentTarget.ariaLabel;
	$rmaTextarea.val(text);
});

$('.modal-footer .btn-group').click(e=>{

	if(e.target.tagName != 'LABEL' || !e.target.previousElementSibling.checked)
		return;

	e.preventDefault();

	e.target.previousElementSibling.checked = false;
});

function addToRma(rmaNumber){

	var serialNumber = prompt('Enter the Unit Serial Number');
	if (serialNumber == null || serialNumber == '')
		return;

	$.post('/rma/rest/add_to_rma', { rmaNumber: rmaNumber, serialNumber: serialNumber})
	.done(function(message){

		if(message)
			alert(message);

		else{
			const json = JSON.stringify(['rmaNumber', rmaNumber]);
			Cookies.set('rmaSearch', json, { expires: 7, path: '' });
			location.reload();
		}
	})
	.fail(function(error) {
		if(error.statusText!='abort')
			alert(error.responseText);
	});
}

$sortBy.change(function(){
	const id = $(this).prop('id');
	Cookies.set('sortBy', id, { expires: 7, path: '' });
	urlParams.set('sortBy', id);

	const cookie = Cookies.get('rmaSearch')
	if(cookie){
		var rmaSearch = JSON.parse(cookie);
		var $input = $('#' + rmaSearch[0]);
 	   clearTimeout(timer);
 	   timer = setTimeout(search, 500, $input);
	}
	$('.tooltip').remove();
});

$('#readyToShip').click(function(e){
	e.preventDefault();

	let rmaId = $saveComment.val();

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
function thumbnailsClick(e, index, commentId, onWeb){

	if (e.detail === 1){
		e.preventDefault();
		thumbnailsTimeout = setTimeout(showThumbnails, 400, index, commentId, onWeb);
	}else
		clearTimeout(thumbnailsTimeout);
};

function showThumbnails(index, commentId, onWeb){
	$imgModal = $('#imgModal');
	$imgModal.load('/rma/show_img', {commentID: commentId, imgIndex: index, onWeb: onWeb}, function(){$imgModal.modal('show');})
}
// Copy content to the clipboard
$('.accordion').click(e=>{

	if(!e.ctrlKey || e.target.localName != 'strong')
		return;

	let input = document.createElement('input');
    input.setAttribute('value', e.target.innerText);
    document.body.appendChild(input);
    input.select();
	document.execCommand('copy');
    document.body.removeChild(input);
	showToast('Copy to Clipboard', `The text '${e.target.innerText}' has been copied to the clipboard.`, 'text-bg-success');
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
function confirmAddRmaModal(sn){

	const $m = $('<div>', {class: 'modal', tabindex: -1});
	const $btn = $('<button>', {type: 'button', class: 'btn btn-outline-primary', text: 'Create RMA', disabled: true});
	const $textarea = $('<textarea>', {id: sn, class: 'form-control', rows: 3, style: 'height:100%;', placeholder: 'Description of the malfunction'}).on('input', e=>$btn.prop('disabled', e.currentTarget.value.length==0));
	$btn.click(()=>{

		$addRMA.removeClass('btn-outline-primary').addClass('btn-secondary').prop('disabled', true);

		$.post('/rma/rest/add_rma', { serialNumber: sn, cause: $textarea.val()})
		.done(message=>{
			if(message.cssClass=='text-bg-success'){

				// Sort By
				var $radio = $sortBy.filter(':checked');
				if(!$radio.length)	// If no one checked.
					$radio = $('#rmaOrderByRmaNumber').prop('checked', true);

				var sortBy = $radio.prop('id');
				$accordion.load('/rma/search', {id : 'rmaSerialNumber', value : sn, sortBy: sortBy});

			}else
				$accordion.empty().append($('<div>', {class: message.cssClass + ' text-center p-3'}).append(message.message));
		})
		.fail(function(error) {
			if(error.statusText!='abort')
				alert(error.responseText);
		});

		$m.modal('hide');
	});
	$m.append(
				$('<div>', {class: 'modal-dialog'})
				.append(
					$('<div>', {class: 'modal-content'})
					.append(
						$('<div>', {class: 'modal-header'})
						.append($('<h5>', {class: 'modal-title', text: 'Create an RMA for ' + sn + '?'}))
						.append($('<button >', {type: 'button', class: 'btn-close', 'data-bs-dismiss': 'modal', 'aria-label': 'Close'})))
					.append(
						$('<div>', {class: 'modal-body'})
						.append(
							$('<div>', {class: 'form-floating'})
							.append($textarea)
							.append($('<label>', {for: sn, text: 'Description of the malfunction:'}))))
					.append(
						$('<div>', {class: 'modal-footer'})
						.append($('<button>', {type: 'button', class: 'btn btn-outline-secondary', 'data-bs-dismiss': 'modal', text: 'Cancel'}))
						.append($btn))));

	new bootstrap.Modal($m);
	$m.modal('show').on('hidden.bs.modal', ()=>$m.remove());
}