package irt.components.beans.jpa.repository.rma;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.rma.RmaCommentWeb;

public interface RmaCommentsWebRepository extends CrudRepository<RmaCommentWeb, Long> {

	List<RmaCommentWeb> findByRmaId(Long rmaId);
	List<RmaCommentWeb> findByCommentContaining(String value);
}
