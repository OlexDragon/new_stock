let prinsipal;
let timeout;
export default async function getPrincipal(){
	if(!prinsipal){
		prinsipal =  await fetchPrincipal();
		if(typeof prinsipal === 'string')
			prinsipal = null;
		else{
			clearTimeout(timeout);
			timeout = setTimeout(()=>prinsipal=null, 10000);
		}
	}
	return prinsipal;
}
async function fetchPrincipal(){
	return await $.get('/users/principal');
}