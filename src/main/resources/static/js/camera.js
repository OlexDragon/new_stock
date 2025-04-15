
const md = navigator.mediaDevices;
const video = document.getElementById("video");

if(video)
	navigator.mediaDevices.getUserMedia({ audio: false, video: {  facingMode: 'environment' }})
	.then((stream)=>{
		video.srcObject = stream;
	}).catch(error=>{
		console.log(error);
		alert(error);
	});

$('#snapshotBtn').click(snapshot);
function snapshot(){
	if(!video){
		alert('The video element does not exist.');
		return;
	}
	const canvas = document.getElementById('canvas');
	canvas.width = video.videoWidth;
	canvas.height = video.videoHeight;
	const ctx = canvas.getContext('2d');

	ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
	const fd = new FormData();
	fd.append('snapshot', canvas.toDataURL('image/png', 1));

	const url = window.location.pathname.replace('/rma/camera/', '/rma/rest/camera/');
	postFormData(url, fd)
	.done(message=>{
		if(message)
			alert(message);
	});
}
function postFormData(url, formData){
	return $.ajax({
		url: url,
		type: 'POST',
		processData: false,
		contentType: false,
		data: formData
	});
}
function message(object){
	const $header = $('<h4>');
	$('body').append($header);

	if(typeof object === 'string')
		$header.text(object);

	else if(typeof object === 'object'){
		const text = JSON.stringify(object);
		$header.text(text);

	}else
		$header.text(typeof object);
}
$('#attachImages').on('input', e=>{
	const fd = new FormData();
	fd.append('file', e.currentTarget.files[0]);
	const url = window.location.pathname.replace('/rma/camera/', '/rma/rest/camera/fd/');
	postFormData(url, fd)
	.done(message=>{
		if(message)
			alert(message);
	});
});
setTimeout(()=>window.top.close(), 30*60*1000);