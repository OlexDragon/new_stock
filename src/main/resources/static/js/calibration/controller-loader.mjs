import {$modal} from '../calibration.mjs';
import ModalLoader from './modal-loader.mjs';

export default class ControllerlLoader extends ModalLoader{

	#contrillerClass;

	constructor(path, prefix){
		super();
		if(path){
			$modal.load(path, this._onLoad.bind(this)).off();
			this.#contrillerClass = serialNumber ? import(`./calibrate/${prefix}-buc.mjs`) : import(`./calibrate/${prefix}-fcm.mjs`);
		}
	}
	show(){
		$modal.modal('show');
	}
	hide(){
		$modal.modal('hide');
	}
	_onLoad(){
		this.#contrillerClass.then(({default:Controller})=>{
			const controller = new Controller($modal);

			if(controller.onHide)
				this.on($modal, 'hide.bs.modal', controller.onHide.bind(controller));
			else
				console.warn('A onHide method needs to be created.', controller);

			if(controller.onShow)
				this.on($modal, 'shown.bs.modal', controller.onShow.bind(controller));
			else
				console.warn('A onShow method needs to be created.', controller);
		});
	}
	destroy() {
		super.destroy();
		if(this.#contrillerClass?.destroy)
			this.#contrillerClass.destroy();
		this.#contrillerClass = null;
	}
}