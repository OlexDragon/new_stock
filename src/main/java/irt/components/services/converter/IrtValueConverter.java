package irt.components.services.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import irt.components.beans.irt.IrtValue;
import jakarta.persistence.AttributeConverter;

public class IrtValueConverter implements  AttributeConverter<IrtValue, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
	public String convertToDatabaseColumn(IrtValue irtValue) {
	    if (irtValue == null) return null;
        try {
            return objectMapper.writeValueAsString(irtValue);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error serializing object to JSON", e);
        }
	}

	@Override
	public IrtValue convertToEntityAttribute(String dbData) {
	       if (dbData == null || dbData.isBlank()) return null;
	        try {
	            return objectMapper.readValue(dbData, IrtValue.class);
	        } catch (JsonProcessingException e) {
	            throw new IllegalArgumentException("Error deserializing JSON to object", e);
	        }
	}

}
