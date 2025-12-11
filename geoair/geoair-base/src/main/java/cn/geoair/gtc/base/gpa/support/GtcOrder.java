package cn.geoair.gtc.base.gpa.support;

import java.io.Serializable;
import java.util.Locale;
import java.util.Optional;

import cn.geoair.gtc.base.lang.lambda.GkSerializableFunction;
import cn.geoair.gtc.base.lang.lambda.GkfLambdaMeta;
import cn.geoair.gtc.base.util.GutilLambda;


/**
 * 排序
 */
public class GtcOrder<T> implements Serializable {

	private static final long serialVersionUID = 1522511010900108987L;
	private static final boolean DEFAULT_IGNORE_CASE = false;
	private static final NullHandling DEFAULT_NULL_HANDLING = NullHandling.NATIVE;
	public static final Direction DEFAULT_DIRECTION = Direction.ASC;

	private Direction direction = DEFAULT_DIRECTION;
	private boolean ignoreCase = DEFAULT_IGNORE_CASE;
	private NullHandling nullHandling = DEFAULT_NULL_HANDLING;

	private Class<T> entityClass;
	private String property;
	private GkSerializableFunction<T, ?> propertyFun;


	/**
	 * Creates a new {@link  GtcOrder} instance. if order is {@literal null} then order defaults to
	 * {@link  GtcSort#DEFAULT_DIRECTION}
	 *
	 * @param direction can be {@literal null}, will default to {@link  GtcSort#DEFAULT_DIRECTION}
	 * @param property must not be {@literal null} or empty.
	 * @param ignoreCase true if sorting should be case insensitive. false if sorting should be case sensitive.
	 * @param nullHandling must not be {@literal null}.
	 */
	private GtcOrder(Class<T> typeClass) {
		this.entityClass = typeClass;
	}

	/**
	 * Creates a new {@link  GtcOrder} instance. Takes a single property. Direction is {@link Direction#ASC} and
	 * NullHandling {@link NullHandling#NATIVE}.
	 *
	 * @param property must not be {@literal null} or empty.
	 */
	public static<T> GtcOrder<T> asc(String property, Class<T> typeClass) {

		 GtcOrder<T> order = new GtcOrder<T>(typeClass);
		order.property = property;
		return order.with(Direction.ASC);
	}

	/**
	 * Creates a new {@link  GtcOrder} instance. Takes a single property. Direction is {@link Direction#DESC} and
	 * NullHandling {@link NullHandling#NATIVE}.
	 *
	 * @param property must not be {@literal null} or empty.
	 */
	public static<T> GtcOrder<T> desc(String property, Class<T> typeClass) {

		 GtcOrder<T> order = new GtcOrder<T>(typeClass);
		order.property = property;
		return order.with(Direction.DESC);
//
//		return new  gtcOrder<T>(typeClass).with(Direction.DESC);
	}

	public static <T> GtcOrder<T> asc(GkSerializableFunction<T, ?> propertyFunction) {
		 GtcOrder<T> order = new GtcOrder<T>((Class<T>)null).with(Direction.ASC);
		order.propertyFun = propertyFunction;
		return order;
	}

	public static <T> GtcOrder<T> desc(GkSerializableFunction<T, ?> propertyFunction) {
		 GtcOrder<T> order =  new GtcOrder<T>((Class<T>)null).with(Direction.DESC);
		order.propertyFun = propertyFunction;
		return order;
	}


	/**
	 * Returns the order the property shall be sorted for.
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Class<T> getEntityClass() {
		if(entityClass == null) {
			GkfLambdaMeta lm = GutilLambda.extract(propertyFun);
			entityClass = (Class<T>)lm.getInstantiatedClass();
		}
		return entityClass;
	}
	/**
	 * Returns the order the property shall be sorted for.
	 *
	 * @return
	 */
	public Direction getDirection() {
		return direction;
	}

	/**
	 * Returns the property to order for.
	 *
	 * @return
	 */
	public String getProperty() {
		return property;
	}

	public GkSerializableFunction<T, ?> getPropertyFun() {
		return propertyFun;
	}

	/**
	 * Returns whether sorting for this property shall be ascending.
	 *
	 * @return
	 */
	public boolean isAscending() {
		return this.direction.isAscending();
	}

	/**
	 * Returns whether sorting for this property shall be descending.
	 *
	 * @return
	 */
	public boolean isDescending() {
		return this.direction.isDescending();
	}

	/**
	 * Returns whether or not the sort will be case sensitive.
	 *
	 * @return
	 */
	public boolean isIgnoreCase() {
		return ignoreCase;
	}

	/**
	 * Returns the used {@link NullHandling} hint, which can but may not be respected by the used datastore.
	 *
	 * @return
	 */
	public NullHandling getNullHandling() {
		return nullHandling;
	}

	/**
	 * Returns a new {@link  GtcOrder} with the given {@link Direction}.
	 *
	 * @param direction
	 * @return
	 */
	public GtcOrder<T> with(Direction direction) {
		this.direction = direction;
		return this;
	}


	/**
	 * Returns a new {@link  GtcOrder} with case insensitive sorting enabled.
	 *
	 * @return
	 */
	public GtcOrder<T> ignoreCase() {
		this.ignoreCase = true;
		return this;
	}

	/**
	 * Returns a {@link  GtcOrder} with the given {@link NullHandling}.
	 *
	 * @param nullHandling can be {@literal null}.
	 * @return
	 */
	public GtcOrder<T> with(NullHandling nullHandling) {
		this.nullHandling = nullHandling;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		int result = 17;

		result = 31 * result + direction.hashCode();
		result = 31 * result + property.hashCode();
		result = 31 * result + (ignoreCase ? 1 : 0);
		result = 31 * result + nullHandling.hashCode();

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof GtcOrder)) {
			return false;
		}

		 GtcOrder<?> that = (GtcOrder<?>) obj;

		return this.entityClass.equals(that.entityClass) && this.direction.equals(that.direction) && this.property.equals(that.property)
				&& this.ignoreCase == that.ignoreCase && this.nullHandling.equals(that.nullHandling);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		String result = String.format("%s: %s", property, direction);

		if (!NullHandling.NATIVE.equals(nullHandling)) {
			result += ", " + nullHandling;
		}

		if (ignoreCase) {
			result += ", ignoring case";
		}

		return result;
	}




	/**
	 * Enumeration for sort directions.
	 *
	 */
	public static enum Direction {

		ASC, DESC;

		/**
		 * Returns whether the direction is ascending.
		 *
		 * @return
		 */
		public boolean isAscending() {
			return this.equals(ASC);
		}

		/**
		 * Returns whether the direction is descending.
		 *
		 * @return
		 */
		public boolean isDescending() {
			return this.equals(DESC);
		}


		public String value() {
			return this.name();
		}

		/**
		 * Returns the {@link Direction} enum for the given {@link String} value.
		 *
		 * @param value
		 * @throws IllegalArgumentException in case the given value cannot be parsed into an enum value.
		 * @return
		 */
		public static Direction fromString(String value) {

			try {
				return Direction.valueOf(value.toUpperCase(Locale.US));
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format(
						"Invalid value '%s' for orders given! Has to be either 'desc' or 'asc' (case insensitive).", value), e);
			}
		}

		/**
		 * Returns the {@link Direction} enum for the given {@link String} or null if it cannot be parsed into an enum
		 * value.
		 *
		 * @param value
		 * @return
		 */
		public static Optional<Direction> fromOptionalString(String value) {

			try {
				return Optional.of(fromString(value));
			} catch (IllegalArgumentException e) {
				return Optional.empty();
			}
		}
	}
	/**
	 * Enumeration for null handling hints that can be used in {@link  GtcOrder} expressions.
	 *
	 */
	public static enum NullHandling {

		/**
		 * Lets the data store decide what to do with nulls.
		 */
		NATIVE,

		/**
		 * A hint to the used data store to order entries with null values before non null entries.
		 */
		NULLS_FIRST,

		/**
		 * A hint to the used data store to order entries with null values after non null entries.
		 */
		NULLS_LAST;
	}
}
