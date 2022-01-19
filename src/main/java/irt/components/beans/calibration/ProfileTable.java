package irt.components.beans.calibration;

import java.util.List;

import irt.components.beans.calibration.update.TableValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor @Getter @Setter @ToString
public class ProfileTable {

	private final ProfileTableTypes type;
	private final String name;

	private String index;

	public String toString(List<TableValue> values) {

		switch(type) {

		case NEW:
			return getNewTable(values);

		case OLD:
			return getOldTable(values);

		case KA:
		default:
			return null;
		}
	}

	private String getNewTable(List<TableValue> values) {

		StringBuilder sb = new StringBuilder();
		sb.append(name).append("lut-size ").append(index).append(' ').append(values.size()).append("\r\n");
		values.forEach(v->sb.append("lut-entry ").append(index).append(' ').append(v.getInput()).append(' ').append(v.getOutput()).append("\r\n"));
		sb.append("lut-ref ").append(index).append(' ').append(name).append("\r\n");
		return sb.toString();
	}

	private String getOldTable(List<TableValue> values) {

		StringBuilder sb = new StringBuilder();
		sb.append(name).append("-lut-size ").append(values.size()).append("\r\n");
		values.forEach(v->sb.append(name).append("-lut-entry ").append(v.getInput()).append(' ').append(v.getOutput()).append("\r\n"));

		return sb.toString();
	}

	public boolean match(String line) {

		switch(type) {

		case NEW:
			return getNewTableMatch(line);

		case OLD:
			return getOldTableMatch(line);

		case KA:
		default:
			return false;
		}
	}

	private boolean getNewTableMatch(String line) {

		boolean isLut;
		if(line.startsWith("#"))
			isLut =  line.startsWith("#" + "lut-");
		else
			isLut = line.startsWith("lut-");

		if(isLut) {
			final String index = line.split("\\s+", 3)[1];
			return this.index.equals(index);
		}

		return false;
	}

	private boolean getOldTableMatch(String line) {

		if(line.startsWith("#"))
			return line.startsWith("#" + name + "-lut-");

		return line.startsWith(name + "-lut-");
	}
}
