import {get1CTravelerSection, post1CTravelerSection} from '../one-c/one-c-traveler.mjs';
import calibRwInfo from '../fetch/calib-rw-info.mjs';

setTimeout(showGates, 1000);

async function showGates(callBackValue){
	const info = await calibRwInfo.getValueAsync();
	if(info.error){
		console.error(info);
		return;
	}

	const keys = Object.keys(info);
	if(!keys.length){
		console.warn('No Calib RW Info:');
		return;
	}

	const carriers = getGates(info);
	if(!carriers.length)
		return;

	const fcmIndex = carriers.findIndex(c=>c.name === 'FCM');
	if(fcmIndex>-1){
		const fcm = carriers.splice(fcmIndex, 1)[0];
		if(fcm.vars)
			setTimeout(()=>showCard('FCM', fcm.vars), 1000);
	}

	const imIndex = carriers.findIndex(c=>c.name === 'IM');
	if(imIndex>-1){
		const im = carriers.splice(imIndex, 1)[0];
		if(im.vars)
			setTimeout(()=>showCard('IM', im.vars), 1000);
	}
	// If all gaits are the same, there is no need to compare with 1C data, just show cards.
	if(notReady(carriers)){
		showCards(carriers, 'text-bg-warning ', 'All gaits are the same');
		return;
	}

	const devices1C = await get1CTravelerSection('devices');

	if(!devices1C || !devices1C.startsWith('[')){
		console.warn('1C Profile Section "Devices" is empty or invalid:', devices1C);
		showCards(carriers, 'text-bg-warning ', '1C Profile Section "Devices" is empty or invalid:');
		return;
	}
	const carriers1C = gatesFrom1C(devices1C);
//	console.log(carriers1C);

// Save all gaits data to 1C if 1C Profile Section devices is empty, otherwise compare and update only different values.
//	if(carriers1C.length){
//		const text = toText(carriers);
//		const gate1C = carriers1C[0].vars[0];
//		if(text !== gate1C.auxiliary){
//			gate1C.auxiliary = text;
//			saveTo1C(gate1C, carriers[0].vars[0]);
//		}
//	}
	if(carriers1C.length>carriers.length){
		if(carriers1C.length===carriers.length*2){
			const c = devideCariers(carriers);
			carriers.length = 0;
			carriers.push(...c);
		}else{
			console.warn('1C Profile Section "Devices" has more carriers than Calib RW Info gates data. 1C Carriers:', carriers1C.map(c=>c.name), 'Calib RW Info Carriers:', carriers.map(c=>c.name));
			showCards(carriers, 'text-bg-info ', '1C Profile Section "Devices" has more carriers than Calib RW Info gates data. 1C Carriers::');
			return;
		}
	}
	carriers1C.forEach((carrier1C, index)=>{
		const unitCarrier = carriers[index].vars;
		let hasUpdate = false;
		carrier1C.vars.forEach((device1C, i)=>{
			const unitGate = unitCarrier[i];
			if(!unitGate?.value)
				return;
			if(!device1C.gate){
				setTimeout(saveTo1C, 1008*i, device1C, unitGate);
				return;
			}
			if(+(device1C.dac_value.value ?? device1C.dac_value) !== unitGate.value)
				hasUpdate = true;
		});
		showCard(carriers[index].name, unitCarrier, hasUpdate ? 'text-bg-secondary' : 'text-bg-success', hasUpdate ? 'Gates are different from 1C' : undefined);
		if(hasUpdate)
			showUpdateCard(carrier1C, unitCarrier);
	});
}
function updare1CData(carrier1C, unitCarrier){
	for(let i=0; i<carrier1C.length && i<unitCarrier.length; i++){
		if(+carrier1C[i].dac_value !== unitCarrier[i].value || carrier1C[i].gate !== unitCarrier[i].name)
			saveTo1C(carrier1C[i], unitCarrier[i]);
    }
	location.reload();
}
function devideCariers(carriers){
	const array = [];
	carriers.forEach(c=>{
		const copy = structuredClone(c);
		c.vars.splice(8,8);
		copy.vars.splice(0,8);
		array.push(c);
		array.push(copy);
	});
	return array;
}
async function saveTo1C(device1C, gate){
	delete device1C.PCB;
	delete device1C.Power;
	if(!device1C.auxiliary)
		delete device1C.auxiliary;
	const oldValue = +device1C.dac_value;
	device1C.dac_value = {value: `${gate.value}`};
	if(oldValue && oldValue !== device1C.dac_value.value)
		device1C.dac_value.comment = 'Old value: ' + oldValue;
	device1C.gate = gate.name;
	post1CTravelerSection(device1C, 'devices')
	.then(response=>console.log(`Saved to 1C Profile Section devices: ${gate.name} with value ${gate.value}. Response:`, response))
	.catch(error=>console.error('Error saving to 1C Profile Section devices: ${gate.name} with value ${gate.value}', error));
}
function getGates(infoObj){
	if(!infoObj)
		return [];
	if(infoObj.gates?.dac.list)
		return infoObj.gates.dac.list;
	if(infoObj.dp.list.length)
		return infoObj.dp.list.filter(g=>g?.name);
	console.warn('No gates data found in Calib RW Info');
}
function gatesFrom1C(json){
	const map = new Map();
	JSON.parse(json).sort((a,b)=>{
		const difference = b.Power-a.Power;
		if(difference)
			return difference;
		const setting = a.Setting.localeCompare(b.Setting);
		if(setting)
			return setting;
		return a.ParentSetting.localeCompare(b.ParentSetting);
	}).forEach(device=>{
		let mapValue = map.get(device.ParentSetting);
		if(!mapValue){
            mapValue = [];
            map.set(device.ParentSetting, mapValue);
        }
		mapValue.push(device);
	});
	const carriers = [];
	map.values().forEach(devices=>carriers.push({name: devices[0].ParentSetting, vars: devices}));
	return carriers;
}
//function toText(carriers){
//	return carriers.map(c=>`${c.name} Gates:\n` + c.vars.filter(g=>g.name).map(g=>`  ${g.name}: ${g.value}`).join('\n')).join('\n\n');
//}
function showCards(carriers, bsClass, title){
	carriers.forEach(carrier=>showCard(carrier.name, carrier.vars, bsClass, title));
}
function showUpdateCard(devices1C, unitCarrier){
	return $('#unitDataCards')
		.append(
			$('<div>', {class: 'card col-xxl-2 col-lg-3 col-md-6 col-sm-6'})
			.append(
				$('<div>', {class: 'card-header bg-warning', text: '1C ' + devices1C.name}))
			.append(
				$('<div>', {class: 'card-body'})
				.append(
					devices1C.vars.map(g=>$('<div>', {class: 'row'}).append($('<div>', {class: 'col', text: g.gate + ':'})).append($('<div>', {class: 'col-auto fw-bold', text: g.dac_value.value ?? g.dac_value}))))
				.append(
					$('<div>', {class: 'row'})
					.append(
						$('<button>', {class: 'btn btn-sm btn-outline-primary mt-2', text: 'Update 1C Data'}).click(()=>updare1CData(devices1C.vars, unitCarrier))))));
}
function showCard(header, gates, bsClass, title){
	if(!gates){
		console.warn('No gates data to show for', title);
		return;
	}
	const withName = gates.filter(g=>g.name);
	if(!withName.length){
		console.warn('No gates with property "name" to show for', header, gates);
		return;
	}
	return $('#unitDataCards')
		.append(
			$('<div>', {class: 'card col-xxl-2 col-lg-3 col-md-6 col-sm-6'})
			.append(
				$('<div>', {class: `card-header ${bsClass ?? ''}`, text: header, title: title}))
			.append(
				$('<div>', {class: 'card-body'})
				.append(
					withName.map(g=>$('<div>', {class: 'row'}).append($('<div>', {class: 'col', text: g.name + ':'})).append($('<div>', {class: 'col-auto fw-bold', text: g.value}))))));
}
function notReady(carriers){
	const gaits = carriers.filter(c=>c.name!=='FCM').map(c=>c.vars).flat().filter(c=>c.name);
	if(gaits.length<2)
		return !gaits[0]?.value;
	if(gaits.filter(g=>!g.value).length>carriers.length)
		return true;
	const firstValue = gaits[0].value;
	return gaits.every(g=>g.value === firstValue);
}