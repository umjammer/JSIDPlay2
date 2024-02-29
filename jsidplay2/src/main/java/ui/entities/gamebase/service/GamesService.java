package ui.entities.gamebase.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import ui.entities.gamebase.Games;
import ui.entities.gamebase.Games_;

public class GamesService {
	private EntityManager em;

	public GamesService(EntityManager em) {
		this.em = em;
	}

	public List<Games> select(char firstLetter) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Games> query = cb.createQuery(Games.class);
		Root<Games> games = query.from(Games.class);
		Path<String> name = games.get(Games_.name);
		final Predicate predicate;
		if (Character.isLetter(firstLetter)) {
			predicate = cb.like(name, firstLetter + "%");
		} else {
			// first character matches everything EXCEPT letters
			predicate = cb.and(cb.not(cb.like(name, "a%")), cb.not(cb.like(name, "b%")), cb.not(cb.like(name, "c%")),
					cb.not(cb.like(name, "d%")), cb.not(cb.like(name, "e%")), cb.not(cb.like(name, "f%")),
					cb.not(cb.like(name, "g%")), cb.not(cb.like(name, "h%")), cb.not(cb.like(name, "i%")),
					cb.not(cb.like(name, "j%")), cb.not(cb.like(name, "k%")), cb.not(cb.like(name, "l%")),
					cb.not(cb.like(name, "m%")), cb.not(cb.like(name, "n%")), cb.not(cb.like(name, "o%")),
					cb.not(cb.like(name, "p%")), cb.not(cb.like(name, "q%")), cb.not(cb.like(name, "r%")),
					cb.not(cb.like(name, "s%")), cb.not(cb.like(name, "t%")), cb.not(cb.like(name, "u%")),
					cb.not(cb.like(name, "v%")), cb.not(cb.like(name, "w%")), cb.not(cb.like(name, "x%")),
					cb.not(cb.like(name, "y%")), cb.not(cb.like(name, "z%")));
		}
		query.where(predicate).orderBy(cb.asc(name)).select(games);
		return em.createQuery(query).getResultList();
	}
}
