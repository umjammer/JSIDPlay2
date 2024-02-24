package ui.entities.debug.service;

import java.io.IOException;
import java.time.Instant;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import ui.entities.debug.DebugEntry;

public class DebugService extends Handler {

	private static final ThreadLocal<EntityManager> THREAD_LOCAL_ENTITY_MANAGER = new ThreadLocal<>();

	private EntityManagerFactory entityManagerFactory;

	public DebugService(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public void publish(LogRecord record) {

		EntityManager em = null;
		try {
			em = getEntityManager();
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
			Logger.getAnonymousLogger().severe("Error in DebugService publish: " + e.getMessage());
		} finally {
			freeEntityManager();
		}
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() throws SecurityException {
	}

	private EntityManager getEntityManager() throws IOException {
		if (entityManagerFactory == null) {
			throw new IOException("Database required, please specify command line parameters!");
		}
		EntityManager em = THREAD_LOCAL_ENTITY_MANAGER.get();

		if (em == null) {
			em = entityManagerFactory.createEntityManager();
			THREAD_LOCAL_ENTITY_MANAGER.set(em);
		}
		return em;
	}

	private void freeEntityManager() {
		EntityManager em = THREAD_LOCAL_ENTITY_MANAGER.get();

		if (em != null) {
			em.clear();
		}
	}

}
