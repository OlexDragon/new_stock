import {$modal} from '../calibration.mjs';
import DestroyableComponent from './destroyable-component.mjs';

export default class ModalLoader extends DestroyableComponent{

	constructor(path){
		super();
		this._$modal = $modal;
		if(path){
			$modal.load(path, this._onLoad.bind(this));
		}
	}
	show(){
		$modal.modal('show');
	}
	hide(){
		$modal.modal('hide');
	}
	_onLoad(){
		console.warn('ModalLoader._onLoad not implemented');
	}
}