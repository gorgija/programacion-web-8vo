package py.com.pg.webstock.gwt.server.service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Projections;

import py.com.pg.webstock.entities.BaseEntity;
import py.com.pg.webstock.gwt.client.service.BaseDAOService;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public abstract class BaseDAOServiceImpl<T extends BaseEntity> extends
		RemoteServiceServlet implements BaseDAOService<T> {

	private static final long serialVersionUID = -432284516858356051L;

	@PersistenceContext(unitName = "WebStockJPA")
	EntityManager em;

	@PersistenceContext(unitName = "WebStockJPA")
	Session session;

	@Resource
	UserTransaction ut;

	@Override
	public T get(int id) {
		return em.find(getClase(), id);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> getEntidades() {
		return filter(session.createCriteria(getClase()).list());
	}

	@Override
	public T add(T entidad) {
		try {
			ut.begin();
			em.persist(entidad);
			System.out.println("Creada entidad con id " + entidad.getId());
			ut.commit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return entidad;
	}

	@Override
	public T remove(T entidad) {
		try {
			ut.begin();
			em.remove(em.merge(entidad));
			System.out.println("Eliminada entidad con id " + entidad.getId());
			ut.commit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return entidad;
	}

	@Override
	public T remove(int id) {
		T entidad = get(id);
		try {
			ut.begin();
			em.persist(entidad);
			System.out.println("Eliminada entidad con id " + entidad.getId());
			ut.commit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return entidad;
	}

	@Override
	public T update(T entidad) {
		try {
			ut.begin();
			em.merge(entidad);
			System.out.println("Modificada entidad con id " + entidad.getId());
			ut.commit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return entidad;
	}

	@Override
	public Long getCount() {
		return (Long) session.createCriteria(getClase())
				.setProjection(Projections.rowCount()).uniqueResult();
	}

	@SuppressWarnings("unchecked")
	public Class<T> getClase() {
		ParameterizedType type = (ParameterizedType) this.getClass()
				.getGenericSuperclass();

		return (Class<T>) type.getActualTypeArguments()[0];
	}

	@SuppressWarnings("unchecked")
	public List<T> getByExample(T example) {
		return filter(session.createCriteria(getClase())
				.add(Example.create(example)).list());
	}

	/**
	 * Ve para borrar las listas y cosas asi que no pueden ser enviadas al
	 * cliente pues no pueden ser serializadas, pues son lazys! tipo el examen
	 * 
	 * @param lista
	 * @return
	 */
	public List<T> filter(List<T> lista) {
		try {
			ArrayList<Field> aCerar = null;
			ArrayList<Field> aLimpiar = null;
			for (T entidad : lista) {
				if (aCerar == null) {
					aCerar = new ArrayList<Field>();
					aLimpiar = new ArrayList<Field>();
					Class<T> clase = getClase();
					// esto esta mal, no retorna lso fields de entidades
					// heredadas.
					for (Field f : clase.getDeclaredFields()) {
						if (f.getType() == List.class) {
							f.setAccessible(true);
							aCerar.add(f);
						}
						if (BaseEntity.class.isAssignableFrom(f.getType())) {
							f.setAccessible(true);
							aLimpiar.add(f);
						}
					}
				}
				for (Field f : aCerar) {
					f.set(entidad, null);
				}
				for (Field f : aLimpiar) {
					f.set(entidad, crearInstanciaLimpia(f.get(entidad)));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lista;
	}

	public Object crearInstanciaLimpia(Object objeto) {
		if (objeto == null)
			return null;
		try {
			Class<?> clase = (Class<?>) objeto.getClass();
			Object nuevo = clase.newInstance();
			for (Field f : clase.getDeclaredFields()) {
				// si es estatico omito
				if (Modifier.isStatic(f.getModifiers()))
					continue;
				// si es una lista omito
				if (f.getType() == List.class)
					continue;
				// si es otra entidad omito
				if (BaseEntity.class.isAssignableFrom(f.getType()))
					continue;
				f.setAccessible(true);
				Object o = f.get(objeto);
				f.set(nuevo, o);
			}
			return nuevo;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return objeto;
	}
}
