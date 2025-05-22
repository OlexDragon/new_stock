class CalibrationManagement{

	interval;
	outrutPower;

	constructor(){
		this.createUnit();
	}

	createUnit(){

		if(serialNumber)
			this.serialNumber = serialNumber;

		else{
			const sn = prompt("Please enter Unit Serial Number:", "IRT-");

			if (!sn || !sn.trim())
				return 'No Unit Serial Number to connect to.'

			this.serialNumber = sn;
		}

		this.unit = new IrtUnit(this.serialNumber);
	}
	setOutpoutPower(action, toSet){

		if(this.interval){
			console.log('CalibrationManagement is busy.')
			return false;
		}

		const outrutPower = [];
		this.interval = setInterval(()=>getOutputPower(d=>{

			outrutPower.push(d);
			if(outrutPower.length>1){
				this.outrutPower = getMinMax(outrutPower)
				if(this.outrutPower.difference>0.8){
					this.stop();
					alert('The output power is unstable.')
					throw new Error('The output power is unstable.');
				}
			}

			if(outrutPower.length>5){
				clearInterval(this.interval);
				toolOutputPower(ip=>{
					const difference = toSet - this.outrutPower.average
					if(Math.abs(difference)<=0.2){
						this.stop();
						this.outrutPower.toolOutput = ip;
						action(this.outrutPower)
						return;
					}
					const toSetInput = ip + difference
					toolOutputPower(undefined, toSetInput);
					setTimeout(()=>{
						this.interval = undefined;
						this.setOutpoutPower(action, toSet);
					}, 100);
				});
			}
		}), 100);

		return true;
	}
	stop(){
		clearInterval(this.interval);
		this.interval = undefined;
	}
}
function getMinMax(array){
	const minMax = {};
	minMax.min = Math.min(...array);
	minMax.max = Math.max(...array);
	minMax.difference = minMax.max - minMax.min;
	minMax.average = array.reduce((a, b) => a + b) / array.length;
	return minMax;
}
//const cm = new CalibrationManagement();
//setTimeout(()=>cm.setOutpoutPower(v=>console.log(v), 33), 1500);