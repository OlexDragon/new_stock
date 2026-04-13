import {type as typeFromInfo} from '../../panel-info.js'

const deviceType = {};

deviceType[1] = {}
deviceType[1].description =  "Main Controller";
deviceType[1].type =  "BAIS";

deviceType[2] = {}
deviceType[2].description =  "BIAS Board";
deviceType[2].type =  "BAIS";

deviceType[100] = {}
deviceType[100].description =  "PicoBUC l to Ku";
deviceType[100].type =  "BAIS";

deviceType[101] = {}
deviceType[101].description =  "PicoBUC l to C";
deviceType[101].type =  "BAIS";

deviceType[102] = {}
deviceType[102].description =  "C-Band SSPA";
deviceType[102].type =  "BAIS";

deviceType[110] = {}
deviceType[110].description =  "BUC Low Power Ku";
deviceType[110].type =  "BAIS_LOW_POWER";

deviceType[111] = {}
deviceType[111].description =  "BUC Low Power C";
deviceType[111].type =  "BAIS";

deviceType[112] = {}
deviceType[112].description =  "BUC Low Power SSPA";
deviceType[112].type =  "BAIS";

deviceType[113] = {}
deviceType[113].description =  "BUC Low Power LOW C";
deviceType[113].type =  "BAIS";

deviceType[199] = {}
deviceType[199].description =  "FUTURE_BIAS_BOARD";
deviceType[199].type =  "BAIS";

deviceType[200] = {}
deviceType[200].description =  "HPB_L_TO_KU";
deviceType[200].type =  "HP_BAIS";

deviceType[201] = {}
deviceType[201].description =  "HPB_L_TO_C";
deviceType[201].type =  "HP_BAIS";

deviceType[202] = {}
deviceType[202].description =  "HPB_SSPA";
deviceType[202].type =  "HP_BAIS";

deviceType[210] = {}
deviceType[210].description =  "KA_BAND";
deviceType[210].type =  "KA_BIAS";

deviceType[211] = {}
deviceType[211].description =  "KA_SSPA";
deviceType[211].type =  "KA_BIAS";

deviceType[250] = {}
deviceType[250].description =  "KU rack mount";
deviceType[250].type =  "CONTROLLER";

deviceType[251] = {}
deviceType[251].description =  "C rack mount";
deviceType[251].type =  "CONTROLLER";

deviceType[252] = {}
deviceType[252].description =  "C rack mount SSPA";
deviceType[252].type =  "CONTROLLER";

deviceType[260] = {}
deviceType[260].description =  "Transceiver";
deviceType[260].type =  "BAIS";

deviceType[300] = {}
deviceType[300].description =  "Ku rack mount SSPB";
deviceType[300].type =  "CONTROLLER";

deviceType[301] = {}
deviceType[301].description =  "Outdoor Redundancy Protection Controller";
deviceType[301].type =  "CONTROLLER";

deviceType[310] = {}
deviceType[310].description =  "Intelligent Redundant Protection Controller";
deviceType[310].type =  "CONTROLLER_IRPC";

deviceType[311] = {}
deviceType[311].description =  "2 switchs Redundancy protection controller";
deviceType[311].type =  "CONTROLLER";

deviceType[313] = {}
deviceType[313].description =  "Outdoor Redundancy Protection Controller";
deviceType[313].type =  "CONTROLLER";

deviceType[410] = {}
deviceType[410].description =  "DLRS";
deviceType[410].type =  "CONTROLLER";

deviceType[411] = {}
deviceType[411].description =  "DLRS2";
deviceType[411].type =  "CONTROLLER_ODRC";	// Outdoor Downlink Redundancy Controller

deviceType[412] = {}
deviceType[412].description =  "1:2 Redundancy LNB";
deviceType[412].type =  "LNB";

deviceType[500] = {}
deviceType[500].description =  "L to Ku Converte";
deviceType[500].type =  "CONVERTER";

deviceType[1001] = {}
deviceType[1001].description =  "L to Ku Converter";
deviceType[1001].type =  "CONVERTER";

deviceType[1002] = {}
deviceType[1002].description =  "L to 70 Converter";
deviceType[1002].type =  "CONVERTER";

deviceType[1003] = {}
deviceType[1003].description =  "140 to L Converter";
deviceType[1003].type =  "CONVERTER";

deviceType[1004] = {}
deviceType[1004].description =  "L to 140 Converter";
deviceType[1004].type =  "CONVERTER";

deviceType[1005] = {}
deviceType[1005].description =  "L to Lu Converter";
deviceType[1005].type =  "CONVERTER";

deviceType[1006] = {}
deviceType[1006].description =  "L to C Converter";
deviceType[1006].type =  "CONVERTER";

deviceType[1007] = {}
deviceType[1007].description =  "70 to Ku Converter";
deviceType[1007].type =  "CONVERTER";

deviceType[1008] = {}
deviceType[1008].description =  "Ku to 70 Converter";
deviceType[1008].type =  "CONVERTER";

deviceType[1009] = {}
deviceType[1009].description =  "140 to Ku Converter";
deviceType[1009].type =  "CONVERTER";

deviceType[1010] = {}
deviceType[1010].description =  "Ku to 140 Converter";
deviceType[1010].type =  "CONVERTER";

deviceType[1011] = {}
deviceType[1011].description =  "Lu to L Converter";
deviceType[1011].type =  "CONVERTER";

deviceType[1012] = {}
deviceType[1012].description =  "C to L Converter";
deviceType[1012].type =  "CONVERTER";

deviceType[1013] = {}
deviceType[1013].description =  "L to DBS Converter";
deviceType[1013].type =  "CONVERTER";

deviceType[1019] = {}
deviceType[1019].description =  "L to KA Converter";
deviceType[1019].type =  "CONVERTER_KA";

deviceType[1021] = {}
deviceType[1021].description =  "L to X Converter";
deviceType[1021].type =  "CONVERTER";

deviceType[1051] = {}
deviceType[1051].description =  "L to SSPA Converter";
deviceType[1051].type =  "CONVERTER";

deviceType[1052] = {}
deviceType[1052].description =  "Modul";
deviceType[1052].type =  "CONVERTER";

deviceType[1100] = {}
deviceType[1100].description =  "Reference Board";
deviceType[1100].type =  "REFERENCE_BOARD";

deviceType[2001] = {}
deviceType[2001].description =  "Bias Board Modul";
deviceType[2001].type =  "BAIS";

export default (typeId) =>{
	if(!(typeFromInfo || typeId))
		return;
	return deviceType[typeId ? typeId : typeFromInfo[0]].type;
}

export function description(typeId){
	if(!(typeFromInfo || typeId))
		return;
	return deviceType[typeId ? typeId : typeFromInfo[0]].description;
}
