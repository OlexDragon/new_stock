import {unitSerialNumber} from '../../calibration.mjs';
import { allModules, getDeviceDebugRead } from '../../calibration.mjs';

if(!unitSerialNumber){
	alert('No serial number.');
}else{

	const $stickers = $('#stickers');

	setTimeout(()=>{
		Object.values(allModules).sort((a,b)=>a-b).forEach((moduleIndex, i)=>{
			setTimeout(getInfo, i*150, moduleIndex);
		});
	}, 200);
	function getInfo(devid){
		allModules[devid] = {};

		$.get('/calibration/rest/module-info', { sn: unitSerialNumber, moduleIndex: devid })
		.done(data=>{
			if(!data)
				console.warn('Failed to retrieve unit information.', {sn: unitSerialNumber, devid: devid});
			else
				allModules[devid].info = dataToObject(data);
			addRow(devid, allModules[devid].info);
		})
		.fail(connectionFail);

		setTimeout(getUnitHelp, 50, unitSerialNumber, devid);
	}
	function connectionFail(error){
		console.error('Connection failed.', error.statusText);
	}
	function dataToObject(data){
		if(!data){
			console.warn('No data received.');
			 return '';
		}
		if(data.startsWith('<!DOCTYPE html>')){
			console.warn('Received HTML instead of data.', data, allModules);
			return '';
		}
		return JSON.parse(`{"${data.split('\n').map(line=>line.trim()).filter(line=>line!=='').join('","').replace(/: /g, '":"')}"}`)
	}
	function getUnitHelp(sn, devid){
		getDeviceDebugRead(sn, devid, 'hwinfo', 100).done(data=>{
			if(!data || data.startsWith('<!DOCTYPE html>') || data.startsWith('Invalid')){
				console.warn('Failed to retrieve unit help information.', {sn: sn, devid: devid});
				return;
			}
			allModules[devid].help = data;
			showSticket(sn, devid, data);
		});
	}
	function addRow(devid, info){
		const text = JSON.stringify(info).replace(/,/g, '\n').replace(/"/g, ' ').replace(/{|}/g, '');	// Modul Info
		if(!info || !info['Serial number']){
			$stickers.append($('<div>', {class: 'row', text: `Module ${devid}: No information available.`, title: text}));
			return;
		}
		setTimeout(()=>{
			$stickers.append($(
					'<div>', {class: 'row'})
					.append($('<div>', {class: 'col', text: info['Serial number'], title: text}))
					.append($('<div>', {class: 'col-auto', text: devid, title: text}))
					.append($('<div>', {id: `sticker${devid}`, class: 'col text-end'})));}, 5);
	}
	function parseETC(help){
		const lines = help.split('\n');
		let etc;
		for(let line of lines)
			if(line.endsWith('ETC')){
				etc = +line.replace(/\D/g,'');
				break;
			}
		return etc;
	}
	function showSticket(unitSN, devid, help){
		const etc = parseETC(help);
		getDeviceDebugRead(unitSN, devid, 'regs', etc).done(data=>{
			if(!data){
                console.warn('Failed to retrieve sticker data.', {sn: unitSN, devid: devid, help: help});
                return;
            }
			const regs = dataToObject(data);
			const sticker = removeLeadingZeros(regs['USER_MEMORY_00 (0x20)']) + removeLeadingZeros(regs['USER_MEMORY_01 (0x21)']) + removeLeadingZeros(regs['USER_MEMORY_02 (0x22)']) + removeLeadingZeros(regs['USER_MEMORY_03 (0x23)']) + removeLeadingZeros(regs['USER_MEMORY_04 (0x24)']) + removeLeadingZeros(regs['USER_MEMORY_05 (0x25)']);
			$stickers.find(`#sticker${devid}`).text(sticker.replace(/^0+/, ''));
		});
	}
	function removeLeadingZeros(str){
		return str.substring(str.length-2, str.length);
	}
}