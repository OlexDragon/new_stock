class SerialPortServer{

	#$spServers;
	#ports;
	#busyCount;
	constructor(){
		this.#$spServers = $('#spServers').change(this.#serverChange.bind(this));
		this.#busyCount = 0;
	}
	get serverName(){
		return this.#$spServers.val();
	}
	/**
	 * @param {string} name - The name of the server to select
	 */
	set serverName(name){
		if(!name)
			return;
		const option = this.#$spServers.children().get().find(el=>el.value===name);
		if(!option){
			alert(`Server named "${name}" not found.\nThe server may not be running.`)
			return;
		}
		option.selected = true;
	}
	/**
	 * Fetches the list of serial ports from the selected server and calls the callback with the result.
	 * If the list is already cached and update is not requested, it returns the cached list.
	 * @param {Function} callBack - Function to call with the list of ports or error
	 * @param {boolean} [update=false] - If true, forces fetching a fresh list from the server
	 * Note: This function handles concurrent calls by queuing them if a fetch is already in progress.
	 * If the busy count exceeds 50, it resets to prevent a potential stack overflow from too many queued calls.
	 * The list of ports is cached for the duration of the session and can be invalidated by setting update to true or changing the server.
	 * The server is selected via a dropdown, and the selected server's name is stored in cookies for persistence across sessions.
	 * The function also handles the case where no server is selected or the server does not respond with a list of ports, in which case it calls the callback with an error or empty result.
	 * **/
	allPorts(callBack, update){

		if(!this.#$spServers){
			console.error('Server Select does not found.');
			callBack();
			return;
		}

		var spHost = this.#$spServers.val();
		if(!spHost){
			callBack();
			return;
		}

		if(!update && this.#ports){
			callBack(this.#ports);
			return
		}else
			this.#ports = undefined;

		if(this.#busyCount){
			console.log('Method allPorts is busy');
			if(this.#busyCount>50){
				this.#busyCount = 0;
				console.warn('Stack overflow');
				return;
			}
			setTimeout(()=>this.allPorts(callBack), 100);
			return;
		}
		++this.#busyCount;
		$.post(`http://${spHost}/serial-ports`)
		.then(r=>{
			if(Array.isArray(r)){
				this.#ports = r;
//				setTimeout(()=>this.#ports=undefined, 10000);
			}
			this.#busyCount = 0;
			callBack(r);
		});
	}
	#serverChange({currentTarget:{value}}){
		this.#ports = null;
		Cookies.set("spServers", value, { expires: 99, path: '' });
		subscribers.forEach(pub=>pub(value));
	}
}

const subscribers = [];
export function subscribe(callback) {
	subscribers.push(callback);
}

const controller = new SerialPortServer();
export default controller;

//for(let i=0; i<10; i++){
//	setTimeout(()=>controller.allPorts(t=>console.log(t)), i*20);
//}

$('#btn-http-comport').click(()=>{
	let httpServer = controller.serverName;
	if(httpServer){
		let url = 'http://' + httpServer
		let win = window.open(url, '_blank');
		if(win) 
			win.focus();
		else
			alert('Please allow popups for this website');
	}
});
// Get HTTP Serial Port Server from the cookies
const cookie = Cookies.get("spServers");
if(cookie){
	controller.serverName = cookie;
}
