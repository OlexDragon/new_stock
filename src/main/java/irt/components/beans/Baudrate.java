package irt.components.beans;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public enum Baudrate {
	BAUDRATE_9600	(9600),
	BAUDRATE_19200	(19200),
	BAUDRATE_38400	(38400),
	BAUDRATE_57600	(57600),
	BAUDRATE_115200	(115200);

	private final int value;

	public int getValue() {
		return value;
	}

	@Override
	public String toString(){
		return Integer.toString(value);
	}

	public static Baudrate valueOf(int baudrate) {
		Baudrate result = null;

		for(Baudrate b:values())
			if(b.getValue()==baudrate){
				result = b;
				break;
			}

		return result;
	}
}
