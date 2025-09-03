$('thead tr').dblclick(function(){
	_csrf= $( "input[name='_csrf']" ).val();
	$('#modal').load('/users/edit', {_csrf : _csrf});
})
$('#tableBody tr').dblclick(function(){
	var userId = $(this).data('value');
	_csrf= $( "input[name='_csrf']" ).val();
	$('#modal').load('/users/edit', {_csrf : _csrf, userId : userId});
})