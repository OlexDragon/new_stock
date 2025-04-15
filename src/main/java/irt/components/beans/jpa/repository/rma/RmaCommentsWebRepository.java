package irt.components.beans.jpa.repository.rma;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.rma.Comment;
import irt.components.beans.jpa.rma.RmaCommentWeb;

public interface RmaCommentsWebRepository extends CrudRepository<RmaCommentWeb, Long> {

	List<RmaCommentWeb> findByRmaId(Long rmaId);
	List<RmaCommentWeb> findByCommentContaining(String value);
	List<RmaCommentWeb>  findByUserId(long userId);
	Optional<Comment> findTop1ByUserIdOrderByIdDesc(long userId);
}
