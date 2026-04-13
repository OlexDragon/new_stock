import Tool from './tool.mjs'
//import {eventBus} from '../../calibration.mjs'

export default class InputTool extends Tool{

	#$toastContainer;
	#selectedTool;
	#$inputs;
	constructor($parents){
		super($parents, true);
		this._timeout = 3000;
		super._init = super._init.bind(this);
		this._init();
		this.#$toastContainer = $('#toast-container');
		this.#$inputs = this.on($parents.find('.input-value'),'input', this.#onInput);
		this.on(this.#$inputs, 'change', ({currentTarget:el})=>el.closest('.input-group').querySelectorAll('button')[0].click());
		this.on(this.#$inputs, 'mouseenter', ({ctrlKey, currentTarget:el, currentTarget:{localName}})=>{

			if(!ctrlKey || localName != 'input' || this.#$toastContainer.html().trim())
				return;

			showSetupToast(this.#$toastContainer, el);
		});
		this.on(this.#$inputs, 'focusin', _=>{
			const $toast = $('.toast');
			if($toast.length)
				$toast.remove();
		})
		.each((_,el)=>{

			if(el.localName != 'input')
				return;

			let step = Cookies.get(el.id + "Step");
			if(step)
				el.dataset.step = step;
		});
		this.on(this.#$inputs, 'keydown', e=>{

			const el = e.currentTarget;

			if(e.originalEvent.code === 'Space'){
				e.preventDefault();
				if(e.ctrlKey){
					let another = getAnother(el);
					if(another)
						another.focus();
				}
				return;
			}

			if(el.localName != 'input')
				return;

			let step;
			if(el.dataset.step === 'undefined')
				step = 1;
			else
				step = el.dataset.step ? parseFloat(el.dataset.step) : 1;

			switch(e.originalEvent.code){

			case 'ArrowRight':

				if(!e.ctrlKey)
					return;

				if(showStepValue(this.#$toastContainer, e))
					break;

				if(step<0.1)
					return;

				el.dataset.step = step / 10;
				showStepValue(this.#$toastContainer, e);
				break;

				case 'ArrowLeft':

					if(!e.ctrlKey)
						return;

					if(showStepValue(this.#$toastContainer, e))
						break;

					if(step>10)
						return;

					el.dataset.step = step * 10;
					showStepValue(this.#$toastContainer, e);
					break;

				case 'NumpadAdd':
				case 'Equal':

					if(!(e.ctrlKey || e.shiftKey))
						brack;

					if(showStepValue(this.#$toastContainer, e))
						break;

					if(e.ctrlKey)
						el.dataset.step = step * 2;

					else
						el.dataset.step = newStep(step, true);
						

					showStepValue(this.#$toastContainer, e);
				break;

				case 'NumpadSubtract':
				case 'Minus':

					if(!(e.ctrlKey || e.shiftKey))
						brack;

					if(showStepValue(this.#$toastContainer, e))
						break;

					if(e.ctrlKey)
						el.dataset.step = (step / 2).toFixed(2);

					else
						el.dataset.step = newStep(step).toFixed(2);

					if(el.dataset.step < 0.01)
						el.dataset.step = 0.1;

					showStepValue(this.#$toastContainer, e);
				break;

			case 'Backspace':

				if(!e.ctrlKey)
					return;

				if(showStepValue(this.#$toastContainer, e))
					break;

				if(el.dataset.step.length > 1)
					el.dataset.step = parseFloat(el.dataset.step.slice(0,-1));

				if(el.dataset.step < 0.01)
					el.dataset.step = 0.1;

				showStepValue(this.#$toastContainer, e);
				break;

			case 'Numpad0':
			case 'Numpad1':
			case 'Numpad2':
			case 'Numpad3':
			case 'Numpad4':
			case 'Numpad5':
			case 'Numpad6':
			case 'Numpad7':
			case 'Numpad8':
			case 'Numpad9':
			case 'Digit0':
			case 'Digit1':
			case 'Digit2':
			case 'Digit3':
			case 'Digit4':
			case 'Digit5':
			case 'Digit6':
			case 'Digit7':
			case 'Digit8':
			case 'Digit9':

				if(!e.ctrlKey)
					return;

				if(showStepValue(this.#$toastContainer, e))
					break;

				el.dataset.step = el.dataset.step + e.originalEvent.key;

				showStepValue(this.#$toastContainer, e);
				break;

			case 'ArrowUp':
				if(el.value=='')
					break;

				if(e.shiftKey && e.ctrlKey){
					e.preventDefault();
					changeAnother(el, 'ArrowUp');
					return;
				}

				doStep(el, 'ArrowUp');
				break;

			case 'ArrowDown':
				if(el.value=='')
					break;

				if(e.shiftKey && e.ctrlKey){
					e.preventDefault();
					changeAnother(el, 'ArrowDown');
					return;
				}

				doStep(el, 'ArrowDown');
				break;

			case 'Enter':
			case 'NumpadEnter':
				el.closest('.input-group').querySelectorAll('button')[0].click()
				break;

			default:
				return;
			}

			e.preventDefault();
		});
	}
	get(name){
		this.set(name, '');
	}
	set(name, value){
		let id = toInputId(name);
//		console.warn(value ? 'set' : 'get', `${name} => ${id}:`, value);
		this.#$inputs.filter(id).val(value).change();
	}
	value(name){
		let id = toInputId(name);
//		console.warn(`value(${name})`, 'id:', id);
		return this.#$inputs.filter(id).val();
	}
	get powerUnit(){
		if(!this.#toolIsSelected())
			return;
		const {dataset:{powerUnit}} = this.#selectedTool;
		return powerUnit;
	}
	get freqFactor(){
		if(!this.#toolIsSelected())
			return;
		const {dataset:{freqFactor}} = this.#selectedTool;
		return freqFactor;
	}
	toolComand(index){
		const toolCommand = this.command;
		return toolCommand.split(' ')[index];
	}
	#toolIsSelected(){
		if(!this.#selectedTool){
			const {selectedIndex} = this._$tool[0];
			if(selectedIndex>=0)
				this.#selectedTool = this._$tool[0].options[selectedIndex];
			else
				 return false;
		}
		return true;
	}
	_toolCommand(toolCommand, value, index, option) {
	    // Extract the command token
	    let cmd = toolCommand.split(' ')[index];
	    const hasValue = value != null && value !== '';

	    if (hasValue) {
	        // Build value string
	        let val = ' ' + value;

	        if (option?.innerText) {
	            let unit = option.innerText;
	            if (this.toolName === 'ANRITSU68047C')
	                unit = unit.replace('z', '');
	            val += ' ' + unit;
	        }

	        return {
	            command: cmd + val,
	            getAnswer: false
	        };
	    }

	    // No value → read command
	    return {
	        command: this.readFormate.replace('{V}', cmd),
	        getAnswer: true
	    };
	}
	_toValue(answer, input, option){
		switch(input.localName){

		case 'select':
			return answer;

		default:
			let factor = input.id==='inputFrequency' ? this.freqFactor : 1;
			const divider = +(option?.dataset.divider ?? 1);
			const value = parseFloat(answer);
			return value/(divider/factor);
		}
	}
	_showToolSettings(e){
		super._showToolSettings(e);
		if(!e)
			return;

		const {currentTarget:el, currentTarget:{selectedIndex}} = e;
		if(selectedIndex>=0)
			this.#selectedTool = el.options[selectedIndex];
	}
	#onInput({currentTarget:el}){
		const text = el.value ? 'Set' : 'Get';
		const btn = el.closest('.input-group').querySelectorAll('button')[0];
		if(btn.innerText!=text)
			btn.innerText = text;
	}
}
function showSetupToast($toastContainer, target){

	let $input = $('<input>', {type: 'number', class: 'form-control', style: 'text-align:center;', value: target.dataset.step !== 'undefined' ? target.dataset.step : 1})
	.on('focusout', e=>{

		let step = e.currentTarget.value;
		if(!step)
			return;

		target.dataset.step = step;
	});

	let $toast = $('<div>', {class: 'toast', role: 'alert', 'aria-live': 'assertive', 'aria-atomic': true, 'data-bs-delay': 5*60*1000})
		.append(
			$('<div>', {class: 'toast-header'})
			.append(
				$('<strong>', {class: 'me-auto', text: target.title + ' Step'})
			)
			.append(
				$('<button>', {class: 'btn-close', type: 'button', 'data-bs-dismiss': 'toast', 'aria-label': 'Close'})
			)
		)
		.append(
			$('<div>', {class: 'toast-body'})
			.append($input)
		)
	.appendTo($toastContainer)
	.on('hide.bs.toast', function(){
		Cookies.set(target.id + "Step", target.dataset.step, { expires: 9999, path: '' });
		this.remove();
	});

	new bootstrap.Toast($toast).show();
}
function showStepValue($toastContainer, e){

	let $toast = $('.toast');
	if($toast.length){
		$toast.find('input').val(e.currentTarget.dataset.step);
		return false;	// Been open
	}

	showSetupToast($toastContainer, e.currentTarget);
	return true; 	// Open
}
function doStep(el, arrow){
	switch(arrow){

		case 'ArrowUp':
			el.value = (parseFloat(el.value) + parseFloat(el.dataset.step)).toFixed(2);
			$(`#${el.id}Btn`).click()
			break;

		case 'ArrowDown':
			el.value = (parseFloat(el.value) - parseFloat(el.dataset.step)).toFixed(2);
			$(`#${el.id}Btn`).click()
	}
}
function getAnother(el){
	return el.closest('.row').querySelectorAll(`input[id=${el.id == 'inputPower' ? 'inputFrequency' : 'inputPower'}]`)[0];
}
function newStep(step, add){

	const sing = add ? 1 : -1;
	let diminutive;

	if(compareStep(step, 100, add))
		diminutive = 100 * sing;

	else if(compareStep(step, 10, add))
		diminutive = 10 * sing;

	else if(compareStep(step, 1, add))
		diminutive = 1 * sing;

	else if(compareStep(step, 0.1, add))
		diminutive = 0.1 * sing;

	else
		diminutive = 0.01 * sing;

	return step + diminutive;
}
function compareStep(step, toCompare, add){

	if(add)
		return step >= toCompare;
	else
		return step > toCompare
}
function changeAnother(el, arrow){

	let another = getAnother(el);
	if(!another)
		return;

	switch(arrow){

		case 'ArrowUp':
			doStep(another[0], 'ArrowUp');
			break;

		case 'ArrowDown':
			doStep(another[0], 'ArrowDown');
	}	
}
function toInputId(name){

	if(!name)
		return '#inputPower';

	switch(name.toUpperCase()){

	case 'INPUTPOWER':
	case 'POWER':
		return '#inputPower';

	case 'INPUTFREQUENCY':
	case 'FREQUENCY':
	case 'FRE':
		return '#inputFrequency';

	case 'OUTPUT':
	case 'INPUTOUTPUT':
        return '#inputOutput';
	}
}