package irt.components.beans.irt;

import java.math.BigDecimal;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.ToString;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class IrtFrequency {
//	private final static Logger logger = LogManager.getLogger();

	// Frequency in MHz
	private final Optional<BigDecimal> value;

	public IrtFrequency(String value) {

		final Optional<Integer> oIndex = Optional.of(value.lastIndexOf(" ")).filter(index->index>=0);

		final int power = oIndex.map(index->value.substring(++index)).map(String::trim).map(String::toUpperCase).map(FrequencyUnit::valueOf).orElse(FrequencyUnit.HZ).getPower();

		this.value = oIndex.map(index->value.substring(0, index)).map(BigDecimal::new)

				.map(
						bd->{

							final int mhzPower = FrequencyUnit.MHZ.getPower();

							if(power==mhzPower)
								return bd;

							int p = mhzPower - power;
							final double divider = Math.pow(10, p);
//							logger.error("power: {}; mhzPower: {}; p:{}; divider:{};", power, mhzPower, p, divider);
							return bd.divide(new BigDecimal(divider));
						});
	}

	@Getter 
	public enum FrequencyUnit{
		HZ(1),
		KHZ(3),
		MHZ(6),
		GHZ(9);

		private final int power;

		private FrequencyUnit(int power) {
			this.power = power;
		}
	}
}
