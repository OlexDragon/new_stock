const irtDiagnostic = {};

irtDiagnostic.configuration = {};
irtDiagnostic.configuration.command = 'config';
irtDiagnostic.configuration.fields = 0;

irtDiagnostic.measurement = {};
irtDiagnostic.measurement.command = 'status';
irtDiagnostic.measurement.fields = 0;

irtDiagnostic.info = {};
irtDiagnostic.info.command = 'info';
irtDiagnostic.info.fields = 0;

irtDiagnostic.registers = {};
irtDiagnostic.registers.command = 'regs';
irtDiagnostic.registers.fields = 3;

irtDiagnostic.hwInfo = {};
irtDiagnostic.hwInfo.command = 'hwinfo';
irtDiagnostic.hwInfo.fields = 1;

irtDiagnostic.alarms = {};
irtDiagnostic.alarms.command = 'alarms';
irtDiagnostic.alarms.fields = 0;

irtDiagnostic.profile = {};
irtDiagnostic.profile.command = 'profile';
irtDiagnostic.profile.fields = 0;

irtDiagnostic.logfile = {};
irtDiagnostic.logfile.command = 'logfile';
irtDiagnostic.logfile.fields = 0;

class IrtUnit{
	constructor(serialNumber){

		if(!serialNumber)
			throw new Error("Serial number missing.");

		this.serialNumber = serialNumber;
		$.get('/calibration/rest/all-modules', {sn: this.serialNumber})
		.done(data=>{
			this.modules = data;
		});
	}

	moduleCheck(module){

		if(!this.modules)
			throw new Error("No information about modules.");

		if(!module)
			module = this.modules.System;

		else if(typeof module === 'string')
			module = this.modules[module];

		return module;
	}

	getInfo(action, module, errorAction){

		module = this.moduleCheck(module);

		if(!module)
			module = this.modules.System;

		else if(typeof module === 'string')
			module = this.modules[module];

		$.post('/calibration/rest/module-info', {sn: this.serialNumber, moduleIndex: module})
		.done(action)
		.fail(error=>error(error, errorAction));
	}

	
	login(action, errorAction){

		$.post('/calibration/rest/login', {sn: this.serialNumber})
		.done(action)
		.fail(error=>error(error, errorAction));
	}

	statuses = ['OFF', 'ON'];
	calibrationMode(action, status, errorAction){

		module = this.moduleCheck(module);

		if(typeof status === "undefined")
			status = '';
		else if(typeof status === 'number')
			status = this.statuses[status];

		$.post('/calibration/rest/calibration-mode', {sn: this.serialNumber, moduleIndex: module, status: status})
		.done(data=>{
			if(!data)
				throw new Error("There is no response from the unit.");

			action(data["Calibration mode"]);
		})
		.fail(error=>error(error, errorAction));
	}

	diagnostic(action, module, command, index, address, value){

		module = this.moduleCheck(module);

		if(!command?.command)
			command = irtDiagnostic.info;

		if(!command.fields)
			index = address = value = '';

		else{
			if(!index)
				throw new Error('The "index" property must be set. index = ' + index);

			if(address === undefined)
				address = '';

			if(value === undefined)
				value = '';
		}

		$.post('/calibration/rest/diagnostic', {sn: this.serialNumber, moduleIndex: module, command: command.command, index: index, address: address, value: value})
		.done(action)
		.fail(error=>errorAction(error));
	}

	fcmDac(action, index){
		if(!index)
			index = 2;
		const module = this.moduleCheck();
		this.diagnostic(data=>dacAction(action, data, index), module, irtDiagnostic.registers, 100);
	}
	measurement(action, module){

		module = this.moduleCheck(module);
		$.post('/calibration/rest/measurement', {sn: this.serialNumber, moduleIndex: module})
		.done(action)
		.fail(error=>errorAction(error));
	}
}

// indeex - DAC number; data - text below; return decimal value or arrat of lines

// DAC1: 0x7AD (1965)
// DAC2: 0xE34 (3636)
// DAC3: 0xC76 (3190)
// DAC4: 0x1F4 (500)
function dacAction(action, data, index){

	const d = data.split('\n').filter(line=>line.includes(index + ':'));
	if(d.length){
		const dac = d[0].split('(')[1].replace(")", '')
		action(+dac);
	}else
		action(d);
}
function errorAction(error, errorAction){
	console.error(error);
	if(errorAction)
		errorAction(error);
}