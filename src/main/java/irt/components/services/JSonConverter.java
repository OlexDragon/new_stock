package irt.components.services;

import java.util.Optional;

import javax.persistence.AttributeConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSonConverter implements AttributeConverter<Double[], String> {
	private final static Logger logger = LogManager.getLogger();

	@Override
	public String convertToDatabaseColumn(Double[] list) {

		final ObjectMapper mapper = new ObjectMapper();

		try {

			return mapper.writer().withDefaultPrettyPrinter().writeValueAsString(list);

		} catch (JsonProcessingException e) {
			logger.catching(e);
		}

		return null;
	}

	@Override
	public Double[] convertToEntityAttribute(String json) {

		return Optional.ofNullable(json).filter(j->!j.isEmpty())

				.map(
						j->{

							try {

								return new ObjectMapper().readValue(j, Double[].class);

							} catch (JsonProcessingException e) {
								logger.catching(e);
							}
							return null;

						})

				.orElse(null);
	}
}
