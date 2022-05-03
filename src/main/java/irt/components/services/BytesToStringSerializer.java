package irt.components.services;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class BytesToStringSerializer extends JsonSerializer<byte[]>{

	@Override
	public void serialize(byte[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

		gen.writeStartArray();

	    for (byte b : value) {
	        gen.writeNumber(b & 0xFF);
	    }

	    gen.writeEndArray();
	}
	
}
