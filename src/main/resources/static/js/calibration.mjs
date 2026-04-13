import OutputTool from './calibration/tools/output-tool.mjs';
import InputTool from './calibration/tools/input-tool.mjs'
import { handleLoginIfNeeded } from './calibration/global-login-handler.mjs';

export let unitSerialNumber;
export const outputTool = new OutputTool($('#collapseOutput'));
export const inputTool = new InputTool($('#collapseInput'));
export const $menuPowerOffset = $('#menuPowerOffset');


$(document).ajaxComplete((event, xhr) => {
    handleLoginIfNeeded(xhr.responseText);
});

if(serialNumber){
	unitSerialNumber = /^[0-9]$/.test(serialNumber.charAt(0)) ? 'irt-' + serialNumber : serialNumber;
	import('./calibration/cards/onrender-card.mjs');
	import('./calibration/cards/tuning-card.mjs');
	import('./calibration/cards/header-card.mjs');
}
if(info){
	const serialNumberDiv = document.getElementById('serialNumber');
	const resizeObserver = new ResizeObserver(()=>{

		if(!serialNumberDiv.innerText)
			return;

		getAllModules();
	});
	resizeObserver.observe(serialNumberDiv);
}

export let f_singIn;
export let allModules;
export function getDeviceDebugRead(sn, devid, command, groupindex){
	return $.get('/calibration/rest/device_debug_read', {sn: sn, devid: devid, command: command, groupindex: groupindex});
}

function getAllModules(singInCallback){
	if(typeof singInCallback === 'string'){
		alert('Sign-in failed.\nThis unit cannot be calibrated.');
		return;
	}
	if(!info){
		console.log('the variable info = ', info);
		return;
	}
	const sn = info["Serial number"];
	$.get('/calibration/rest/all-modules', {sn: sn})
	 .done((data)=>{
		if(typeof data === 'string'){
			if(data.includes('Please sign in')){
				if(f_singIn){
					f_singIn(sn, getAllModules);
				} else {
					import('./calibration/sign-in.mjs').then((module)=>{
						f_singIn = module.signIn;
						f_singIn(sn, getAllModules);
					});
					return;
				}
			}
			return;
		}
		allModules = data;
		import('./calibration/cards/stickers-card.mjs');
		import('./calibration/cards/unit-gates-card.mjs');
	});
}

export const $modal = $('#modal');
//let worker;
//$('#scan').click(e=>{
//	e.preventDefault();
//	if(!worker || worker.constructor.name !== 'IPScanner'){
//		import('./calibration/modales/ip-scanner.mjs').then(({default:Scanner})=>{
//			worker = new Scanner($modal);
//			worker.scan();
//		});
//	}else
//		worker.scan();
//});
export function resetModalController(){
    modalController = null;
}
let modalController;
$('nav.navbar.fixed-top .cal-menu').click(calMenuClick);
async function calMenuClick(e){
	e.preventDefault();
	const {currentTarget:{dataset:{className, path}}} = e;
	if(modalController?.constructor.name !== className){
		if(modalController)
			modalController.destroy();
		const {default:ModalController} = await import(path);
		modalController = new ModalController();
	}
	modalController.show();
}
const $saveToCookies = $('.save-to-cookies').change(({currentTarget:el})=>{
	if(!el.value)
		return;
	Cookies.set(el.id, $(el).find('option:selected').text(), { expires: 99, path: '/calibration' });
});
$.each($saveToCookies, (_, tool)=>{
	const cookie = Cookies.get(tool.id)
	if(!cookie)
		return;
	const option = Array.from(tool.children).find(({value, innerText})=>{
		if(!value)
			return false;
		 return innerText === cookie;
	 });
	 if(option)
		 option.selected = true;
});
