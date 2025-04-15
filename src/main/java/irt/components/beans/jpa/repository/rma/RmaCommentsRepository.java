package irt.components.beans.jpa.repository.rma;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.rma.Comment;
import irt.components.beans.jpa.rma.RmaComment;

public interface RmaCommentsRepository extends CrudRepository<RmaComment, Long> {

	List<RmaComment> findByRmaId(Long rmaId);

	List<RmaComment> findByCommentContaining(String value, PageRequest of);

	List<RmaComment>  findByUserId(long userId);
	Optional<Comment> findTop1ByUserIdOrderByIdDesc(long userId);
}
