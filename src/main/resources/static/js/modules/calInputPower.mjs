import {setFromCookies} from './calInputPowerF.mjs';
import {$ipComPorts} from './calInputPowerE.mjs';

// Priset
gerSerialPorts(ports=>{
	if(!ports){
		alert('It looks like the Serial Port Server is down.');
		return;
	}
	$('<option>', {selected: 'selected', disabled: 'disabled', hidden: 'hidden', title:'Remote Serial Port.', text:'Select Remote Serial Port.'}).appendTo($ipComPorts);
	$.each(ports, (i, portName)=>{
		$('<option>', {value: portName, text: portName}).appendTo($ipComPorts);
	});
	setFromCookies();
});

const x = [];
const y = [];
function clear(){
	x.length = y.length = 0;
	ipChart.update();
}
const ipChart = new Chart(document.getElementById('ipChart'), {
									type: "line",
									data: {
											labels: x,
											datasets: [{
													label: 'Calibration VAlues',
													fill: false,
													lineTension: 0,
													backgroundColor: "rgba(0,0,255,1.0)",
													borderColor: "rgba(0,0,255,0.1)",
													data: y,
													yAxisID: 'y',
											}]
 									},
									options: {
				    						responsive: true,
				    					    interaction: {
				    					        mode: 'index',
				    					        intersect: false,
				    					      },
				    			    		stacked: false,
				    			    		plugins: {
				    			      			title: {
				    			        			display: true,
				    			        			text: $serialNumber.text() + ' - ' + $('#unit_description').children('h4').text()
				    			      			}
				    			    		},
				    			    		scales: {
				    			    			y: {
				    			    				ticks: {
				    			    					color: "blue"
				    			    				},
				    			        			type: 'linear',
				    			        			display: true,
				    			        			position: 'left',
				    			        		},
				    			    		}
									}
							});
$modal.modal('show');
export {ipChart, x, y, clear};
