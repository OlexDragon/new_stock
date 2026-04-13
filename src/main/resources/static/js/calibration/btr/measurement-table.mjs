import ModalLoader from '../modal-loader.mjs';
import {partNumber, localPartNumber, description} from '../cards/header-card.mjs';
import {id, serialNumber} from '../cards/onrender-card.mjs';

export default class MeasurementTable extends ModalLoader{

	#id;
	constructor(){
		super(`/calibration/btr?id=${id ?? 0}&sn=${serialNumber ?? 0}&pn=${partNumber ?? 0}&&localPN=${localPartNumber}&descr=${encodeURI(description)}`);
		this.#id = id;
	}
	show(){
		if(!this.#id){
			alert('Unit information not found.');
			return;
		}
		super.show();
	}
	_onLoad(){
		$('.add-template').click(uploadTemplate);
		$('.get-excel').click(toExcel).on({
	    mouseenter: e=>{
	    	if (e.ctrlKey){
	    		e.currentTarget.title = 'Add Template';
		        e.currentTarget.innerText = 'Save';
	    	}else if (e.shiftKey){
	    		e.currentTarget.title = 'Add Template';
		        e.currentTarget.innerText = 'Local';
	    	}
	    },
	    mouseleave: e=>{
			e.currentTarget.title = 'load Excel';
	        e.currentTarget.innerText = 'Excel';
	    }
	});
	}
}
function toExcel({ctrlKey, shiftKey, currentTarget:{innerText, dataset:{measId}}}){

	if (ctrlKey){
		uploadTemplate();
		return;

	}else if (shiftKey){
		uploadTemplate(undefined, localPartNumber);
		return;
	}
	const url = `/btr/rest/get-btr?sn=${serialNumber}&pn=${partNumber}&&localPN=${localPartNumber}&measId=${measId}`;
	downloadURL(url);
}
let input;
function uploadTemplate(_, localPN){
	if(input){
		input.click();
		return;
	}
	input = document.createElement('input');
	input.type = 'file';
	input.accept = '.xlsx';

	input.onchange = e => { 
		const fd = new FormData();
		fd.append('file', e.target.files[0]);
		fd.append('pn', partNumber);
		if(localPN)
			fd.append('localPN', localPN);

		const url = '/btr/rest/template/upload';
		postFormData(url, fd)
		.done(data=>{
			alert(data);
			location.reload();
		})
		.fail(function(error) {
			if(error.statusText!='abort'){
			var responseText = error.responseText;
				if(responseText)
					alert(error.responseText);
				else
					alert("Server error. Status = " + error.status)
			}
		});
	}

	input.click();
}
