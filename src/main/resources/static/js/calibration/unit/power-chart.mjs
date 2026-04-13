import homePageInfo from '../fetch/home-page-info.mjs'
import ModalLoader from '../modal-loader.mjs';
import IrtChart from '../chart.mjs';
import unitMeasurement from '../fetch/unit-measurement.mjs';
//import calInfo from '../fetch/calibration-info.mjs';
import getAlarms from '../fetch/alarms-info.mjs';
import {resetModalController} from '../../calibration.mjs';

export default class PowerChart extends ModalLoader{

	#ip;
	#valueController = unitMeasurement;
	#valueName = 'data.outpower';
	#chart;
	#unit = 'dBm';
	#busy = false;
	#busyCount = 0;
	#$lastPoint;
	#$tolerance;
	#$btnByPower;
	#formater = new Intl.DateTimeFormat('en-US', { hour: 'numeric', minute: '2-digit', second: '2-digit', 	hour12: false });
	#worker = null; // handle returned by startPoll (has .stop())
	#workerIntervalMs = 3000; // polling interval in ms
	#$alarmStatus;
	#$$modaBody;

	constructor(){
		super(`/calibration/modal/power_chart/modal`);
	}

	_onLoad(){
		this.#$$modaBody = this._$modal.find('.modal-body');
		this.#$tolerance = $('#sensitivity');
		this.#$lastPoint = $('#lastPoint');
		this.on(this._$modal.find('#restart'), 'click', this.#restart.bind(this));
		const $btns = this.on(this._$modal.find("input[name=testBy]"), 'change', this.#controllerChange.bind(this));
		this.on(this._$modal, 'shown.bs.modal', () => this.#update());
		this.on(this._$modal, 'hidden.bs.modal', () => this.#stopWorker());
		homePageInfo(2000).then(info=>{
			if(!info){
				console.error('homePageInfo returns NULL.');
				alert('Something went wrong. Please try again later.');
				resetModalController(); // prevent opening this modal again
				this.hide();
				return;
			}
			this.#$btnByPower = $btns.filter('#byPower');
			this._$modal.find('.modal-title.text-primary').text(info.sysInfo.sn);
//			console.log('home page info = ', info);
			this.#ip = info.netInfo.addr;

			// Initialize chart with two datasets: Power and Temperature
			this.#chart = new IrtChart(this._$modal.find('#powerChart'), ['Power(dBm)', 'Temperature(°C)']);
		});
//		calInfo.get(unitInfo=>{
//					console.log('calibration info = ', unitInfo);
//				});
	}

	#update(){
		this.#busy = false;

		// Ensure no previous worker is running
		this.#stopWorker();

		// Start an auto-tracked interval via DestroyableComponent.setInterval
		// this.setInterval wraps timer-worker's startPoll and falls back to native setInterval
		this.#worker = this.setInterval(() => { if (this.#chart) this.#measure(); }, this.#workerIntervalMs);
	}

	#measure(){
		// Avoid overlapping fetches
		if (this.#busy){
			this.#busyCount++;
			if(this.#busyCount > 5){
				console.warn('Fetch has been busy for ' + this.#busyCount + ' consecutive ticks. Resetting busy state to avoid lockup.');
				this.#busyCount = 0;
				this.#busy = false;
				this.#valueController.abort(); // attempt to abort any in-flight fetch if supported by the controller
			}
			console.warn('Previous fetch is still in progress. Skipping this tick.');
			 return;
		}
		this.#busy = true;
		// Delegate to the value controller which will call back appendPoint
		// appendPoint will clear #busy in its finally block
		this.#valueController.get(this.#appendPoint.bind(this));
		// Also fetch alarms info on each tick
		this.#checkAlarms();
	}
	
	#stopWorker(){
		if(!this.#worker) return;
		try{
			// Delegate cleanup to DestroyableComponent helper which knows how to stop handles
			this.clearInterval(this.#worker);
		}catch(e){ /* ignore */ }
		this.#worker = null;
	}

	async #appendPoint(unitInfo){
		try{
			if(!unitInfo){
				console.warn('POST ' + this.#valueController.url + ' returns NULL.');
				const $children = this.#$alarmStatus.children();
				if($children.length==1 || !$($children.get(1)).text().endsWith('Communication lost.'))
					$children.first().after($('<div>', {class: 'alert alert-secondary', text: this.#formater.format(new Date()) + ' -> Communication lost.'}));
				return;
			}
			
			const date = this.#formater.format(new Date());
			// Temperature
			const temperature = await this.#valueController.valueOf(['data', 'temperature']);
			const valueOf = await this.#valueController.valueOf(this.#valueName);
//			console.log('valueOf = ', valueOf, 'this.#valueName = ', this.#valueName);
			if(!valueOf){
				console.warn('Value is not found by path: ' + this.#valueName, '; unitInfo = ', unitInfo);
				return;
			}
			let value = Number(valueOf.replace(/[<>]/g, ''));
			if(isNaN(value)){
				console.warn('Value is not a number: ' + valueOf);
				value = null;
			}
			const { x, y } = this.#chart.data;

			this.#$lastPoint.text(` ( ${date} - Value: ${value !== null ? value.toFixed(1) + ' ' + this.#unit : 'N/A'}; Temperature: ${temperature !== null ? temperature.toFixed(1) + ' °C' : 'N/A'} )`);
			if (x.length <= 1)
			    return this.#chart.appendPoint(date, [value, temperature]);

			const lastIndex = x.length - 1;
			const lastValue = y[0].at(-1);
			const lastTemp  = y[1].at(-1);
			const tolerance = +this.#$tolerance.val();

			const shouldReplace =
			    value !== lastValue
			        ? Math.abs(value - lastValue) <= tolerance
			        : Math.abs(temperature - lastTemp) <= 5;

			shouldReplace
			    ? this.#chart.replacePoint(lastIndex, date, [value, temperature])
			    : this.#chart.appendPoint(date, [value, temperature]);

		}finally{
			this.#busy = false;
		}
	}
	#restart(){
		this.#stopWorker();
		this.#valueController.abort(); // abort any in-flight fetch if supported by the controller

		// Clear chart and reinitialize with 3 empty points
		this.#chart.clear();
		this.#chart.update();
		this.#update();
		this.#$alarmStatus?.remove();
		this.#$alarmStatus = null;
	}

	async #controllerChange({currentTarget:{id, dataset:{unit, property}}}){
		switch(id){
			case 'byPM':
				const {default:pmUnitMeasurement} = await import('../fetch/pm-unit-measurement.mjs');
				if(pmUnitMeasurement.isReady){
					this.#valueController?.destroy();
					// Use PM unit measurement as the data source but do not subscribe here
					this.#valueController = pmUnitMeasurement;
				} else{
					setTimeout(()=>this.#$btnByPower.prop('checked', true), 10);
					return;
				}
				break;
			default:
				this.#valueController?.destroy();
				// use the pre-imported calibration info instance
				this.#valueController = unitMeasurement;
		}
		$('label[for="sensitivity"]').text(`Sensitivity (+/-${unit})`);
		this.#valueName = property;
		this.#restart();
	}

	async #checkAlarms(){
		const alarms = await getAlarms();

		if(!this.#$alarmStatus?.length){
			this.#$alarmStatus = $('<div>', { id: 'alarmStatus', class: 'alert alert-warning', role: 'alert'}).append($('<h4>', {class: 'alert-heading', text: 'Alarms'}));
			this.#$$modaBody.append(this.#$alarmStatus);
		}

		const $children = this.#$alarmStatus.children();
		//No Alarms
		if(alarms.summary=='cleared' || alarms.summary=='no alarm'){
			let text = 'summary - No Alarms';

			if($children.length>1 && $($children.get(1)).text().endsWith(text))
				return;

			$children.first().after($('<div>', {class: 'alert alert-success', text: this.#formater.format(new Date()) + ' -> ' + text}));
			return;
		}

		let text = alarms.alarms.filter(a=>a.status!='cleared').filter(a=>a.status!='no alarm').map(a=>a.desc + ' - ' + a.status).toString();
		if($children.length>1){
			let lastText = $($children.get(1)).text();
			let split = lastText.split(' -> ');

			if(split.length>1 && split[1]==text)
				return;
		}
		$children.first().after($('<div>', {class: 'alert alert-danger', text: this.#formater.format(new Date()) + ' -> ' + text}));
	}

	destroy(){
		// Stop worker if running
		this.#stopWorker();
		// (no subscription to PMUnitMeasurement created here)

		super.destroy();

		// Destroy Chart.js instance
		if(this.#chart){
			this.#chart.destroy();
			this.#chart = null;
		}

		// Clear jQuery references
		this.#$lastPoint = null;

		// Clear properties
		this.#ip = null;
		if(this.#valueController && typeof this.#valueController.destroy === 'function')
			this.#valueController.destroy();
		this.#valueController = null;
		this.#busy = false;

		console.log('PowerChart instance destroyed');
	}
}