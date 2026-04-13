export default class Parameters{

	#parameters;
	#names;
	#name

	constructor(parameters, name){
		this.name = name;
		this.#parameters = Object.freeze(parameters);
		this.#names = Object.keys(this.#parameters).reduce((a,key)=>{a[this.#parameters[key].code] = key; return a;}, []);
	}

	get parameters(){
		return this.#parameters;
	}

	get names(){
		return this.#names;
	}

	toCode(name){

		if(typeof name === 'number')
			return name;

		return this.#parameters[name].code;
	}

	toName(code){
		return this.#names[code]; 
	}

	parser(value){
		const c = this.toCode(value);
		const n = this.toName(c);
		return this.#parameters[n]?.parser;
	}

	toString(value){
		const c = this.toCode(value);
		return `${this.#name}: ${this.toName(c)} (${c})`;
	}

	get all(){
		return 255;
	}
}