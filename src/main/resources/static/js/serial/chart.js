
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
		if(!y.length || y[index]<oldY[i]){
			if(y.length>2){
				const firstX = x[y.length-2];
				const midleX = x[index];
				const lastX = oldX[i];
				const timesX1 = midleX - firstX;
				const timesX2 = lastX - firstX;

				const firstY = y[y.length-2];
				const midleY = y[index];
				const lastY = oldY[i];
				let timesY1 = ((midleY - firstY)/timesX1);
				let timesY2 = ((lastY - firstY)/timesX2);
				if(timesY1>10 || timesY2>10){
					timesY1 = timesY1.toFixed(0);
					timesY2 = timesY2.toFixed(0);
				}else if(timesY1>1 || timesY2>1){
					timesY1 = timesY1.toFixed(1);
					timesY2 = timesY2.toFixed(1);
				}else{
					timesY1 = timesY1.toFixed(2);
					timesY2 = timesY2.toFixed(2);
				}
				if(timesY1 == timesY2){
					x[index] = lastX;
					y[index] = lastY;
					continue;
				}
			}
			x.push(oldX[i]);
			y.push(oldY[i]);
			this.update();
		}
	}
}
Chart.prototype.reset = function(){
	x.length = y.length = 0;
	x.push(...oldX);
	y.push(...oldY);
	this.update();
}
