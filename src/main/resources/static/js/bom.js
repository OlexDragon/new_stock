
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
	Cookies.set("bomSearch", JSON.stringify([attrId, val]), { expires: 7 });
	$('.searchInput').filter(':not(#' + attrId + ')').val('');

	$('#accordion').load('/bom/search', {id : attrId, value : val});
}

$('#accordion').on('show.bs.collapse', function () {

	var $activeCard = $(this.children)
  	var $cardBody = $activeCard.find('.accordion-body');

	if($cardBody.children().length)
 		return;

	var bomKey = $activeCard.attr('data-bom-key');
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

