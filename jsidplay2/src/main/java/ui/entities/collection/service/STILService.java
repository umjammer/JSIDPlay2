package ui.entities.collection.service;

import java.util.ArrayList;
import java.util.function.Function;

import javax.persistence.EntityManager;

import libsidutils.stil.STIL.Info;
import libsidutils.stil.STIL.STILEntry;
import libsidutils.stil.STIL.TuneEntry;
import ui.entities.collection.HVSCEntry;
import ui.entities.collection.StilEntry;

public class STILService {

	private EntityManager em;

	public STILService(EntityManager em) {
		this.em = em;
	}

	public void add(Function<String, STILEntry> stilFnct, final HVSCEntry hvscEntry) {
		STILEntry stilEntry = stilFnct.apply(hvscEntry.getPath());
		if (stilEntry != null) {
			// add tune infos
			addSTILInfo(hvscEntry, stilEntry.getInfos());
			// go through subsongs & add them as well
			for (final TuneEntry entry : stilEntry.getSubTunes()) {
				addSTILInfo(hvscEntry, entry.infos);
			}
		}
	}

	private void addSTILInfo(HVSCEntry hvscEntry, ArrayList<Info> infos) {
		for (Info info : infos) {
			StilEntry stil = new StilEntry(info);
			stil.setHvscEntry(hvscEntry);
			try {
				em.persist(stil);
			} catch (Throwable e) {
				System.err.println("Tune: " + hvscEntry.getPath());
				System.err.println(e.getMessage());
			}
			hvscEntry.getStil().add(stil);
		}
	}

	public void clear() {
		em.createQuery("DELETE from StilEntry").executeUpdate();
	}

}
