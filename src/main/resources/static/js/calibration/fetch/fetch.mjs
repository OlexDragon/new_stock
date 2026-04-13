import {unitSerialNumber} from '../../calibration.mjs'

export default class Fetch{

	#path;

	constructor(path){
		this.#path = path;
	}

	async receive(){
//		console.log(`Receiving data from path: ${this.#path}...`);
		return await $.get(`calibration/rest/${this.#path}`, {sn: unitSerialNumber});
	}
}
export function ajaxJSON(url, object){
	var json = JSON.stringify(object);

	return $.ajax({
		url: url,
		type: 'POST',
		contentType: "application/json",
		data: json,
	    dataType: 'json'
	});	
}

 export function connectionFail(error) {
	const errorCode = error.getResponseHeader('error-code');
	if(errorCode && errorCode<0){
		alert(error.getResponseHeader('error-line'));
		return true;
	}

	if(error.statusText!='abort'){
		if(error.responseText)
			alert(error.responseText);
		else if(error.statusText)
			alert(error.statusText);
		else{
			let status;
			switch(error.status){
			case 0: status = 'Connection Refused.';
					break;
			default: status = error.status;
		}
		alert("Server error. Status = " + status);
		}
		return true;
	}
	return false;
}
