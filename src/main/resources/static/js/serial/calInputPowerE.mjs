import {setCookies, getInfo, ipRun, startButtonText, readSetToolValue, chartReset, BTN_START, BTN_STOP} from './calInputPowerF.mjs';
import {clear} from './calInputPower.mjs';

const $ipInfo	 = $('#ipInfo');
const $ipStart	 = $('#ipStart');
const $ipTool	 = $('#ipTool');
const $ipStep	 = $('#ipStep');
const $ipMax	 = $('#ipMax');
const $ipComPorts= $('#ipComPorts');

let ipIntervalID;
const commandInputPower = 2;
function ipStop(){
		clearInterval(ipIntervalID);
		$ipTool.prop('readonly', false);
		$ipStep.prop('readonly', false);
		$ipInfo.removeClass('disabled');
		startButtonText(BTN_START);
		ipIntervalID = null;
}
function reset(){
	if(oldToolValue)
		$ipTool.val(oldToolValue);
	else
		$ipTool.val(-60);
	readSetToolValue();
}
$ipInfo.click(getInfo);
let oldToolValue;
$ipStart.click(()=>{

	// Stop Button
	if($ipStart.text()=='Stop'){
		ipStop();
		$ipInfo.removeClass('disabled');
		return;
	}
	if($ipStart.text()=='Reset'){
		chartReset();
		return;
	}
	if($ipStart.text()=='Restart'){
		clear();
		$ipTool.val(oldToolValue);
	}

	$ipInfo.addClass('disabled');
	$ipTool.prop('readonly', true);
	$ipStep.prop('readonly', true);

	// Check Serial Port, Tool Addreass and Input Tool Type
	const toSend = getToSendIT(commandInputPower);
	if(!checkInputTool(toSend)){
		$modal.modal('hide');
		$collapseInput.show();
		return;
	}

	readSetToolValue();
	oldToolValue = $ipTool.val();
	startButtonText(BTN_STOP);

	ipIntervalID = ipRun();
});
$ipComPorts.change(e=>{
	if(e.currentTarget.value){
		$ipInfo.removeClass('disabled');
		getInfo();
		setCookies(e.currentTarget);
	}else
		$ipInfo.addClass('disabled');
});
$ipStep.on('focusout', e=>setCookies(e.currentTarget));
$ipMax.on('focusout', e=>setCookies(e.currentTarget));
$modal.on('hidden.bs.modal', function () {
	clearInterval(interval);
	interval = undefined;
	err = null;
});
$ipTool.keypress(e=>{
	if(e.which == 13){  // the enter key code
		readSetToolValue();
		setTimeout(()=>$ipTool.val($inputPower.val()), 300);
	}
})
.change(()=>$ipStart.text('Start'));
$modal.on('hidden.bs.modal', ipStop);
export {$ipStart, $ipComPorts, $ipTool, $ipStep, $ipMax, $ipInfo, reset, ipStop}