package irt.components.services.converter;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import irt.components.beans.irt.IrtValue;

public class IrtValueConverter extends JsonDeserializer<IrtValue> {

	@Override
	public IrtValue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
		return new IrtValue(p.getValueAsString());
	}

}
