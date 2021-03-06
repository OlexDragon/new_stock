package irt.components.beans.jpa.pn;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class PnType {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private Long first;
	private Long last;
}
