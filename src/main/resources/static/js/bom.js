
$('#miBOMs').addClass('active');

// Input listener
timer = 0;
$('.searchInput').on('input', function(){
    if (timer) {
    	clearTimeout(timer);
    }
    timer = setTimeout(search, 800, $(this));
});

function search($this){

	var val = $.trim($this.val());
	if(!val)
		return;

	if(window.location.search)
		window.history.pushState({}, document.title, window.location.href.split("?")[0] );

	var attrId = $this.prop('id');
	Cookies.set("bomSearch", JSON.stringify([attrId, val]), { expires: 7, path: '' });
	$('.searchInput').filter(':not(#' + attrId + ')').val('');

	$('#accordion').load('/bom/search', {id : attrId, value : val});
}

$('#accordion').on('shown.bs.collapse', function () {

	var accordionItem = $(this).children().filter(function(index,a){return !$(this).find('button').hasClass('collapsed');});
  	var $cardBody = accordionItem.find('.accordion-body');
  	let childrenLength = $cardBody.children().length;

	if(childrenLength)
 		return;

	var bomKey = accordionItem.attr('data-bom-key');
	$cardBody.load('/bom/components', {bomKey: bomKey});
});

if(!window.location.search){
	// Get Part Number, Mfr PN or Description from the cookies
	var cookie = Cookies.get("bomSearch")
	if(cookie){
		var bomSearch = JSON.parse(cookie);
		var $input = $("#" + bomSearch[0]).val(bomSearch[1]);
		search($input);
	}
}

