
var cookie = Cookies.get("outputPowermeter")
if(cookie){
	try {
		$('option[value=' + cookie + ']').prop('selected', true);
	}catch(err) {}
}
$('#outputPowermeter').change(function(){
	Cookies.set("outputPowermeter", this.value, { expires: 999 })
});

$('#ouputGet').click(function(){

	var copPort = $('#outputComPorts').val();
	var powermeter = $('#outputPowermeter').val();

	if(!(copPort && powermeter)) return;

	
});