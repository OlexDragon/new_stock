
// Input listener
timer = 0;
$('.searchInput').on('input', function(){
    if (timer) {
    	clearTimeout(timer);
    }
    timer = setTimeout(search, 600, $(this));
});

function search($this){

	var val = $.trim($this.val());
	if(!val)
		return;

	var attrId = $this.prop('id');
	Cookies.set("componentSearch", JSON.stringify([attrId, val]), { expires: 7 });
	$('.searchInput').filter(':not(#' + attrId + ')').val('');

	$("#content").load('/components?id=' + attrId + '&value=' + val)
}

// Get Part Number, Mfr PN or Description from the cookies
var cookie = Cookies.get("componentSearch")
if(cookie){
	var componentSearch = JSON.parse(cookie);
	var $input = $("#" + componentSearch[0]).val(componentSearch[1]);
	search($input);
}

function inventory(row){

	var $row = $(row);
	var partNumber = $row.find('.partNumber').text();
	var description = $row.find('.description').text();

	$('#modal_title').text(partNumber + ' : ' + description);
	var $body = $('#modal-body').empty();

	var $uSerialNumber = $('<input>', {class: 'form-control', id: 'snsUnit', type: 'text', placeholder: 'Unit Serial Number'});

	$('<div>', {class: 'input-group mb-3'})
	.append($('<label>', {class: 'input-group-text', for: 'snsInvertory'}).text('USNs:'))
	.append($uSerialNumber)
	.appendTo($body);

	var $serialNumbers = $('<input>', {class: 'form-control', id: 'snsInvertory', type: 'text', placeholder: 'Component Serial Numbers'});

	$('<div>', {class: 'input-group mb-3'})
	.append($('<label>', {class: 'input-group-text', for: 'snsInvertory'}).text('CSNs:'))
	.append($serialNumbers)
	.appendTo($body);

	var $qty = $('<input>', {class: 'form-control', id: 'qtyInvertory', type: 'number', placeholder: 'Quantity'}).val(1);

	$('<div>', {class: 'input-group mb-3'})
	.append($('<label>', {class: 'input-group-text', for: 'qtyInvertory'}).text('Quantity:'))
	.append($qty)
	.appendTo($body);

	var $comments = $('<input>', {class: 'form-control', id: 'descrInvertory', type: 'text', placeholder: 'Work Order, Serial Number, Comments'});

	$('<div>', {class: 'input-group'})
	.append($('<label>', {class: 'input-group-text', for: 'descrInvertory'}).text('Comm.:'))
	.append($comments)
	.appendTo($body);


	var $btnSend = $('<button>', { type:"button", class:"btn btn-primary disabled"}).text('Send');

	$('.modal-footer').empty().append($btnSend)
	.append($('<button>', { type:"button", class:"btn btn-secondary", 'data-bs-dismiss':"modal" }).text('close'));

	$('#modal').modal('show');

	$qty.on('input', function () {

		var value = this.valueAsNumber;
		if(value<1)
			this.value = 1;
	});

	$comments.on('input', function () {

		var value = this.value;
		if(value)
			$btnSend.removeClass('disabled');
		else
			$btnSend.addClass('disabled');
	});

	$btnSend.click(function(){

		$('#modal').modal('hide');

		var usn = $uSerialNumber.val();
		var id = $row.attr('id');
		var userName = $('#dropdownMenuButton').text();
		var qty = $qty.val();
		var sns = $serialNumbers.val();
		var comments = $comments.val();

		$.post('/invertory/rest', {unitSerialNumber: usn, key : id, userName: userName, qty: qty, serialNumbers: sns, comments: comments})
		.done(function(){
		})
		.fail(function(error) {
			if(error.statusText!='abort')
				alert(error.responseText);
		});
	});
}