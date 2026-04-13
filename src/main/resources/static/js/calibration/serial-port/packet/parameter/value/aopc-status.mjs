export default class AopcStatus{

	#number;
	constructor(number){
		this.#number = number;
	}

	toString(){
		switch(this.#number){
//
//		case 0:
//			return 'AOPC_NONE';
//
//		case 1:
//			return 'AOPC_OFF';

		case 2:
			return 'AOPC_NORMAL';

		case 3:
			return 'AOPC_LOW';

		case 4:
			return 'AOPC_HIGH';

		case 5:
			return 'AOPC_SUSPENDED';

		case 6:
			return 'AOPC_OVERDRIVE';
		}
	}
}