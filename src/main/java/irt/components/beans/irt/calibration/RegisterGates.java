package irt.components.beans.irt.calibration;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class RegisterGates implements Diagnostics{
	private final static Logger logger = LogManager.getLogger();

	private Integer gate0;
	private Integer gate1;
	private Integer gate2;
	private Integer gate3;
	private Integer gate4;
	private Integer gate5;
	private Integer gate6;
	private Integer gate7;
	private Integer gate8;
	private Integer gate9;
	private Integer gate10;
	private Integer gate11;
	private Integer gate12;
	private Integer gate13;
	private Integer gate14;
	private Integer gate15;

	public RegisterGates(String str) {
		logger.traceEntry(str);
		
		try {
			final String[] split = str.split("GATE");
			Arrays.stream(split).map(s->s.split(":", 2)).filter(s->s.length>1).map(s->new BasicNameValuePair(s[0].trim(), s[1].trim()))
			.forEach(pair->{
				try {
					final Field field = RegisterGates.class.getDeclaredField("gate" + pair.getName());
					field.set(this, Integer.parseInt(pair.getValue().split("x")[1], 16));
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					logger.catching(e);
				}
			});
		} catch (Exception e) {
			logger.catching(e);
		}
	}
}
