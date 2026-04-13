import {get1CTravelerSection} from '../one-c/one-c-traveler.mjs';
import {serialNumber} from '../one-c/one-c.mjs';
import {serialNumber as onrenderSerialNumber, addSN} from './onrender-card.mjs';

export let partNumber;
export let localPartNumber;
export let description;

if(serialNumber){
	const $oneCHeadDescr = $('#oneCHeadDescr');
	const $oneCHeadNotes = $('#oneCHeadNotes');
	const $oneCHeadPn = $('#oneCHeadPn');
	const $oneCHeadIntern = $('#oneCHeadIntern').click(parsePN);
	setTimeout(readHeaderFrom1C, 1000);
	function readHeaderFrom1C(){
		get1CTravelerSection('header').then(data => {
			if(!data || !data.startsWith('{')){
				console.warn('1C Profile Section header is empty or invalid:', data);
				return;
			}
			const json = JSON.parse(data);
			$oneCHeadDescr.text(description = json.Description || '');
			$oneCHeadNotes.text(json.Notes || '');
			$oneCHeadPn.text(partNumber = json.SalesSKU || '');
			$oneCHeadIntern.text(localPartNumber = json.Product || '').after($('<div', {text: json.WO || ''}));

			if(!onrenderSerialNumber)
				addSN(json);

		}).catch(errorHandler);
	}	
	function errorHandler(error){
		console.error('Error fetching 1C Profile Section converter:', error);
	}
	function parsePN({currentTarget:{innerText}}){
		const split = innerText.split('-');
		let message = '';

		switch (split[0].charAt(0)) {
		case 'A':
			message = "AntBUC\n";
			break;
		case 'P':
			message = "PicoBUC\n";
			break;
		case 'K':
			message = "KiloBUC\n";
 			break;
		case 'R':
			message = "Rack Mount\n";
			break;
		case 'F':
			message = "FemtoBUC\n";
		break;
	    }

		switch (split[1].charAt(0)) {
		case '1':
			message += "S-Band\n";
			break;
		case '2':
			message += "C-Band\n";
			break;
		case '3':
			message += "X-Band\n";
			break;
		case '4':
			message += "Ku-Band\n";
			break;
 		case '5':
			message += "Ka-Band\n";
			break;
		}

		switch (split[1].substring(0, 2)) {
		case '21':
			message += "LMI 5.725-6.025 GHz\n";
			break;
		case '22':
			message += "Standard 5.85-6.425 GHz\n";
			break;
		case '23':
			message += "Full 5.85-6.75 GHz\n";
			break;
		case '24':
			 message += "Russian 5.975-6.475 GHz\n";
			break;
		case '25':
			message += "Txt. 6.425-6.725 GHz\n";
			break;
		case '26':
			message += "Palapa 6.425-6.665 GHz\n";
			break;
		case '27':
			message += "Insat 6.725-7.025 GHz\n";
			break;
		case '28':
			message += "Tropo 4.4-5.0 GHz\n";
			break;
		case '31':
			message += "Std. 7.9-8.1 GHz\n";
			break;
		case '32':
			message += "Ext. 7.9-8.4 GHz\n";
			break;
		case '41':
			message += "Low. 12.75-13.25 GHz\n";
			break;
		case '42':
			message += "Ext. 13.75-14.5 GHz\n";
			break;
		case '43':
			message += "Std. 14.0-14.5 GHz\n";
			break;
		case '44':
			message += "Hifg. 14.5-14.8 GHz\n";
			break;
		}

		message += parseInt(split[2]) + ' W\n';
		if (split[3].charAt(0) === 'A')
			message += 'GaAs\n';
		else
			message += 'GaN\n';

		switch (split[3].charAt(1)) {
		case '1':
			message += "External\n";
			break;
		case '2':
			message += "Internal\n";
			break;
		case '3':
			message += "Autosense\n";
			break;
		}

		if (split[3].charAt(2) === 'A')
			message += 'AC\n';
		else
			message += 'DC\n';

		if (split[3].charAt(4) === '1')
			message += 'Redundant\n';

//		console.log(message);
		alert(message);
	}
}