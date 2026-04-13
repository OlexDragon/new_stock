import spServer from '../serial-port-server.mjs';
import CommandSender from './command-sender.mjs';
import {show as showToast} from '../../toast-worker.mjs';

export default class Prologix extends CommandSender{

	#$prologix;
	#$addr;
	#$btnAddr;
	#$buttons;
	#$btnGetAll;
	#portController;

	constructor($parent){
		super();
		this.#$prologix = $parent.find('.prologix');
		this.#$addr = this.#$prologix.find('input').change(this.#addressChange.bind(this));
		this.#$btnAddr = this.#$prologix.find('.b-addr').click(this.#btnAddrClick.bind(this));
		this.#$buttons = this.#$prologix.find('.b-prologix').click(this.post.bind(this));
		$parent.on('show.bs.collapse', this.#onCollapse.bind(this));
		this.#$btnGetAll = $parent.find('.b-get-all').click(this.#getAllClick.bind(this));
		$parent.find('.b-set-default').click(this.#setDefaultClick.bind(this));
		$parent.find('.b-reset').click(this.#resetClick.bind(this))
	}
	get isReady(){
		if(this.#$buttons.filter((_, el)=>el.offsetParent === null).length===3)
			return true;
		return this.#$buttons.filter('.btn-success').length===3
	}
	portController(controller){
		this.#portController = controller
	}
	hide(h){
		const toHide = this.#$prologix.find('button.col');
		if(h===false)
			toHide.show(500);
		else
			toHide.hide(500);
	}
	get addr(){
		return this.#$addr.val();
	}
	get port(){
		return this.#portController.port;
	}
	sendAddr(){
		this.#$btnAddr.click();
	}
	#addressChange({currentTarget:{value}}){
		subscribers.forEach(pub=>pub(value));
		this.#$btnAddr.click();
	}
	#btnAddrClick(){
		const addr = this.#$addr.val();
		let getAnswer;
		let commands
		if(addr){
			commands =  `["++addr ${addr}"]`;
			getAnswer = false;
		}else{
			commands =  '["++addr"]';
			getAnswer = true;
		}
		this.#post(this.#$addr[0], commands, getAnswer);
	}
	post({currentTarget:btn, currentTarget:{dataset:{commands, getAnswer}}}){
		this.#post(btn, commands, getAnswer);
	}
	#post(btn, commands, getAnswer){
		if(!this.#portController){
			console.error('The serial port controller has not been set.')
			alert('The serial port controller has not been configured.');
			return;
		}
		commands = JSON.parse(commands);
		const toSend = [];
		$.each(commands, function(_, command){
			const c = {};
			c.getAnswer = getAnswer;
			c.command = command;
			toSend.push(c);
		});
		super.post(this.#portController.port, toSend, 100, getAnswer ? this.#callBack.bind(btn) : undefined);
	}
	#resetClick({currentTarget:{dataset:{commands, getAnswer}}}){
		console.log(commands, getAnswer);
		this.#post(undefined, commands, getAnswer);
		setTimeout(()=>this.#$btnGetAll.click(), 100);
	}
	#callBack(response){

		const answer = super.checkResponse(response);
		if(!answer){
			const c = response?.commands[0];
			if(c && c.getAnswer === false){
				refreshBtn(this);
			}
			return;
		}

		const answerTxt = $.trim(String.fromCharCode.apply(String, answer));

		if(answerTxt == 'Unrecognized command'){

			let title = 'Unrecognized command.'
			let message = 'The Prologix MODE must be CONTROLLER.';
			console.log(title + ' : ' + message);
			showToast(title, message, 'text-bg-info');
			return;
		}
		if(this.localName==='input'){
			// Set address
			this.value = answerTxt;
			subscribers.forEach(pub=>pub(answerTxt));
			return;
		};
		const index = +answerTxt;
		const title = JSON.parse(this.dataset.title);
		this.innerText = JSON.parse(this.dataset.message)[index];
		this.title = JSON.parse(this.dataset.title)[index];
		Array.from(this.classList).filter(cls=>cls.startsWith('btn-')).forEach(cls=>{
				this.classList.remove(cls)});
		this.classList.add(JSON.parse(this.dataset.classes)[index]);
		this.setAttribute('data-commands', `["${JSON.parse(this.dataset.commandsToChoose)[index]}"]`);
		this.setAttribute('data-get-answer', false);
	}
	#onCollapse(){
		const port = this.#portController.port;
		if(port && port !== 'NI GPIB'){
			this.#$buttons.filter((_,btn)=>btn.dataset.getAnswer==='true').each((_,btn)=>btn.click());
		}
	}
	#getAllClick(){
		this.#$buttons.each((_,btn)=>refreshBtn(btn))
	}
	#setDefaultClick({currentTarget:{dataset:{commands, btnClass}}}){
		const cmnds = JSON.parse(commands);
		const cls = JSON.parse(btnClass);
		const t = cls.forEach((c,i)=>this.#$buttons.filter(`.${c}`).attr('data-commands', `["${cmnds[i]}"]`).attr('data-get-answer', false).click());
	}
}
const subscribers = [];
export function subscribe(callback) {
	subscribers.push(callback);
}
function refreshBtn(btn){
	const c = btn.dataset.commands;
	const i = c.indexOf(' ');
	if(i>0){
		const nc = c.slice(0, i) + '"]';
		btn.setAttribute('data-commands', `["${JSON.parse(nc)}"]`);
	}
	btn.setAttribute('data-get-answer', true);
	btn.click();

}
