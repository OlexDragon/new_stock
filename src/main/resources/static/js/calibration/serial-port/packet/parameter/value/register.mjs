import {parseToIntArray, intToBytes} from '../../service/converter.mjs'

export default class Register{
	static parseRegister(bytes){
		const ints = parseToIntArray(bytes);
		return new Register(ints[0], ints[1], ints[2]);
	}

	#index;
	#addr;
	#value;

	constructor(index, address, value){
		this.#index = index;
		this.#addr = address;
		this.#value = value;
	}

	get index(){
		return this.#index;
	}

	get address(){
		return this.#addr;
	}

	get value(){
		return this.#value;
	}

	set value(v){
		this.#value = v;
	}

	toBytes(){
		const bytes = [];
		bytes.push(intToBytes(this.#index));
		bytes.push(intToBytes(this.#addr));
		if(this.#value!==undefined)
			bytes.push(intToBytes(this.#value));
		return bytes.flat();
	}

	clone(){
		return new Register(this.#index, this.#addr, this.#value);
	}

	toString(){
		return `index: ${this.#index}; addr: ${this.#addr}; value: ${this.#value}`;
	}
}