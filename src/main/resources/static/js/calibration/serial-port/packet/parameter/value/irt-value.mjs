
export default class IrtValue{

	constructor(value, prefix, postfix, divider){
		this.value = value;
		this.prefix = prefix;
		this.postfix = postfix;
		this.divider = divider;
	}

	toString(){
		return `${this.prefix ?? ""}${this.divider ? this.value/this.divider : this.value}${this.postfix ?? ""}`
	}
}
