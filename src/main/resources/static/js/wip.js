$('#miVIP').addClass('active');

let $modal = new bootstrap.Modal('#modal');
let $modalTextarea = $('#modalTextarea');
let $modalTitle = $('.modal-title');
let $modalPartNumber = $('#modalPartNumber');
let $modalCardIndex = $('#modalCardIndex');
let $fromWIP = $('#fromWIP');
let $fromLOG = $('#fromLOG');
let $toastContainer = $('#toast-container');

let $wipFiles = $('#wipFiles');
let $content = $('#content');

let $labels = $('label.btn');
$.post('/wip/rest/files', function(data){

	let type = typeof data;

	if(data && type == 'string'){
		$('#dropdownMenuButton').dropdown('toggle').parent().find('input[name=username]').focus();
		return;
	}

	$wipFiles.empty();

	if(!(data && data.length)){
		$('<option>', {checked:true, text: 'Files not found...'}).appendTo($wipFiles);
		return;
	}


	let d = data.map(d=>Object.entries(d)[0]);
	d.sort((a,b)=>b[1]-a[1]);

	d.forEach(([fileName, timestamp]) => {

		let date = new Date(timestamp);

		$('<option>', {value: fileName, text: fileName + ' - Modified: ' + date.toISOString().split('T')[0]}).appendTo($wipFiles);
 	})
	
	$wipFiles.first().prop('checked', true);
});

$('#wipFiles').change(loadContent);

var $wipWO = $('#wipWO')
				.focus(e=>{

					if($toastContainer.children().length)
						return;

					let focusValue = $wipWO.val();
					let $toast = addToast('Type the Work Order.', 'When you\'re done typing, don\'t forget to hit the ENTER key.', 'text-bg-info', 8000);
					$toast.on('hidden.bs.toast', e=>{
						let val = $wipWO.val();
						if(val != focusValue)
							loadContent(e);
					});
				})
				.change(e=>{
					$toastContainer.empty();
					loadContent(e);
				});

$wipWO.parents('form').submit(loadContent);

function loadContent(e){
	e.preventDefault();

	let wo = $wipWO.val().trim();
	if(!wo)
		return;

	$content.html('<div class="spinner-border" role="status"><span class="visually-hidden">Loading...</span></div>');

	let file = $wipFiles.val();

	$content.load('/wip/content', {file: file, wo: wo.toUpperCase()}, (data, status, req)=>{

		if(status == "error"){
        	showError(req);
			return;
		}

		$('.card-header button').click(function(){

			$modalTitle.text(this.value);
			let $cardBody = $(this).parents('.card-body');
			$modalCardIndex.val($cardBody.data('index'));

			let partNumber = $cardBody.find('.fromWIP .partNumber').text();
			let description = $cardBody.find('.fromWIP .description').text();

			$modalPartNumber.val(partNumber);
			$modalTextarea.val(description);

			$fromWIP.prop('checked', true);

			let hasLOG = $cardBody.find('.fromLOG .partNumber');
			if(hasLOG.length){
				$fromWIP.prop('disabled', false);
				$fromLOG.prop('disabled', false);
			}else{
				$fromWIP.prop('disabled', true);
				$fromLOG.prop('disabled', true);
			}

			$modal.show();	
		});
	});
}
$fromWIP.change(e=>modalContent(e));
$fromLOG.change(e=>modalContent(e));
function modalContent(e){

	let wo = $modalTitle.text();
	let index = $modalCardIndex.val()
	let $cardBody = $(`.card-body[data-index=${index}]`);

	let id = e.currentTarget.id;	
	let partNumber = $cardBody.find(`.${id} .partNumber`).text();
	let description = $cardBody.find(`.${id} .description`).text();

	$modalPartNumber.val(partNumber);
	$modalTextarea.val(description);
}
$('.modal-footer button[type=submit]').click(e=>{

	let toSend = {};
	let wo = toSend.wo = $modalTitle.text();
	toSend.wipFile = $wipFiles.val();

	if(!confirm(`Are you sure you want to save ${toSend.wo}?`)) return;

	toSend.partNumber = $modalPartNumber.val();
	toSend.description = $modalTextarea.val();

	let index = $modalCardIndex.val()
	let $cardBody = $(`.card-body[data-index=${index}]`);

	let $partNumber = $cardBody.find(`.fromLOG .partNumber`);
	if($partNumber.length){
		toSend.fromLOG = {};
		toSend.fromLOG.partNumber = $partNumber.text();
		toSend.fromLOG.description = $cardBody.find(`.fromLOG .description`).text();
	}

	toSend.fromWIP = {};
	toSend.fromWIP.partNumber = $cardBody.find(`.fromWIP .partNumber`).text();
	toSend.fromWIP.description = $cardBody.find(`.fromWIP .description`).text();
	$modal.hide();

	$.ajax('/wip/rest/save',{
		data : JSON.stringify(toSend),
		contentType : 'application/json',
		method : 'POST',
    	type: 'POST', // For jQuery < 1.9
    	success: function(data){
 
    		data.forEach((text,i)=>{

    			let colorClass;

    			if(text.includes('the same'))
    				colorClass = 'text-bg-primary';
    			else if(text.includes('been updated'))
     				colorClass = 'text-bg-success';
    			else
    				colorClass = 'text-bg-warning';

				let $toast = addToast(wo, text, colorClass, 16000);

	 			$toast.on('hide.bs.toast', e=>{
					$toastContainer.empty();
					loadContent(e);
	 			});
	    	});
    	},
        error: function(error) {
        	showError(req);
        }
    });
});

function addToast(headerText, text, colorClass, delay){
	console.log(`Showing the Toast-> HEADER: ${headerText}; TEXT: ${text}`);
	let $closeButton = $('<button>', {type: 'button', class: 'btn-close', 'data-bs-dismiss': 'toast', 'aria-label': 'Close'});
	let $toast = $('<div>', {class: 'toast', role: 'alert', 'aria-live': 'assertive', 'aria-atomic': 'true'}).append($('<div>', {class: `toast-header ${colorClass}`}).append($('<strong>', {class: 'me-auto', text: headerText})).append($closeButton)).append($('<div>', {class: 'toast-body', text: text}));
	$toast.on('hidden.bs.toast', e=>$toast.remove());
	$toastContainer.append($toast);
	bootstrap.Toast.getOrCreateInstance($toast, {delay: delay}).show();
	return $toast;
}
function showError(req){
	if(req.statusText == "error")
		$content.prepend($('<div>', {class: 'alert alert-danger', role: 'alert', text: `A Server Error has occurred. ( status = ${req.status} )`}));
}