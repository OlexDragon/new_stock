import CachedInfo from './cached-info.mjs';

// depricated, but still used by some legacy code, so we keep it here for now

class CalibrationInfo extends CachedInfo {
	constructor() {
		super($.get, '/calibration/rest/monitorInfo', 1000 );
	}
}

const ci = new CalibrationInfo();
export default ci;


/* Returned data example:
{
	{
	    "bias": {
	        "class": "biasinfo",
	        "power1": {
	            "status": "NORMAL",
	            "unit": "mV",
	            "value": 464
	        },
	        "power2": {
	            "status": "NORMAL",
	            "unit": "mV",
	            "value": 203
	        },
	        "temperature": 37.6,
	        "title": "On-board sensors:"
	    },
	    "boards": null,
	    "epsu": {
	        "class": "psuinfo",
	        "data": [
	            {
	                "data": {
	                    "text": "<pre>\nOn-board sensors:\n       FP_DET (0x00):     0.0 mV\n       RP_DET (0x01):     0.0 mV\n\n  PWR_ENTRY_1_MON (100): 25.46 V\n    Current_mon_1 (101): 3.15 A\n\n  PWR_ENTRY_2_MON (102): 25.40 V\n    Current_mon_2 (103): 3.55 A\n\n  PWR_ENTRY_3_MON (104): 25.24 V\n    Current_mon_3 (105): 2.21 A\n\n  PWR_ENTRY_4_MON (106): 25.28 V\n    Current_mon_4 (107): 2.14 A\n\n  PWR_ENTRY_5_MON (108): 11.49 V\n    Current_mon_5 (109): 1.88 A\n\n  PWR_ENTRY_6_MON (110): 0.18 V\n    Current_mon_6 (111): 0.01 A\n\n          -5V_MON (112): -4.98 V\n      DAC_REF_mon (113): 3.01 V\n\n      TEMP_SENSE1 (114): 30.00 degC\n      TEMP_SENSE2 (115): 31.75 degC\n    TMON_MEAS_INT (116): 33.00 degC\n      Temp_S_Filt (117): 0.01 V (7.20 degC)\n  TSENSE_CONV_RES (118): 26.75 degC\n\n\n</pre>"
	                },
	                "name": "HPBM1",
	                "powerSuplyData": [
	                    {
	                        "name": "FP_DET (0x00)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "mV",
	                            "value": 0
	                        }
	                    },
	                    {
	                        "name": "RP_DET (0x01)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "mV",
	                            "value": 0
	                        }
	                    },
	                    {
	                        "name": "PWR_ENTRY_1_MON (100)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "V",
	                            "value": 25.46
	                        }
	                    },
	                    {
	                        "name": "Current_mon_1 (101)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "A",
	                            "value": 3.15
	                        }
	                    },
	                    {
	                        "name": "PWR_ENTRY_2_MON (102)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "V",
	                            "value": 25.4
	                        }
	                    },
	                    {
	                        "name": "Current_mon_2 (103)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "A",
	                            "value": 3.55
	                        }
	                    },
	                    {
	                        "name": "PWR_ENTRY_3_MON (104)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "V",
	                            "value": 25.24
	                        }
	                    },
	                    {
	                        "name": "Current_mon_3 (105)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "A",
	                            "value": 2.21
	                        }
	                    },
	                    {
	                        "name": "PWR_ENTRY_4_MON (106)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "V",
	                            "value": 25.28
	                        }
	                    },
	                    {
	                        "name": "Current_mon_4 (107)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "A",
	                            "value": 2.14
	                        }
	                    },
	                    {
	                        "name": "PWR_ENTRY_5_MON (108)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "V",
	                            "value": 11.49
	                        }
	                    },
	                    {
	                        "name": "Current_mon_5 (109)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "A",
	                            "value": 1.88
	                        }
	                    },
	                    {
	                        "name": "PWR_ENTRY_6_MON (110)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "V",
	                            "value": 0.18
	                        }
	                    },
	                    {
	                        "name": "Current_mon_6 (111)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "A",
	                            "value": 0.01
	                        }
	                    },
	                    {
	                        "name": "-5V_MON (112)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "V",
	                            "value": -4.98
	                        }
	                    },
	                    {
	                        "name": "DAC_REF_mon (113)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "V",
	                            "value": 3.01
	                        }
	                    },
	                    {
	                        "name": "TEMP_SENSE1 (114)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "degC",
	                            "value": 30
	                        }
	                    },
	                    {
	                        "name": "TEMP_SENSE2 (115)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "degC",
	                            "value": 31.75
	                        }
	                    },
	                    {
	                        "name": "TMON_MEAS_INT (116)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "degC",
	                            "value": 33
	                        }
	                    },
	                    {
	                        "name": "Temp_S_Filt (117)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "V",
	                            "value": 0.01
	                        }
	                    },
	                    {
	                        "name": "TSENSE_CONV_RES (118)",
	                        "value": {
	                            "status": "NORMAL",
	                            "unit": "degC",
	                            "value": 26.75
	                        }
	                    }
	                ]
	            },
	            {
	                "data": null,
	                "name": null,
	                "powerSuplyData": null
	            }
	        ],
	        "title": "External PSU monitor:",
	        "visible": 0
	    },
	    "hss1": null,
	    "hss2": null,
	    "power": null,
	    "temperature": null
	}
*/
