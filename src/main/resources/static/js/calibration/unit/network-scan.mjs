import ModalLoader from '../modal-loader.mjs';

export default class NetworkScan extends ModalLoader{

	#$btnStop;
	#$inputStart;
	#$inputEnd;
	#$modalBody;

	constructor(){
		super(`/calibration/modal/calibration/modal`);
	}
	_onLoad(){

		this.on(this._$modal, 'hide.bs.modal', this._onHide.bind(this));
		this.#$btnStop = this.on($('<button>', { type: 'button', class: 'btn btn-outline-primary', text: 'Stop' }), 'click', this.#btnStopClick.bind(this));
		this.#$inputStart = this.on($('<input>', { type: 'number', class: 'col form-control', placeholder: 'Start IP', title: 'Scan from', value: '2'}), 'change', this.#limitChange.bind(this));
		this.#$inputEnd = this.on($('<input>', { type: 'number', class: 'col form-control', placeholder: 'End IP', title: 'Scan to', value: '254'}), 'change', this.#limitChange.bind(this));

		this._$modal.find('.modal-title').text('IP Scanner');
		this.#$modalBody = this._$modal.find('.modal-body');
		this._$modal.find('.modal-footer')
		.empty()
		.append(this.#$inputStart)
		.append(this.#$inputEnd)
		.append(this.#$btnStop)
		.append($('<button>', { type: 'button', class: 'btn btn-secondary', 'data-bs-dismiss': 'modal', text: 'Close' }));

		this.scan();
	}
	_onHide(){
		this._stop = true;
	}
    #limitChange() {
        const start = +this.#$inputStart.val() || 2;
        if (start < 2)
            this.#$inputStart.val(2);
        else if (start > 254)
            this.#$inputStart.val(254);
        const end = +this.#$inputEnd.val() || 254;
        if (end < start)
            this.#$inputEnd.val(start);
        else if (end > 254)
            this.#$inputEnd.val(254);
    }
	async scan() {
		this._stop = false;
		this.#$modalBody.empty();
		let start = +this.#$inputStart.val() || 2;
		const end = +this.#$inputEnd.val() || 254;
		const ip = '192.168.30.';
		for(; start<=end; ){
			if(count >= 10){	// limit concurrent requests
				await delay(50);
				continue;
			}
			if(this._stop)
				break;
			const toScan = ip + start;
			const $row = $('<div>', {class: 'row mb-1'}).append($('<div>', {class: 'col'}).append($('<a>', {text: toScan, href: 'http://' + toScan, target: '_blank'})));
			setTimeout(() => {this.#$modalBody.append($row);}, 5);
			sendRequest(toScan, $row);
			start++;
			await delay(150);
		}
		this.#$btnStop.text('Start');
	}
    #btnStopClick({ currentTarget: btn }) {
        if (btn.innerText === 'Stop') {
			this._stop = true;
        }else{
			btn.innerText = 'Stop';
			this.scan();
		}
    }
	destroy() {
		super.destroy();
		this.#$btnStop = null;
		this.#$inputStart = null;
		this.#$inputEnd = null;
		this.#$modalBody = null;
	}
}

let count = 0;
async function sendRequest(ip, $row) {
	count++;
	const result = await $.post('/calibration/rest/scan', {ip: ip})
	count--;
	const sn = result?.sysInfo?.sn;
	if(!sn){
		setTimeout(()=>$row.remove(), 5);
		return;
	}
	setTimeout(() =>
		$row.append(
			$('<div>', {class: 'col'})
			.append($('<a>', {text: sn, href: 'http://' + sn, target: '_blank', title: result.sysInfo.desc})))
		.append(
			$('<div>', {class: 'col-auto'})
			.append($('<button>', {class: 'btn btn-sm btn-outline-primary', text: 'Singn in', value: ip}).click(btnSigninClick)))
		.append(
			$('<div>', {class: 'col-auto'})
			.append($('<a>', {class: 'btn btn-sm btn-outline-success', text: 'Cal', href: '/calibration?sn=' + ip, target: '_blank'}).click(btnSigninClick))), 5);
}
function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}
function btnSigninClick({value}){
	signIn(value, (data)=>{
		let title;
		let bsClass;
		if(data.startsWith('Error')){
			bsClass = 'text-bg-danger';
			title = 'Sign-in Failed';
		} else {
			bsClass = 'text-bg-success';
			title = 'Sign-in Successful';
		}
		toast(title, data, bsClass);
	});
}
