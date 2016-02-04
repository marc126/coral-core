package org.coral.core.persistence.postgresql;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link UserType} that persists objects as JSONB.
 * <p>
 * Unlike the default JPA object mapping, {@code JSONBUserType} can also be used
 * for properties that do not implement {@link Serializable}.
 * <p>
 * Users intending to use this type for mutable non-<code>Collection</code>
 * objects should override {@link #deepCopyValue(Object)} to correctly return a
 * <u>copy</u> of the object.
 */
public class JSONBUserType extends CollectionUserType implements
    ParameterizedType {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String JSONB_TYPE = "jsonb";
  public static final String CLASS = "CLASS";
  private Class returnedClass;

  @Override
  public Class<Object> returnedClass() {
		return Object.class;
  }

  @Override
  public int[] sqlTypes() {
    return new int[]{Types.JAVA_OBJECT};
  }

  @Override
  public Object nullSafeGet(ResultSet resultSet, String[] names,
      SessionImplementor session, Object owner)
      throws HibernateException, SQLException {
    try {
      final String json = resultSet.getString(names[0]);
      return json == null
          ? null
          : MAPPER.readValue(json, returnedClass);
    } catch (IOException ex) {
      throw new HibernateException(ex);
    }
  }

  @Override
  public void nullSafeSet(PreparedStatement st, Object value, int index,
      SessionImplementor session) throws HibernateException, SQLException {
    try {
      final String json = value == null
          ? null
          : MAPPER.writeValueAsString(value);
      // otherwise PostgreSQL won't recognize the type
      PGobject pgo = new PGobject();
      pgo.setType(JSONB_TYPE);
      pgo.setValue(json);
      st.setObject(index, pgo);
    } catch (JsonProcessingException ex) {
      throw new HibernateException(ex);
    }
  }

  @Override
  protected Object deepCopyValue(Object value) {
    return value;
  }

  @Override
  public void setParameterValues(Properties parameters) {
    final String clazz = (String) parameters.get(CLASS);
    try {
      returnedClass = ReflectHelper.classForName(clazz,JSONBUserType.class);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Class: " + clazz
          + " is not a known class type.");
    }
  }

}