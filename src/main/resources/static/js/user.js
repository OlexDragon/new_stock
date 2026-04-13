$('thead tr').dblclick(function(){
	$('#modal').load('/users/edit');
});
$('#tableBody tr').dblclick(function(){
	var userId = $(this).data('value');
	$('#modal').load('/users/edit', {userId : userId});
});