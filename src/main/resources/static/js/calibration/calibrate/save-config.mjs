import {show as showToast} from '../../toast-worker.mjs';
import {ajaxJSON, connectionFail} from '../fetch/fetch.mjs';
import {unitSerialNumber} from '../../calibration.mjs';
import DestroyableComponent from '../destroyable-component.mjs';

export default class SaveConfig extends DestroyableComponent{

	#$elements;
	#values;
	#url;

	constructor($config, tableName){
		super(tableName);
		// Track the root element so its events get cleaned up, then find all input and select elements with a data-db-name attribute inside the dropdown menu, and store their initial values.

		this.#$elements = $config.on('hidden.bs.dropdown' + this._ns, this.#save.bind(this)).parent()
								.find('.dropdown-menu')
								.find('input, select')
								.filter((_,el)=>el.dataset.dbName);
		this.#values = this.#$elements
								.map((_,el)=>el.type === 'checkbox' ? el.checked : el.value)
								.get();
		this.#url = `calibration/rest/${tableName}`
	}

	destroy() {
	    super.destroy();
	    this.#$elements = this.#values = this.#url = null;
	}

	#save(){
		if(!this.#values.some((v,i)=>{
								const el = this.#$elements[i];
								const current = el.type === 'checkbox' ? el.checked : el.value;
								return v!==current;
							}))	
			return;
		$.get('/users/principal')
		.done(principal=>{
			if(!principal.authenticated || !principal.authorities.includes('CALIBRATION_SETTINGS')){
				showToast('No authorities', 'To save changes, you need access rights to the calibration settings.', 'text-bg-warning');
				return;
			}
			if(confirm('Do you want to save your changes to the database?')){

				const object = Object.fromEntries(
				    Array.from(this.#$elements, el => [
				        el.dataset.dbName,
				        el.type === 'checkbox' ? el.checked : el.value
				    ])
				);
				object.partNumber = unitSerialNumber;

				ajaxJSON(this.#url, object)
				.done(function(data){
					if(data.message){
						console.log('Save successful:', data);
						showToast('Save Setting', data.message, 'text-bg-success');
					}else
						console.warn('Save:', data);
				})
				.fail(function(error) {
					if(error.statusText!='abort'){
						$modal.modal('hide');
						connectionFail(error);
					}
				});
			}
		});
	}
}