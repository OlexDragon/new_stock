import {UnitMeasurement} from './unit-measurement.mjs';
import {outputTool} from '../../calibration.mjs';

class PMUnitMeasurement extends UnitMeasurement {

	#toUpdate = [];
	// store bound handler so we can unsubscribe later
	#boundUpdate = null;
	#propertyName;
	#value;

	constructor() {
		super();
		this.#boundUpdate = this.#updateValue.bind(this);
		outputTool.subscribe(this.#boundUpdate);
	}
	get isReady() {
		return outputTool.isReady;
	}
	get(callback){
//		console.log('PMUnitMeasurement get');
		if (typeof callback !== 'function')
			throw new Error('Callback must be a function');
		this.#toUpdate.push(callback);
		outputTool.get();
	}
	abort(reason = 'Aborted'){
		try{
			if(typeof outputTool.abort === 'function') 
				outputTool.abort();
		}catch(e){}
		super.abort(reason);
	}
	#updateValue(propertyName, value) {
//		console.log('PMUnitMeasurement #updateValue', propertyName, value);
		this.#propertyName = propertyName;
		this.#value = value;
		super.get(this.#callback.bind(this));
	}
	#callback(unitInfo){
		if(unitInfo && this.#propertyName === 'outputToolValue')
			unitInfo.power = this.#value;
//		console.log('PMUnitMeasurement #callback', unitInfo);
		try{
			this.#toUpdate.forEach(callback => {
				try{ callback(unitInfo); }catch(e){ console.error('pm-unit-measurement callback error', e); }
			});
		}finally{
			this.#toUpdate = [];
		}
	}
	destroy() {
		try{ if(this.#boundUpdate) outputTool.unsubscribe(this.#boundUpdate); }catch(e){}
		super.destroy();
		this.#toUpdate = null;
		this.#boundUpdate = null;
	}
}

const um = new PMUnitMeasurement();
export default um;