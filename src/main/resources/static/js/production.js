$('a').click((e)=>{
	e.preventDefault();
	$('#productionContainer').load(e.currentTarget.href,data=>{
		let r = data;
	});
});