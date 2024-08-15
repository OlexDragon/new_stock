package irt.components.services.converter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import javax.persistence.AttributeConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class JSonConverter<T> implements AttributeConverter<T, String> {
	final Logger logger = LogManager.getLogger(getClass());

	private Class<T> genericClass;
	private TypeReference<T> typeReference;

	@SuppressWarnings("unchecked")
	public JSonConverter() {

		Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

		if(type instanceof Class) 
			this.genericClass = (Class<T>) type;
		else {
			typeReference = new TypeReference<T>(){

				@Override
				public Type getType() {
					return type;
				}};
		}
	}

	@Override
	public String convertToDatabaseColumn(T generic) {

		try {

			return  new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(generic);

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

							return Optional.ofNullable(genericClass).map(cl->{
								try {
									return new ObjectMapper().readValue(j, cl);
								} catch (JsonProcessingException e) {
									logger.catching(e);
									return null;
								}
							})
							.orElseGet(()->{
								try {
									return new ObjectMapper().readValue(j, typeReference);
								} catch (JsonProcessingException e) {
									logger.catching(e);
									return null;
								}
							});
						})
				.orElse(null);
	}
}
