import spServer, {subscribe as spSubscribe} from '../serial-port-server.mjs';
import Prologic, {subscribe as prologixSubscribe} from './prologix.mjs';
import CommandSender from './command-sender.mjs';
import {show as showToast} from '../../toast-worker.mjs';
import DestroyableComponent from '../destroyable-component.mjs';

export default class Tool extends DestroyableComponent{

	static portAddres = new Map();

	_$parent;
	_$tool;
	_$toolButons;
	_prologix;
	#subscribers = {};
	#сommandSender;
	#serialPort;

	#$selectSerialPort;
	#$toolSetting;
	#_pollWorker = null;
	#_pollCallback = null;
	#_currentPostRequest = null; // Track the current post request so it can be aborted

	constructor($parent, initInChild){
		super();
		this._timeout = 10000;
		this.#сommandSender = new CommandSender();
		this._$parent = $parent
		this.#$selectSerialPort = this.on($parent.find('.com-ports'), 'change', this._spChange.bind(this));
		this._$toolButons = this.on($parent.find('.tool-values button'), 'click', this.#toolBtnClick.bind(this));
		this.#$toolSetting = this.on($parent.prev().find('.tool-setting'), 'change', this.#saveCookies);
		this.#fillSelectTag(this.#$selectSerialPort);
		this._prologix = new Prologic($parent);
		this._prologix.portController(this);
		prologixSubscribe(this._showToolSettings.bind(this))
		spSubscribe(()=>this.#fillSelectTag(this.#$selectSerialPort));
		if(!initInChild)
			this._init();
	}
	_init(){
		this._$tool = this._$parent.find('.tool').change(this._showToolSettings.bind(this));
		const cookies = Cookies.get(this._$tool.prop('id'));
		if(cookies){
			this._$tool.val(cookies).change();
		}
	}

	startPolling(ms = 1000, cb){
		this.stopPolling();
		if(typeof cb !== 'function')
			throw new TypeError('startPolling: callback must be a function');
		this.#_pollCallback = cb;
		// Use DestroyableComponent.setInterval which delegates to timer-worker with fallback
		this.#_pollWorker = this.setInterval(() => { try{ this.#_pollCallback() }catch(err){ console.error('poll callback error', err) } }, ms);
	}

	stopPolling(){
		if(!this.#_pollWorker) return;
		try{ this.clearInterval(this.#_pollWorker); }catch(e){}
		this.#_pollWorker = null;
		this.#_pollCallback = null;
	}

	get(commands, callBack){
		console.error('Tool.get is deprecated, use Tool.post with getAnswer: true in command object', commands);
        return this.#сommandSender.get(this.port, commands, this._timeout, callBack);
    }
    post(commands, callBack){
        const request = this.#сommandSender.post(this.port, commands, this._timeout, callBack);
        this.#_currentPostRequest = request;
        // Clear the reference when the request completes
        if (request && typeof request.always === 'function') {
            request.always(() => {
                this.#_currentPostRequest = null;
            });
        }
        return request;
    }

	/**
	 * Abort any in-flight post request
	 */
	abort(){
		if(this.#_currentPostRequest){
			try{
				if(typeof this.#_currentPostRequest.abort === 'function') 
					this.#_currentPostRequest.abort();
			}catch(e){}
		}
		this.#_currentPostRequest = null;
	}

	get isReady(){

		if(!this._prologix.isReady){
			showToast('The Prologix is not ready', 'PROLOGIX GPIB-USB CONTROLLER is not ready', 'text-bg-warning');
			return false;
		}

		const toolCommand = this.command;
		if(!toolCommand){
			console.log("Tool not selected.");
			alert('Tool not selected.');
			return false;
		}

		const port = this.#serialPort;
		if(!port){
			console.warn("The Serial Port is not selected.");
			alert("The Serial Port is not selected.");
			return false;
		}
		return {toolCommand: toolCommand, port: port};
	}
	get addr(){
		return this._prologix.addr;
	}
	get toolName(){
		return this._$tool.children(':selected').text();
	}
	get command(){
		return this._$tool.val();
	}
	get readFormate(){
		return this._$tool.children(':selected').attr('data-read-format');
	}
	get portsOptions(){
		return this.#$selectSerialPort.children();
	}
	get port(){
		return this.#serialPort;
	}
	set port(portName){
		if(!portName)
			return;
		const option = this.#$selectSerialPort.children().get().find(el=>el.value===portName);
		if(!option){
			console.log(`The Serial Port "${portName}" not found.`)
			alert(`The Serial Port "${portName}" not found.`)
			return;
		}
		option.selected = true;
		this._showToolSettings();
	}
	show(){
		this._$parent.show();
	}
	subscribe(name, callback) {
		if (!this.#subscribers[name]) this.#subscribers[name] = [];
		this.#subscribers[name].push(callback);
	}
	unsubscribe(name, callback) {
		const index = this.#subscribers[name].indexOf(callback);
		if (index !== -1) {
		    this.#subscribers.splice(index, 1);
		}
	}
	_spChange({currentTarget:{id, value}}){
		if(value){
			Cookies.set(id, value, { expires: 99, path: '/calibration' });
			this._prologix.hide(value==='NI GPIB');
		}
		this.#serialPort = value;
		this._showToolSettings();
		return value;
	}
	#fillSelectTag($select){
		spServer.allPorts(ports=>ports && this.#fill($select, ports), true);
	}
	#fill($select, ports) {
	    if (!Array.isArray(ports)) {
	        console.error('There are no serial ports.', ports);
	        return;
	    }

	    $select.empty();

	    // Placeholder
	    $select.append(
	        $('<option>', {
	            text: 'Select Serial Port.',
	            value: '',
	            disabled: true,
	            hidden: true
	        })
	    );

	    // Add ports
	    ports.forEach(p => {
	        $select.append(
	            $('<option>', {
	                text: p,
	                value: p
	            })
	        );
	    });

	    // Restore previous selection if valid
	    this.#serialPort = Cookies.get($select.prop('id'));
	    if (ports.includes(this.#serialPort)) {
	        this._prologix.hide(this.#serialPort === 'NI GPIB');
	    }else
	        this.#serialPort = '';

		$select.val(this.#serialPort).change();
	}
	_showToolSettings(e){
		if(e){
			const {currentTarget:{id, value}} = e;
			if(value)
				Cookies.set(id, value, { expires: 99, path: '/calibration' })
		}
		const port = this.port;
		const toolName = this.toolName;
		const addr = this.addr;
		const message = ` SP: ${port ?? 'not set'} -> Tool: ${toolName ?? 'not set'} -> Addr: ${addr}`;
		this.#$toolSetting.text(message);
		if([port, toolName, addr].every(v=>!v))
			this.#$toolSetting.removeClass('text-info').addClass('text-danger');
		else if(!(port && toolName && addr))
			this.#$toolSetting.removeClass('text-danger').addClass('text-info');
		else
			this.#$toolSetting.removeClass('text-danger text-info');
			
	}
	#toolBtnClick({currentTarget:btn}){

		const ready = this.isReady;
		if(!ready)
			return;
		btn.disabled = true;
		const {port, toolCommand} = ready;
		if(port!=='NI GPIB'){
			const addr = this.addr;
			const settedAddr = Tool.portAddres.get(port);
			if(!settedAddr || settedAddr!==addr){
				Tool.portAddres.set(port, this.addr);
				this._prologix.sendAddr();
			}
		}

		let previousElement = btn.previousElementSibling;
		let option;
		if(previousElement.localName==='select'){
			option = previousElement.options[previousElement.selectedIndex];
			previousElement = previousElement.previousElementSibling;
		}
		const input = previousElement.querySelectorAll("input, select")[0];
		const commands = [this._toolCommand(toolCommand, input.value, btn.value, option)];	// input.value - value to set, btn.value = comsnd index
		// Use DestroyableComponent's setTimeout (worker-backed via timer-worker) so timers are tracked and cleaned up
		this.post(commands, r=>this.#callBack(btn, input, option, r));
	}
	_toolCommand(toolCommand){
		return {command: toolCommand, getAnswer: true};
	}
	#callBack(btn, input, select, response){
//		console.log('response', response);

		btn.disabled = false;

		if(!response.commands[0].getAnswer){
			this.setTimeout(()=>{
				input.value = '';
				btn.click();
			}, 100);
			return;
		}

		const answer = this.#сommandSender.checkResponse(response);
		if(answer === false)
			return;
		if(!answer){
			console.warn('No answer received for command', response);
			return;
		}

		const answerTxt = this._toValue(dataToValue(answer), input, select);
		input.value = answerTxt;
		input.dispatchEvent(new Event('input', { bubbles: true }));	// trigger any input listeners - switching button text to "Set" if value is existing
		const subscribers = this.#subscribers[input.id];
		if(subscribers)
			subscribers.forEach(pub=>pub(input.id, answerTxt));
	}
	_toValue(answer){
		return parseFloat(answer).toFixed(1);
	}
	#saveCookies({currentTarget:{id, value}}){
		Cookies.set(id, value, { expires: 99, path: '/calibration' });
	}
}

function dataToValue(answer){

	answer = $.trim(String.fromCharCode.apply(String, answer));

	const s = answer.split(/\s+/);
//	let a;
	switch(s.length){
	case 1:
		return answer;
	case 2:
		return s[1];
	default:
		return s[s.length-2];
	}
}