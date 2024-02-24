package ui.entities.debug.service;

import java.time.Instant;
import java.util.logging.LogRecord;

import javax.persistence.EntityManager;

import ui.entities.debug.DebugEntry;

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

}
