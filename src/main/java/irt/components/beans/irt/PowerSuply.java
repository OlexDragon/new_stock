package irt.components.beans.irt;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class PowerSuply {

	private String name;
	private Object data;

	@JsonProperty
	public List<IrtData> getPowerSuplyData() {

		if(data==null)
			return null;

		final List<IrtData> datas = new ArrayList<>();

		final String string = data.toString();
		int start = string.indexOf("<pre>") + 5;

		if(start<0)
			return null;

		int stop = string.indexOf("</pre>");
		try(Scanner scanner = new Scanner(string.substring(start, stop))){

			while(scanner.hasNextLine()) {
				final String nextLine = scanner.nextLine();
				datas.add(new IrtData(nextLine));
			}
		}
		return datas.stream().filter(d->d.getValue()!=null).collect(Collectors.toList());
	}
}
