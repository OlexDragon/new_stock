package irt.components.beans;

import java.util.List;

import org.springframework.data.domain.Sort.Direction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import irt.components.beans.jpa.rma.Rma.Status;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class RmaByIDsRequest {

	private List<Long> rmaIds;
	private Status[] status;
	private Direction direction;
	private String name;
	private Integer size;
}
