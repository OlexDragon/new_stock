
// Input listener
timer = 0;
$('.searchInput').on('input', function(){
    if (timer) {
    	clearTimeout(timer);
    }
    timer = setTimeout(search, 600, $(this));
});

var $post;
var showAlert = true;
function search($this){

	var val = $.trim($this.val());
	if(!val)
		return;

	if($post){
		$post.abort();
		$post = null;
	}

	var attrId = $this.prop('id');
	Cookies.set("componentSearch", JSON.stringify([attrId, val]), { expires: 7 });
	$('.searchInput').filter(':not(#' + attrId + ')').val('');

	$post = $.post('/components', {id : attrId, value: val})
	.done(function(data){

		var $content = $("#content").empty();
		var components = data.value;

		$("<div>", { class: "row mr"})
		.append($("<div>", { class: "col-sm-2",  }).append($('<strong>').text('Part Number')))
		.append($("<div>", { class: "col-sm-2" }).append($('<strong>').text('Mfr PN')))
		.append($("<div>", { class: "col-sm" }).append($('<strong>').text('Description')))
		.appendTo($content);

		$.each(components, function(index, component){
			var ood = '';

			if(index % 2 == 0)
				ood = ' ood'

			var $row = $("<div>", { class: "row hover mr clickable" + ood, id: component.Ref_Key, onclick: 'onClick(this.id)'});
			$row.append($("<div>", { class: "col-sm-2 partNumber",  }).text(component.SKU));
			$row.append($("<div>", { class: "col-sm-2 mfrPN" }).text(component.MfrPNs));
			$row.append($("<div>", { class: "col-sm description" }).text(component.Description));
			$row.appendTo($content);
		});
		$post = null;
	})
	.fail(function(error) {
		if(error.statusText!='abort' && showAlert){
			showAlert = false;
			alert(error.responseText);
			showAlert = true;
		}
		$post = null;
	});
}

// Get Part Number, Mfr PN or Description from the cookies
var cookie = Cookies.get("componentSearch")
if(cookie){
	var componentSearch = JSON.parse(cookie);
	var $input = $("#" + componentSearch[0]).val(componentSearch[1]);
	search($input);
}
