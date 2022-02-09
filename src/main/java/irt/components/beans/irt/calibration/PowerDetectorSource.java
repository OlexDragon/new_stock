package irt.components.beans.irt.calibration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum PowerDetectorSource {

	ON_BOARD_SENSOR		("Number(base/value).toFixed(4)"),
	HST1_CURRENT		("Number(base/value).toFixed(4)"),
	FCN_INPUT_AND_GAIN	("Number(base - value).toFixed(1)"),
	FCM_OUTPUT			("Number(base - value).toFixed(1)"),
	HST2_CURRENT		("Number(base/value).toFixed(4)"),
	FCM_INPUT			("Number(base - value).toFixed(1)"),
	UNDEFINED			("Number(base/value).toFixed(4)");	// Default

	 private final String jsFunction;
}
