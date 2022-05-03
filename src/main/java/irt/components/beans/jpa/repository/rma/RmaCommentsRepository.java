package irt.components.beans.jpa.repository.rma;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.rma.RmaComment;

public interface RmaCommentsRepository extends CrudRepository<RmaComment, Long> {

	List<RmaComment> findByRmaId(Long rmaId);
}
