import {show as showToast} from '../../toast-worker.mjs';
import spServer from '../serial-port-server.mjs';

export default class CommandSender{

	#xhr;
	constructor(){
		this.#xhr = new Set();
	}

	get(serialPort, commands, timeout, callBack) {
	    this.#validateAndSend($.get, serialPort, commands, timeout, callBack);
	}

	post(serialPort, commands, timeout, callBack) {
	    this.#validateAndSend(postJson, serialPort, commands, timeout, callBack);
	}
	abortAll() {
	    for (const xhr of this.#xhr) {
	        xhr.abort();
	    }
	    this._active.clear();
	}
	abortLast() {
	     const last = [...this.#xhr].pop();
	     if (last) {
	         last.abort();
	         this.#xhr.delete(last);
	     }
	 }
	checkResponse(response){

		if(response.startsWith && response.startsWith('error: '))
			return {error: response};
		if(!(response && Object.keys(response).length)){
			console.warn('something went wrong for ', this.innerText, response);
			showToast('Read Data ERROR', 'something went wrong for ' + this.innerText, 'text-bg-danger');
			return false;
		}
		const command = response?.commands[0];
		if(!command){
			console.warn('something went wrong for ', this.innerText);
			showToast('Read Data ERROR', 'something went wrong for ' + this.innerText, 'text-bg-danger');
			return;
		}else if(command.errorMessage){
			console.warn(this.innerText, command.command, command.errorMessage);
			showToast(`Read ${command.command} ERROR`, `${this.innerText}\n${command.errorMessage}`, 'text-bg-danger');
			return;
		}else if(!command.answer){
			if(command.getAnswer){
				console.warn(this.innerText, command.command, ` - No Answer`);
				showToast(`No Answer`, `${this.innerText}\n${command.errorMessage}`, 'text-bg-danger');
			}
			return;
		}
		return command.answer;
	}
	#validateAndSend(sender, serialPort, commands, timeout, callBack) {
//		console.warn('CommandSender.#validateAndSend', {sender, serialPort, commands, timeout, callBack});

	    const fail = msg => {
	        console.warn(msg);
	        alert(msg.replace('error: ', ''));
			// Only call callback if it exists AND is a function
			 if (typeof callBack === 'function') {
			     callBack({ error: code, message });
			 }

	        return false;
	    };

	    if (!spServer.serverName)
	        return fail('error: The server name is not defined.');

	    if (!serialPort)
	        return fail('error: Serial Port is not selected.');

	    if (!Array.isArray(commands))
	        return fail('error: The GPIB Command is not set.');

		if(typeof timeout !== 'number' || timeout <= 0)
			return fail('error: The timeout must be a positive number.');

//	    if (callBack || typeof callBack !== 'function')
//	        return fail('error: callBack is not a function.');

	    // All good → send
	    this.#track(
	        sendCommands(sender, spServer.serverName, serialPort, this.addr, commands, timeout, callBack)
	    );
	}
	#track(xhr) {
    	if (!xhr) return;
    	this.#xhr.add(xhr);

    	// Remove from set when finished
    	xhr.always(() => this.#xhr.delete(xhr));
	}
}

function sendCommands(sender, server, serialPort, addr, commands, timeout, callBack){

	if(!readyToSend(server, serialPort, commands))
		return;

	const toSend = {
	        spName: serialPort,
	        commands,
	        addr,
	        timeout
	    };


	// Use single `.then(success, failure)` handler instead of chaining `.then().fail()` for brevity
	return sender(`http://${server}`, toSend).always(callBack);
}
function readyToSend(server, serialPort, command){

	if(!server){
		console.log("The server name is not defined.");
		alert('The server name is not defined.');
		return false;
	}

	if(!serialPort){
		console.warn('Serial Port is not selected.');
		alert('Serial Port is not selected.');
		return false;
	}

	if(!command){
		console.log("The GPIB Command is not set.");
		alert('The GPIB Command is not set.');
		return false;
	}
	return true;
}
export function postJson(url, object){
	var json = JSON.stringify(object);

	return $.ajax({
		url: url,
		type: 'POST',
		contentType: "application/json",
		data: json,
	    dataType: 'json'
	});
}