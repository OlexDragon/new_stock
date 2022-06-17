package irt.components.services;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import javax.persistence.AttributeConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class JSonConverter<T> implements AttributeConverter<T, String> {
	final Logger logger = LogManager.getLogger(getClass());

	private final Class<T> genericClass;

	@SuppressWarnings("unchecked")
	public JSonConverter() {
		this.genericClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	@Override
	public String convertToDatabaseColumn(T generic) {

		final ObjectMapper mapper = new ObjectMapper();

		try {

			final String valueAsString = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(generic);
			logger.debug("length: {}; {}", valueAsString.length(), valueAsString);

			return valueAsString;

		} catch (JsonProcessingException e) {
			logger.catching(e);
		}

		return null;
	}

	@Override
	public T convertToEntityAttribute(String json) {

		return Optional.ofNullable(json).filter(j->!j.isEmpty())

				.map(
						j->{

							try {

								return new ObjectMapper().readValue(j, genericClass);

							} catch (JsonProcessingException e) {
								logger.catching(e);
							}
							return null;

						})

				.orElse(null);
	}
}
