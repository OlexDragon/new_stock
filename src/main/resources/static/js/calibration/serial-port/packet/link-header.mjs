export default class LinkHeader{
	constructor(unitAddr){
	// From bytes
		if(Array.isArray(unitAddr)){
			if(unitAddr.length==LINK_HEADER_SIZE)
				this.unitAddr = unitAddr;
			else
				this.unitAddr = unitAddr[0];
			return
		}
		this.unitAddr = unitAddr;
	}
	toBytes(){
		if(Array.isArray(this.unitAddr))
			return this.unitAddr;
		return [this.unitAddr, 0, 0, 0];
	}
	toString(){
		return this.unitAddr;
	}
}

export const LINK_HEADER_SIZE = 4;