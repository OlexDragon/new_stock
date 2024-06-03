package irt.components.beans;

public interface SerialPortData {

	String getSpName();
	int getTimeout();
	void setErrorMessage(String message);
	Baudrate getBaudrate();

}
