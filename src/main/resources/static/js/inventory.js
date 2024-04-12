
$('#miInventory').addClass('active');

// Input listener
timer = 0;
$('.searchInput').on('input', function(){
    clearTimeout(timer);
    timer = setTimeout(search, 1000, $(this));
});

function search($this){

	var val = $.trim($this.val());
	if(!val)
		return;

	var attrId = $this.prop('id');
	Cookies.set("inventorytSearch", JSON.stringify([attrId, val]), { expires: 7, path: '' });
	$('.searchInput').filter(':not(#' + attrId + ')').val('');

	$("#accordion").load('/inventory', {name: attrId, value: val});
}

// Get Part Number, Mfr PN or Description from the cookies
var cookie = Cookies.get("inventorytSearch")
if(cookie){
	var inventorytSearch = JSON.parse(cookie);
	var $input = $("#" + inventorytSearch[0]).val(inventorytSearch[1]);
	search($input);
}

function inventory(row){

	var $row = $(row);
	var partNumber = $row.find('.partNumber').text();
	var description = $row.find('.description').text();

	$('#modal_title').text(partNumber + ' : ' + description);
	var $body = $('#modal-body').empty();

// Quantity
	var $qty = $('<input>', {class: 'form-control', id: 'qtyInventory', type: 'number', placeholder: 'Quantity'}).val(1);

	$('<div>', {class: 'input-group mb-3'})
	.append($('<label>', {class: 'input-group-text', for: 'qtyInventory'}).text('Quantity:'))
	.append($qty)
	.appendTo($body);

// Comments
	var $comments = $('<input>', {class: 'form-control', id: 'descrInventory', type: 'text', placeholder: 'Work Order, Serial Number, Comments'});

	$('<div>', {class: 'input-group'})
	.append($('<label>', {class: 'input-group-text', for: 'descrInventory'}).text('Comm.:'))
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

		var id = $row.attr('id');
		var userName = $('#dropdownMenuButton').text();
		var qty = $qty.val();
		var comments = $comments.val();

		$.post('/inventory/rest', { productKey : id, userName: userName, qty: qty, comments: comments})
		.done(function(inventoryTransfer){
			var t = inventoryTransfer;
		})
		.fail(function(error) {
			if(error.statusText!='abort')
				alert(error.responseText);
		});
	});
}