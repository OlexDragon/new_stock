import {unitSerialNumber} from '../../calibration.mjs'

export let serialNumber;
if(unitSerialNumber)
(()=>{
	const snSplit = unitSerialNumber.split(/-(.+)/);
	if(snSplit.length < 2)
		snSplit.push(snSplit[0].replace(/[^0-9]/g, ''));
	serialNumber = snSplit[1];
})();
