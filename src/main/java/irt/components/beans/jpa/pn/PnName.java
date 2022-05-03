package irt.components.beans.jpa.pn;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString @EqualsAndHashCode
public class PnName {

	@Id private Long code;
	private String name;
	private String fragment;

	@JsonIgnore
	@OneToMany(mappedBy="nameCode")
	private Set<PnSubtype> pnSubtypes; 
}
