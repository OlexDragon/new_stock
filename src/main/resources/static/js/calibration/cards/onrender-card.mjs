import {show as showToast} from '../../toast-worker.mjs';
import {unitSerialNumber} from '../../calibration.mjs'

if(unitSerialNumber){
    $.get(`/onrender/sn?sn=${unitSerialNumber}`)
        .done(data => {

            if (!data)
                return;

            $('#webSn').text(serialNumber = data.serialNumber);
            $('#webId').text(id = data.id);
            $('#webPn').text(partNumber = data.partNumber.partNumber);
            $('#webDescr').text(data.partNumber.description);
        });
}

export let serialNumber;
export let id;
export let partNumber;
export function addSN(data){

		$.get('/onrender/exists', { sn: unitSerialNumber })
		.done(exists => {
			if (exists) {
				console.log('OnRender data already exists for SN:', unitSerialNumber);
				return;
			} 
			$.post('/onrender/save-sn', { sn: unitSerialNumber, pn: data.SalesSKU, descr: data.Description })
			.done(saved => {
				if(saved)
					showToast('Add Serial Number to OnRender', `Serial number ${unitSerialNumber} has been added.`, 'bg-success');
	            })
			.fail(error => {
				console.error('Error adding OnRender data:', error);
			});
		});
	}
