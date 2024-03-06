package ui.entities.debug.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.LogRecord;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import server.restful.common.Order;
import ui.entities.debug.DebugEntry;
import ui.entities.debug.DebugEntry_;

public class DebugService {

	private EntityManager em;

	public DebugService(EntityManager em) {
		this.em = em;
	}

	public void save(LogRecord record, Boolean tooMuchLogging) {
		try {
			if (em.isOpen()) {
				em.getTransaction().begin();

				DebugEntry debugEntry = new DebugEntry();
				debugEntry.setInstant(Instant.ofEpochMilli(record.getMillis()));
				debugEntry.setSourceClassName(record.getSourceClassName());
				debugEntry.setSourceMethodName(record.getSourceMethodName());
				debugEntry.setLevel(record.getLevel().getName());
				debugEntry.setMessage(record.getMessage());
				debugEntry.setTooMuchLogging(tooMuchLogging);
				em.persist(debugEntry);

				em.getTransaction().commit();
			}
		} catch (Exception e) {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
			throw e;
		}
	}

	public List<DebugEntry> findDebugEntries(Long instant, String sourceClassName, String sourceMethodName,
			String level, String message, int maxResults, Order order, Boolean tooMuchLogging) {
		// SELECT instant, sourceClassName, sourceMethodName, level, message FROM
		// `DebugEntry` WHERE instant >= instant AND sourceClassName LIKE
		// %sourceClassName% AND sourceMethodName LIKE %sourceMethodName% AND level LIKE
		// %level% AND message LIKE %message% AND too_much_logging=tooMuchLogging ORDER
		// BY instant ASC LIMIT maxResults
		try {
			if (em.isOpen()) {
				em.getTransaction().begin();

				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<DebugEntry> query = cb.createQuery(DebugEntry.class);
				Root<DebugEntry> root = query.from(DebugEntry.class);

				Predicate where = whereClause(instant, sourceClassName, sourceMethodName, level, message, order,
						tooMuchLogging, cb, root);
				query.select(root).where(where);

				if (order == Order.ASC) {
					query.orderBy(cb.asc(root.get(DebugEntry_.instant)));
				} else {
					query.orderBy(cb.desc(root.get(DebugEntry_.instant)));
				}
				List<DebugEntry> result = em.createQuery(query).setMaxResults(maxResults).getResultList();

				em.getTransaction().commit();
				return result;
			}
		} catch (Exception e) {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		return null;
	}

	public Long countDebugEntries(Long instant, String sourceClassName, String sourceMethodName, String level,
			String message, Order order, Boolean tooMuchLogging) {
		// SELECT count(*) FROM `DebugEntry` WHERE instant >= instant AND
		// sourceClassName LIKE %sourceClassName% AND sourceMethodName LIKE
		// %sourceMethodName% AND level LIKE %level% AND message LIKE %message%
		try {
			if (em.isOpen()) {
				em.getTransaction().begin();

				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<Long> query = cb.createQuery(Long.class);
				Root<DebugEntry> root = query.from(DebugEntry.class);

				Predicate where = whereClause(instant, sourceClassName, sourceMethodName, level, message, order,
						tooMuchLogging, cb, root);
				query.select(cb.count(root)).where(where);

				Long result = em.createQuery(query).getSingleResult();

				em.getTransaction().commit();
				return result;
			}
		} catch (Exception e) {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		return null;
	}

	private Predicate whereClause(Long instantAsEpochMillis, String sourceClassName, String sourceMethodName,
			String level, String message, Order order, Boolean tooMuchLogging, CriteriaBuilder cb, Root<DebugEntry> root) {
		Instant instant = Instant.ofEpochMilli(instantAsEpochMillis);

		List<Predicate> predicates = new ArrayList<>();

		if (order == Order.ASC) {
			predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get(DebugEntry_.instant), instant));
		} else {
			predicates.add(cb.lessThan(root.<Instant>get(DebugEntry_.instant), instant));
		}

		Optional<Predicate> sourceClassNamePredicate = !sourceClassName.isEmpty()
				? Optional.of(cb.like(root.get(DebugEntry_.sourceClassName), "%" + sourceClassName + "%"))
				: Optional.empty();
		sourceClassNamePredicate.ifPresent(predicates::add);

		Optional<Predicate> sourceMethodNamePredicate = !sourceMethodName.isEmpty()
				? Optional.of(cb.like(root.get(DebugEntry_.sourceMethodName), "%" + sourceMethodName + "%"))
				: Optional.empty();
		sourceMethodNamePredicate.ifPresent(predicates::add);

		Optional<Predicate> levelPredicate = !level.isEmpty()
				? Optional.of(cb.like(root.get(DebugEntry_.level), "%" + level + "%"))
				: Optional.empty();
		levelPredicate.ifPresent(predicates::add);

		Optional<Predicate> messagePredicate = !message.isEmpty()
				? Optional.of(cb.like(root.get(DebugEntry_.message), "%" + message + "%"))
				: Optional.empty();
		messagePredicate.ifPresent(predicates::add);
		
		if (!tooMuchLogging) {
			predicates.add(cb.equal(root.get(DebugEntry_.tooMuchLogging), tooMuchLogging));
		}
		
		return cb.and(predicates.toArray(new Predicate[predicates.size()]));
	}

}
