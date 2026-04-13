import {get1CTravelerSection} from '../one-c/one-c-traveler.mjs';
import {serialNumber} from '../one-c/one-c.mjs';

if(serialNumber){
		const $tunedBy = $('#tunedBy');
		const $cardBody = $tunedBy.parent();

		$cardBody.empty().append($tunedBy);

		// Read IMD values from 1C Profile Sections
		const sections = ['unit-tuning-imd-3', 'unit-tuning-imd-5', 'unit-tuning-imd-10'];
		for(let i=0; i<sections.length; i++)
			setTimeout(readImdFrom1C, i*200, i);

		function readImdFrom1C(index){
			const section = sections[index];
			get1CTravelerSection(section).then(data => {
				if(!data || !data.startsWith('[')){
					console.warn(`1C Profile Section ${section} is empty or invalid:`, data);
					return;
				}
				const json = JSON.parse(data);
				const split = section.split('-');
				const imdValue = json[0].IMD;
				if(json[0].TunedBy)
					$tunedBy.text(`Tuned by ${json[0].TunedBy}`);
				$cardBody.append($('<div>', {class: `row ${imdValue ? "" : "visually-hidden"}`}).append($('<div>', {class: 'col fw-bold', text: `IMD-${split[split.length-1]}:`})).append($('<div>', {class: 'col-auto', text: imdValue})));
				setTimeout(addNote, sections.length*200, json[0].Notes);
			}).catch(errorHandler);	

	}
	function addNote(notes){
		if(!notes) return;
		$cardBody.append($('<div>', {class: 'row mt-3'}).append($('<div>', {class: 'col pre-line', text: notes})));
	}
	function errorHandler(error){
		console.error('Error fetching 1C Profile Section converter:', error);
	}
}