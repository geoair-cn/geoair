package cn.geoair.base.data.tuples;

import java.util.Collection;
import java.util.Iterator;

import cn.geoair.base.data.tuples.valueintf.GkiValue0;
import cn.geoair.base.data.tuples.valueintf.GkiValue1;
import cn.geoair.base.data.tuples.valueintf.GkiValue2;
import cn.geoair.base.data.tuples.valueintf.GkiValue3;
import cn.geoair.base.data.tuples.valueintf.GkiValue4;
import cn.geoair.base.data.tuples.valueintf.GkiValue5;
import cn.geoair.base.data.tuples.valueintf.GkiValue6;
import cn.geoair.base.data.tuples.valueintf.GkiValue7;

/**
 * <p>
 * A tuple of eight elements.
 * </p>
 *
 *
 */
public final class GkOctet<A, B, C, D, E, F, G, H> extends GkTuple implements GkiValue0<A>, GkiValue1<B>, GkiValue2<C>,
		GkiValue3<D>, GkiValue4<E>, GkiValue5<F>, GkiValue6<G>, GkiValue7<H> {

	private static final long serialVersionUID = -1187955276020306879L;

	private static final int SIZE = 8;

	private final A val0;

	private final B val1;

	private final C val2;

	private final D val3;

	private final E val4;

	private final F val5;

	private final G val6;

	private final H val7;

	public static <A, B, C, D, E, F, G, H> GkOctet<A, B, C, D, E, F, G, H> with(final A value0, final B value1,
			final C value2, final D value3, final E value4, final F value5, final G value6, final H value7) {
		return new GkOctet<A, B, C, D, E, F, G, H>(value0, value1, value2, value3, value4, value5, value6, value7);
	}

	/**
	 * <p>
	 * Create tuple from array. Array has to have exactly eight elements.
	 * </p>
	 * @param <X> the array component type
	 * @param array the array to be converted to a tuple
	 * @return the tuple
	 */
	public static <X> GkOctet<X, X, X, X, X, X, X, X> fromArray(final X[] array) {
		if (array == null) {
			throw new IllegalArgumentException("Array cannot be null");
		}
		if (array.length != 8) {
			throw new IllegalArgumentException(
					"Array must have exactly 8 elements in order to create an Octet. Size is " + array.length);
		}
		return new GkOctet<X, X, X, X, X, X, X, X>(array[0], array[1], array[2], array[3], array[4], array[5], array[6],
				array[7]);
	}

	/**
	 * <p>
	 * Create tuple from collection. Collection has to have exactly eight elements.
	 * </p>
	 * @param <X> the collection component type
	 * @param collection the collection to be converted to a tuple
	 * @return the tuple
	 */
	public static <X> GkOctet<X, X, X, X, X, X, X, X> fromCollection(final Collection<X> collection) {
		return fromIterable(collection);
	}

	/**
	 * <p>
	 * Create tuple from iterable. Iterable has to have exactly eight elements.
	 * </p>
	 * @param <X> the iterable component type
	 * @param iterable the iterable to be converted to a tuple
	 * @return the tuple
	 */
	public static <X> GkOctet<X, X, X, X, X, X, X, X> fromIterable(final Iterable<X> iterable) {
		return fromIterable(iterable, 0, true);
	}

	/**
	 * <p>
	 * Create tuple from iterable, starting from the specified index. Iterable can have
	 * more (or less) elements than the tuple to be created.
	 * </p>
	 * @param <X> the iterable component type
	 * @param iterable the iterable to be converted to a tuple
	 * @return the tuple
	 */
	public static <X> GkOctet<X, X, X, X, X, X, X, X> fromIterable(final Iterable<X> iterable, int index) {
		return fromIterable(iterable, index, false);
	}

	private static <X> GkOctet<X, X, X, X, X, X, X, X> fromIterable(final Iterable<X> iterable, int index,
			final boolean exactSize) {

		if (iterable == null) {
			throw new IllegalArgumentException("Iterable cannot be null");
		}

		boolean tooFewElements = false;

		X element0 = null;
		X element1 = null;
		X element2 = null;
		X element3 = null;
		X element4 = null;
		X element5 = null;
		X element6 = null;
		X element7 = null;

		final Iterator<X> iter = iterable.iterator();

		int i = 0;
		while (i < index) {
			if (iter.hasNext()) {
				iter.next();
			}
			else {
				tooFewElements = true;
			}
			i++;
		}

		if (iter.hasNext()) {
			element0 = iter.next();
		}
		else {
			tooFewElements = true;
		}

		if (iter.hasNext()) {
			element1 = iter.next();
		}
		else {
			tooFewElements = true;
		}

		if (iter.hasNext()) {
			element2 = iter.next();
		}
		else {
			tooFewElements = true;
		}

		if (iter.hasNext()) {
			element3 = iter.next();
		}
		else {
			tooFewElements = true;
		}

		if (iter.hasNext()) {
			element4 = iter.next();
		}
		else {
			tooFewElements = true;
		}

		if (iter.hasNext()) {
			element5 = iter.next();
		}
		else {
			tooFewElements = true;
		}

		if (iter.hasNext()) {
			element6 = iter.next();
		}
		else {
			tooFewElements = true;
		}

		if (iter.hasNext()) {
			element7 = iter.next();
		}
		else {
			tooFewElements = true;
		}

		if (tooFewElements && exactSize) {
			throw new IllegalArgumentException("Not enough elements for creating an Octet (8 needed)");
		}

		if (iter.hasNext() && exactSize) {
			throw new IllegalArgumentException(
					"Iterable must have exactly 8 available elements in order to create an Octet.");
		}

		return new GkOctet<X, X, X, X, X, X, X, X>(element0, element1, element2, element3, element4, element5, element6,
				element7);

	}

	public GkOctet(final A value0, final B value1, final C value2, final D value3, final E value4, final F value5,
			final G value6, final H value7) {
		super(value0, value1, value2, value3, value4, value5, value6, value7);
		this.val0 = value0;
		this.val1 = value1;
		this.val2 = value2;
		this.val3 = value3;
		this.val4 = value4;
		this.val5 = value5;
		this.val6 = value6;
		this.val7 = value7;
	}

	public A getValue0() {
		return this.val0;
	}

	public B getValue1() {
		return this.val1;
	}

	public C getValue2() {
		return this.val2;
	}

	public D getValue3() {
		return this.val3;
	}

	public E getValue4() {
		return this.val4;
	}

	public F getValue5() {
		return this.val5;
	}

	public G getValue6() {
		return this.val6;
	}

	public H getValue7() {
		return this.val7;
	}

	@Override
	public int getSize() {
		return SIZE;
	}

	public <X0> GkEnnead<X0, A, B, C, D, E, F, G, H> addAt0(final X0 value0) {
		return new GkEnnead<X0, A, B, C, D, E, F, G, H>(value0, this.val0, this.val1, this.val2, this.val3, this.val4,
				this.val5, this.val6, this.val7);
	}

	public <X0> GkEnnead<A, X0, B, C, D, E, F, G, H> addAt1(final X0 value0) {
		return new GkEnnead<A, X0, B, C, D, E, F, G, H>(this.val0, value0, this.val1, this.val2, this.val3, this.val4,
				this.val5, this.val6, this.val7);
	}

	public <X0> GkEnnead<A, B, X0, C, D, E, F, G, H> addAt2(final X0 value0) {
		return new GkEnnead<A, B, X0, C, D, E, F, G, H>(this.val0, this.val1, value0, this.val2, this.val3, this.val4,
				this.val5, this.val6, this.val7);
	}

	public <X0> GkEnnead<A, B, C, X0, D, E, F, G, H> addAt3(final X0 value0) {
		return new GkEnnead<A, B, C, X0, D, E, F, G, H>(this.val0, this.val1, this.val2, value0, this.val3, this.val4,
				this.val5, this.val6, this.val7);
	}

	public <X0> GkEnnead<A, B, C, D, X0, E, F, G, H> addAt4(final X0 value0) {
		return new GkEnnead<A, B, C, D, X0, E, F, G, H>(this.val0, this.val1, this.val2, this.val3, value0, this.val4,
				this.val5, this.val6, this.val7);
	}

	public <X0> GkEnnead<A, B, C, D, E, X0, F, G, H> addAt5(final X0 value0) {
		return new GkEnnead<A, B, C, D, E, X0, F, G, H>(this.val0, this.val1, this.val2, this.val3, this.val4, value0,
				this.val5, this.val6, this.val7);
	}

	public <X0> GkEnnead<A, B, C, D, E, F, X0, G, H> addAt6(final X0 value0) {
		return new GkEnnead<A, B, C, D, E, F, X0, G, H>(this.val0, this.val1, this.val2, this.val3, this.val4,
				this.val5, value0, this.val6, this.val7);
	}

	public <X0> GkEnnead<A, B, C, D, E, F, G, X0, H> addAt7(final X0 value0) {
		return new GkEnnead<A, B, C, D, E, F, G, X0, H>(this.val0, this.val1, this.val2, this.val3, this.val4,
				this.val5, this.val6, value0, this.val7);
	}

	public <X0> GkEnnead<A, B, C, D, E, F, G, H, X0> addAt8(final X0 value0) {
		return new GkEnnead<A, B, C, D, E, F, G, H, X0>(this.val0, this.val1, this.val2, this.val3, this.val4,
				this.val5, this.val6, this.val7, value0);
	}

	public <X0, X1> GkDecade<X0, X1, A, B, C, D, E, F, G, H> addAt0(final X0 value0, final X1 value1) {
		return new GkDecade<X0, X1, A, B, C, D, E, F, G, H>(value0, value1, this.val0, this.val1, this.val2, this.val3,
				this.val4, this.val5, this.val6, this.val7);
	}

	public <X0, X1> GkDecade<A, X0, X1, B, C, D, E, F, G, H> addAt1(final X0 value0, final X1 value1) {
		return new GkDecade<A, X0, X1, B, C, D, E, F, G, H>(this.val0, value0, value1, this.val1, this.val2, this.val3,
				this.val4, this.val5, this.val6, this.val7);
	}

	public <X0, X1> GkDecade<A, B, X0, X1, C, D, E, F, G, H> addAt2(final X0 value0, final X1 value1) {
		return new GkDecade<A, B, X0, X1, C, D, E, F, G, H>(this.val0, this.val1, value0, value1, this.val2, this.val3,
				this.val4, this.val5, this.val6, this.val7);
	}

	public <X0, X1> GkDecade<A, B, C, X0, X1, D, E, F, G, H> addAt3(final X0 value0, final X1 value1) {
		return new GkDecade<A, B, C, X0, X1, D, E, F, G, H>(this.val0, this.val1, this.val2, value0, value1, this.val3,
				this.val4, this.val5, this.val6, this.val7);
	}

	public <X0, X1> GkDecade<A, B, C, D, X0, X1, E, F, G, H> addAt4(final X0 value0, final X1 value1) {
		return new GkDecade<A, B, C, D, X0, X1, E, F, G, H>(this.val0, this.val1, this.val2, this.val3, value0, value1,
				this.val4, this.val5, this.val6, this.val7);
	}

	public <X0, X1> GkDecade<A, B, C, D, E, X0, X1, F, G, H> addAt5(final X0 value0, final X1 value1) {
		return new GkDecade<A, B, C, D, E, X0, X1, F, G, H>(this.val0, this.val1, this.val2, this.val3, this.val4,
				value0, value1, this.val5, this.val6, this.val7);
	}

	public <X0, X1> GkDecade<A, B, C, D, E, F, X0, X1, G, H> addAt6(final X0 value0, final X1 value1) {
		return new GkDecade<A, B, C, D, E, F, X0, X1, G, H>(this.val0, this.val1, this.val2, this.val3, this.val4,
				this.val5, value0, value1, this.val6, this.val7);
	}

	public <X0, X1> GkDecade<A, B, C, D, E, F, G, X0, X1, H> addAt7(final X0 value0, final X1 value1) {
		return new GkDecade<A, B, C, D, E, F, G, X0, X1, H>(this.val0, this.val1, this.val2, this.val3, this.val4,
				this.val5, this.val6, value0, value1, this.val7);
	}

	public <X0, X1> GkDecade<A, B, C, D, E, F, G, H, X0, X1> addAt8(final X0 value0, final X1 value1) {
		return new GkDecade<A, B, C, D, E, F, G, H, X0, X1>(this.val0, this.val1, this.val2, this.val3, this.val4,
				this.val5, this.val6, this.val7, value0, value1);
	}

	public <X0> GkEnnead<X0, A, B, C, D, E, F, G, H> addAt0(final GkUnit<X0> tuple) {
		return addAt0(tuple.getValue0());
	}

	public <X0> GkEnnead<A, X0, B, C, D, E, F, G, H> addAt1(final GkUnit<X0> tuple) {
		return addAt1(tuple.getValue0());
	}

	public <X0> GkEnnead<A, B, X0, C, D, E, F, G, H> addAt2(final GkUnit<X0> tuple) {
		return addAt2(tuple.getValue0());
	}

	public <X0> GkEnnead<A, B, C, X0, D, E, F, G, H> addAt3(final GkUnit<X0> tuple) {
		return addAt3(tuple.getValue0());
	}

	public <X0> GkEnnead<A, B, C, D, X0, E, F, G, H> addAt4(final GkUnit<X0> tuple) {
		return addAt4(tuple.getValue0());
	}

	public <X0> GkEnnead<A, B, C, D, E, X0, F, G, H> addAt5(final GkUnit<X0> tuple) {
		return addAt5(tuple.getValue0());
	}

	public <X0> GkEnnead<A, B, C, D, E, F, X0, G, H> addAt6(final GkUnit<X0> tuple) {
		return addAt6(tuple.getValue0());
	}

	public <X0> GkEnnead<A, B, C, D, E, F, G, X0, H> addAt7(final GkUnit<X0> tuple) {
		return addAt7(tuple.getValue0());
	}

	public <X0> GkEnnead<A, B, C, D, E, F, G, H, X0> addAt8(final GkUnit<X0> tuple) {
		return addAt8(tuple.getValue0());
	}

	public <X0, X1> GkDecade<X0, X1, A, B, C, D, E, F, G, H> addAt0(final GkPair<X0, X1> tuple) {
		return addAt0(tuple.getValue0(), tuple.getValue1());
	}

	public <X0, X1> GkDecade<A, X0, X1, B, C, D, E, F, G, H> addAt1(final GkPair<X0, X1> tuple) {
		return addAt1(tuple.getValue0(), tuple.getValue1());
	}

	public <X0, X1> GkDecade<A, B, X0, X1, C, D, E, F, G, H> addAt2(final GkPair<X0, X1> tuple) {
		return addAt2(tuple.getValue0(), tuple.getValue1());
	}

	public <X0, X1> GkDecade<A, B, C, X0, X1, D, E, F, G, H> addAt3(final GkPair<X0, X1> tuple) {
		return addAt3(tuple.getValue0(), tuple.getValue1());
	}

	public <X0, X1> GkDecade<A, B, C, D, X0, X1, E, F, G, H> addAt4(final GkPair<X0, X1> tuple) {
		return addAt4(tuple.getValue0(), tuple.getValue1());
	}

	public <X0, X1> GkDecade<A, B, C, D, E, X0, X1, F, G, H> addAt5(final GkPair<X0, X1> tuple) {
		return addAt5(tuple.getValue0(), tuple.getValue1());
	}

	public <X0, X1> GkDecade<A, B, C, D, E, F, X0, X1, G, H> addAt6(final GkPair<X0, X1> tuple) {
		return addAt6(tuple.getValue0(), tuple.getValue1());
	}

	public <X0, X1> GkDecade<A, B, C, D, E, F, G, X0, X1, H> addAt7(final GkPair<X0, X1> tuple) {
		return addAt7(tuple.getValue0(), tuple.getValue1());
	}

	public <X0, X1> GkDecade<A, B, C, D, E, F, G, H, X0, X1> addAt8(final GkPair<X0, X1> tuple) {
		return addAt8(tuple.getValue0(), tuple.getValue1());
	}

	public <X0> GkEnnead<A, B, C, D, E, F, G, H, X0> add(final X0 value0) {
		return addAt8(value0);
	}

	public <X0> GkEnnead<A, B, C, D, E, F, G, H, X0> add(final GkUnit<X0> tuple) {
		return addAt8(tuple);
	}

	public <X0, X1> GkDecade<A, B, C, D, E, F, G, H, X0, X1> add(final X0 value0, final X1 value1) {
		return addAt8(value0, value1);
	}

	public <X0, X1> GkDecade<A, B, C, D, E, F, G, H, X0, X1> add(final GkPair<X0, X1> tuple) {
		return addAt8(tuple);
	}

	public <X> GkOctet<X, B, C, D, E, F, G, H> setAt0(final X value) {
		return new GkOctet<X, B, C, D, E, F, G, H>(value, this.val1, this.val2, this.val3, this.val4, this.val5,
				this.val6, this.val7);
	}

	public <X> GkOctet<A, X, C, D, E, F, G, H> setAt1(final X value) {
		return new GkOctet<A, X, C, D, E, F, G, H>(this.val0, value, this.val2, this.val3, this.val4, this.val5,
				this.val6, this.val7);
	}

	public <X> GkOctet<A, B, X, D, E, F, G, H> setAt2(final X value) {
		return new GkOctet<A, B, X, D, E, F, G, H>(this.val0, this.val1, value, this.val3, this.val4, this.val5,
				this.val6, this.val7);
	}

	public <X> GkOctet<A, B, C, X, E, F, G, H> setAt3(final X value) {
		return new GkOctet<A, B, C, X, E, F, G, H>(this.val0, this.val1, this.val2, value, this.val4, this.val5,
				this.val6, this.val7);
	}

	public <X> GkOctet<A, B, C, D, X, F, G, H> setAt4(final X value) {
		return new GkOctet<A, B, C, D, X, F, G, H>(this.val0, this.val1, this.val2, this.val3, value, this.val5,
				this.val6, this.val7);
	}

	public <X> GkOctet<A, B, C, D, E, X, G, H> setAt5(final X value) {
		return new GkOctet<A, B, C, D, E, X, G, H>(this.val0, this.val1, this.val2, this.val3, this.val4, value,
				this.val6, this.val7);
	}

	public <X> GkOctet<A, B, C, D, E, F, X, H> setAt6(final X value) {
		return new GkOctet<A, B, C, D, E, F, X, H>(this.val0, this.val1, this.val2, this.val3, this.val4, this.val5,
				value, this.val7);
	}

	public <X> GkOctet<A, B, C, D, E, F, G, X> setAt7(final X value) {
		return new GkOctet<A, B, C, D, E, F, G, X>(this.val0, this.val1, this.val2, this.val3, this.val4, this.val5,
				this.val6, value);
	}

	public GkSeptet<B, C, D, E, F, G, H> removeFrom0() {
		return new GkSeptet<B, C, D, E, F, G, H>(this.val1, this.val2, this.val3, this.val4, this.val5, this.val6,
				this.val7);
	}

	public GkSeptet<A, C, D, E, F, G, H> removeFrom1() {
		return new GkSeptet<A, C, D, E, F, G, H>(this.val0, this.val2, this.val3, this.val4, this.val5, this.val6,
				this.val7);
	}

	public GkSeptet<A, B, D, E, F, G, H> removeFrom2() {
		return new GkSeptet<A, B, D, E, F, G, H>(this.val0, this.val1, this.val3, this.val4, this.val5, this.val6,
				this.val7);
	}

	public GkSeptet<A, B, C, E, F, G, H> removeFrom3() {
		return new GkSeptet<A, B, C, E, F, G, H>(this.val0, this.val1, this.val2, this.val4, this.val5, this.val6,
				this.val7);
	}

	public GkSeptet<A, B, C, D, F, G, H> removeFrom4() {
		return new GkSeptet<A, B, C, D, F, G, H>(this.val0, this.val1, this.val2, this.val3, this.val5, this.val6,
				this.val7);
	}

	public GkSeptet<A, B, C, D, E, G, H> removeFrom5() {
		return new GkSeptet<A, B, C, D, E, G, H>(this.val0, this.val1, this.val2, this.val3, this.val4, this.val6,
				this.val7);
	}

	public GkSeptet<A, B, C, D, E, F, H> removeFrom6() {
		return new GkSeptet<A, B, C, D, E, F, H>(this.val0, this.val1, this.val2, this.val3, this.val4, this.val5,
				this.val7);
	}

	public GkSeptet<A, B, C, D, E, F, G> removeFrom7() {
		return new GkSeptet<A, B, C, D, E, F, G>(this.val0, this.val1, this.val2, this.val3, this.val4, this.val5,
				this.val6);
	}

}
