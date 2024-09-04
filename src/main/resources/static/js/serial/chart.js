
const x = [];
const y = [];

let oldX, oldY;

function getChart(){
	x.length = y.length = 0;
	return new Chart(document.getElementById('ipChart'), {
									type: "line",
									data: {
											labels: x,
											datasets: [{
													label: 'Calibration Values',
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

}
Chart.prototype.optimize =  function(start){

	oldX = [...x];
	oldY = [...y];

	if(!start)
		start = -50;

	x.length = y.length = 0;

	for(let i=0; i<oldX.length; i++){

		if(oldX[i]<start)
			continue;

		const index = y.length-1;

		if(index<0 || y[index]<oldY[i]){

			if(index>0){

				const firstY = y[index-1];
				const midleY = y[index];
				const lastY = oldY[i];

				const timesY1 = midleY - firstY;
				const timesY2 = lastY - midleY;

				if(timesY1 == timesY2){
					x[index] = oldX[i];
					y[index] = lastY;
					continue;
				}

				const length1 = timesY1.toString().length;
				const length2 = timesY2.toString().length;
				if(length1>1 || length2>1){
					let y1, y2;
					if(length1>2){
						const power = length1 -2;
						const divider = Math.pow(10, power);
						y1 = Math.round(timesY1 / divider);
						y2 = Math.round(timesY2 / divider);
					}else{
						y1 = timesY1;
						y2 = timesY2;
					}

					if(Math.abs(y1-y2)<10){
						x[index] = oldX[i];
						y[index] = lastY;
						continue;
					}
				}
			}
			x.push(oldX[i]);
			y.push(oldY[i]);
		}
	}
	this.update();
}
Chart.prototype.reset = function(){
	x.length = y.length = 0;
	x.push(...oldX);
	y.push(...oldY);
	this.update();
}
