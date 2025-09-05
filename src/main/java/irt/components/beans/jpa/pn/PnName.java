package irt.components.beans.jpa.pn;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
