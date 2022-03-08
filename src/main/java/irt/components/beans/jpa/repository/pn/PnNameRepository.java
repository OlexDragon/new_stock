package irt.components.beans.jpa.repository.pn;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.pn.PnName;

public interface PnNameRepository extends CrudRepository<PnName, Long> {

	@Query("SELECT pn FROM PnName pn WHERE pn.code>=:first AND pn.code<=:last ORDER BY pn.name")
	List<PnName> findByRange(Long first, Long last);
}
