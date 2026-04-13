import AopcStatus from './aopc-status.js'

export default class FcmStatus{
	static statusBits = Object.freeze({PLL1: 0, PLL2:1, MUTE: 2,  MUTE_TTL: 3, PLL3: 4, LOCK: 5, PRESET: 6, INPUT_LOW: 7, AOPC_0: 8, AOPC_1: 9, AOPC_2:10, LNB_POWER_0: 11, '10 MHz': 12, '100 MHz': 13, HSE: 31});

	#number;
	constructor(number){
		this.#number = number;
	}

	get all(){

		let lnb = 0;
		let aopc = 0;
		const status =  Object.entries(FcmStatus.statusBits).reduce((a,[key, v])=>{

			const mask = 1<<v;
			const r = this.#number&mask;

			if(r)
				a.push(key);
			return a;
		}, []);

		const aopcStatus = new AopcStatus(aopc>>FcmStatus.statusBits.AOPC_0).toString();
		if(aopcStatus)
			status.push(aopcStatus);

		return status;
	}
}