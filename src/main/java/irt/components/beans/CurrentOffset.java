package irt.components.beans;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class CurrentOffset {

	private final String path;
	private final List<String> offsets = new ArrayList<>();

	public CurrentOffset(@JsonProperty("path") String path) {
		this.path = path;
	}
}
