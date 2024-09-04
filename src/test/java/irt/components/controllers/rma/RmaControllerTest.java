package irt.components.controllers.rma;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;

import irt.components.beans.jpa.rma.RmaCommentWeb;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RmaControllerTest {
	private final static Logger logger = LogManager.getLogger();

	@Autowired private EntityManager		entityManager;

	@Autowired private MockMvc mockMvc;

	@Test
	void bySerialNumberTest() throws Exception {

		mockMvc.perform(
				post("/rma/search")
				.param("id", "rmaSerialNumber")
				.param("value", "150")
				.param("sortBy", "rmaOrderBySerialNumber")
				.param("rmaFilter", "ALL"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(model().attribute("rmas", hasSize(greaterThan(2))));
	}

	@Test
	void getIDsTest() throws Exception {

		final List<Long> resultList = getIDs();
		logger.error(resultList);
		assertTrue(resultList.size()==1);
	}

	protected List<Long> getIDs() {

		final CriteriaBuilder criteriaBuilder	 = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Long> criteriaQuery	 = criteriaBuilder.createQuery(Long.class);
		final Root<RmaCommentWeb> root			 = criteriaQuery.from(RmaCommentWeb.class);
		final Predicate like = criteriaBuilder.like(criteriaBuilder.upper(root.get("comment")), "%TEST%");
		criteriaQuery.where(like);
		final CriteriaQuery<Long> select = criteriaQuery.select(root.get("rmaId")).distinct(true);

		return entityManager.createQuery(select).getResultList();
	}
}
