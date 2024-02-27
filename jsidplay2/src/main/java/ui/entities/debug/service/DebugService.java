package ui.entities.debug.service;

import java.time.Instant;
import java.util.List;
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

	public void save(LogRecord record) {
		try {
			if (em.isOpen()) {
				em.getTransaction().begin();

				DebugEntry debugEntry = new DebugEntry();
				debugEntry.setInstant(Instant.ofEpochMilli(record.getMillis()));
				debugEntry.setSourceClassName(record.getSourceClassName());
				debugEntry.setSourceMethodName(record.getSourceMethodName());
				debugEntry.setLevel(record.getLevel().getName());
				debugEntry.setMessage(record.getMessage());
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
			String level, String message, int maxResults, Order order) {
		// SELECT instant, sourceClassName, sourceMethodName, level, message FROM
		// `DebugEntry` WHERE instant >= instant AND sourceClassName LIKE
		// %sourceClassName% AND sourceMethodName LIKE %sourceMethodName% AND level LIKE
		// %level% AND message LIKE %message% ORDER BY instant ASC LIMIT maxResults
		try {
			if (em.isOpen()) {
				em.getTransaction().begin();

				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<DebugEntry> query = cb.createQuery(DebugEntry.class);
				Root<DebugEntry> root = query.from(DebugEntry.class);

				Predicate instantPredicate;
				if (order == Order.ASC) {
					instantPredicate = cb.greaterThanOrEqualTo(root.<Instant>get(DebugEntry_.instant),
							Instant.ofEpochMilli(instant));
				} else {
					instantPredicate = cb.lessThan(root.<Instant>get(DebugEntry_.instant),
							Instant.ofEpochMilli(instant));
				}
				Predicate sourceClassNamePredicate = cb.like(root.get(DebugEntry_.sourceClassName),
						"%" + sourceClassName + "%");

				Predicate sourceMethodNamePredicate = cb.like(root.get(DebugEntry_.sourceMethodName),
						"%" + sourceMethodName + "%");

				Predicate levelPredicate = cb.like(root.get(DebugEntry_.level), "%" + level + "%");

				Predicate messagePredicate = cb.like(root.get(DebugEntry_.message), "%" + message + "%");

				query.select(root).where(cb.and(instantPredicate, sourceClassNamePredicate, sourceMethodNamePredicate,
						levelPredicate, messagePredicate));

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

}
