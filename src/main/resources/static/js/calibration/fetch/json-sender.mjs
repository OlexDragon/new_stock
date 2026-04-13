import spServer from '../serial-port-server.mjs';
import {ajaxJSON} from './fetch.mjs';
import Packet from '../serial-port/packet/packet.mjs';
import packetType from '../serial-port/packet/packet-properties/packet-type.mjs'

export function sendPacket(spName, packet, action){

	if(!packet){
		if(action)
			action({error: 'Packet is not present.'});
		else
			console.warn('Packet is not present.');
		return;
	}

	const command = toCommand(spName, packet, action ? true : false);

	if(command.error){
		if(action)
			action(command);
		else
			console.warn(command.error);
		return;
	}

	const url = command.toSend ? `http://${spServer.serverName}/bytes` : `http://${spServer.serverName}/read`;
		ajaxJSON(url, command)
		.done(data=>{

			if(!action){
				if(data.errorMessage)
					console.log('error', data.errorMessage);
				return;
			}

			let p;
			if(data.errorMessage)
				p = {error: data.errorMessage};
			else if(!data.answer?.length)
				p = {error: "No answer.", timeout: data.timeout};
			else{
				const bytes = answerToBytes(data.answer);
//				console.log(bytes);
				p = new Packet(bytes, packet.linkHeader?.unitAddr); // true - packet with LinkHeader
//				console.log(p);

				if(p?.header?.packetId !== packet.header.packetId) 
					p = {error: 'Received wrong packet.', packet: p};

				else if(p.header.error || packet.header.groupId === packetType.acknowledgement) 
					p = {error: p.header.toString(), packet: p};

				else if(!packet.payloads?.length)
					p = {error: 'Packet does not have payloads.', packet: p};
			}
			if(p.getAcknowledgement)
				sendPacket(spName, p.getAcknowledgement());
			action(p);
		})
		.fail(function(error) {

			if(error.statusText!='abort'){
				var responseText = error.responseText;
				if(responseText){
					if(action)
						action({error: error.responseText});
					else{
						console.error(error.responseText);
						alert(error.responseText);
					}
				}else{
					if(action)
						action({error: "Server error. Status = " + error.status});
					else{
						console.error("Server error. Status = " + error.status);
						alert("Server error. Status = " + error.status);
					}
				}
			}
		});
	}
export function toCommand(spName, packet, getAnswer){

		const hostName = spServer.serverName;
		if(!hostName){
			console.log("Unable to get HTTP Serial Port Hostname.");
			alert('Unable to get HTTP Serial Port Hostname.');
			return;
		}

		const command = {};

		if(!spName){
			logger.warn("The serial port is not selected.");
			return {error: 'The serial port is not selected.'};
		}
		command.spName = spName;
		command.baudrate = 'BAUDRATE_115200';
		if(packet)
			command.toSend = packet.toSend();
		command.termination = 126;
		command.getAnswer = getAnswer;
		command.timeout = 1000;
		return command;
	}
	function answerToBytes(answer){
		const bytes = [];
		const a  = atob(answer);
		for (var i = 0; i < a.length; i++) {
			bytes.push(a.charCodeAt(i));
		}
		return bytes;
	}
