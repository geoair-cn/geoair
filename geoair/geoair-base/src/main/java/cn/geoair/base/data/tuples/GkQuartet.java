package cn.geoair.base.data.tuples;

import java.util.Collection;
import java.util.Iterator;

import cn.geoair.base.data.tuples.valueintf.GkiValue0;
import cn.geoair.base.data.tuples.valueintf.GkiValue1;
import cn.geoair.base.data.tuples.valueintf.GkiValue2;
import cn.geoair.base.data.tuples.valueintf.GkiValue3;

/**
 * <p>
 * A tuple of four elements.
 * </p>
 *
 *
 */
public final class GkQuartet<A, B, C, D> extends GkTuple
		implements GkiValue0<A>, GkiValue1<B>, GkiValue2<C>, GkiValue3<D> {

	private static final long serialVersionUID = 2445136048617019549L;

	private static final int SIZE = 4;

	private final A val0;

	private final B val1;

	private final C val2;

	private final D val3;

	public static <A, B, C, D> GkQuartet<A, B, C, D> with(final A value0, final B value1, final C value2,
			final D value3) {
		return new GkQuartet<A, B, C, D>(value0, value1, value2, value3);
	}

	/**
	 * <p>
	 * Create tuple from array. Array has to have exactly four elements.
	 * </p>
	 * @param <X> the array component type
	 * @param array the array to be converted to a tuple
	 * @return the tuple
	 */
	public static <X> GkQuartet<X, X, X, X> fromArray(final X[] array) {
		if (array == null) {
			throw new IllegalArgumentException("Array cannot be null");
		}
		if (array.length != 4) {
			throw new IllegalArgumentException(
					"Array must have exactly 4 elements in order to create a Quartet. Size is " + array.length);
		}
		return new GkQuartet<X, X, X, X>(array[0], array[1], array[2], array[3]);
	}

	/**
	 * <p>
	 * Create tuple from collection. Collection has to have exactly four elements.
	 * </p>
	 * @param <X> the collection component type
	 * @param collection the collection to be converted to a tuple
	 * @return the tuple
	 */
	public static <X> GkQuartet<X, X, X, X> fromCollection(final Collection<X> collection) {
		return fromIterable(collection);
	}

	/**
	 * <p>
	 * Create tuple from iterable. Iterable has to have exactly four elements.
	 * </p>
	 * @param <X> the iterable component type
	 * @param iterable the iterable to be converted to a tuple
	 * @return the tuple
	 */
	public static <X> GkQuartet<X, X, X, X> fromIterable(final Iterable<X> iterable) {
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
	public static <X> GkQuartet<X, X, X, X> fromIterable(final Iterable<X> iterable, int index) {
		return fromIterable(iterable, index, false);
	}

	private static <X> GkQuartet<X, X, X, X> fromIterable(final Iterable<X> iterable, int index,
			final boolean exactSize) {

		if (iterable == null) {
			throw new IllegalArgumentException("Iterable cannot be null");
		}

		boolean tooFewElements = false;

		X element0 = null;
		X element1 = null;
		X element2 = null;
		X element3 = null;

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

		if (tooFewElements && exactSize) {
			throw new IllegalArgumentException("Not enough elements for creating a Quartet (4 needed)");
		}

		if (iter.hasNext() && exactSize) {
			throw new IllegalArgumentException(
					"Iterable must have exactly 4 available elements in order to create a Quartet.");
		}

		return new GkQuartet<X, X, X, X>(element0, element1, element2, element3);

	}

	public GkQuartet(final A value0, final B value1, final C value2, final D value3) {
		super(value0, value1, value2, value3);
		this.val0 = value0;
		this.val1 = value1;
		this.val2 = value2;
		this.val3 = value3;
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

	@Override
	public int getSize() {
		return SIZE;
	}

	public <X0> GkQuintet<X0, A, B, C, D> addAt0(final X0 value0) {
		return new GkQuintet<X0, A, B, C, D>(value0, this.val0, this.val1, this.val2, this.val3);
	}

	public <X0> GkQuintet<A, X0, B, C, D> addAt1(final X0 value0) {
		return new GkQuintet<A, X0, B, C, D>(this.val0, value0, this.val1, this.val2, this.val3);
	}

	public <X0> GkQuintet<A, B, X0, C, D> addAt2(final X0 value0) {
		return new GkQuintet<A, B, X0, C, D>(this.val0, this.val1, value0, this.val2, this.val3);
	}

	public <X0> GkQuintet<A, B, C, X0, D> addAt3(final X0 value0) {
		return new GkQuintet<A, B, C, X0, D>(this.val0, this.val1, this.val2, value0, this.val3);
	}

	public <X0> GkQuintet<A, B, C, D, X0> addAt4(final X0 value0) {
		return new GkQuintet<A, B, C, D, X0>(this.val0, this.val1, this.val2, this.val3, value0);
	}

	public <X0, X1> GkSextet<X0, X1, A, B, C, D> addAt0(final X0 value0, final X1 value1) {
		return new GkSextet<X0, X1, A, B, C, D>(value0, value1, this.val0, this.val1, this.val2, this.val3);
	}

	public <X0, X1> GkSextet<A, X0, X1, B, C, D> addAt1(final X0 value0, final X1 value1) {
		return new GkSextet<A, X0, X1, B, C, D>(this.val0, value0, value1, this.val1, this.val2, this.val3);
	}

	public <X0, X1> GkSextet<A, B, X0, X1, C, D> addAt2(final X0 value0, final X1 value1) {
		return new GkSextet<A, B, X0, X1, C, D>(this.val0, this.val1, value0, value1, this.val2, this.val3);
	}

	public <X0, X1> GkSextet<A, B, C, X0, X1, D> addAt3(final X0 value0, final X1 value1) {
		return new GkSextet<A, B, C, X0, X1, D>(this.val0, this.val1, this.val2, value0, value1, this.val3);
	}

	public <X0, X1> GkSextet<A, B, C, D, X0, X1> addAt4(final X0 value0, final X1 value1) {
		return new GkSextet<A, B, C, D, X0, X1>(this.val0, this.val1, this.val2, this.val3, value0, value1);
	}

	public <X0, X1, X2> GkSeptet<X0, X1, X2, A, B, C, D> addAt0(final X0 value0, final X1 value1, final X2 value2) {
		return new GkSeptet<X0, X1, X2, A, B, C, D>(value0, value1, value2, this.val0, this.val1, this.val2, this.val3);
	}

	public <X0, X1, X2> GkSeptet<A, X0, X1, X2, B, C, D> addAt1(final X0 value0, final X1 value1, final X2 value2) {
		return new GkSeptet<A, X0, X1, X2, B, C, D>(this.val0, value0, value1, value2, this.val1, this.val2, this.val3);
	}

	public <X0, X1, X2> GkSeptet<A, B, X0, X1, X2, C, D> addAt2(final X0 value0, final X1 value1, final X2 value2) {
		return new GkSeptet<A, B, X0, X1, X2, C, D>(this.val0, this.val1, value0, value1, value2, this.val2, this.val3);
	}

	public <X0, X1, X2> GkSeptet<A, B, C, X0, X1, X2, D> addAt3(final X0 value0, final X1 value1, final X2 value2) {
		return new GkSeptet<A, B, C, X0, X1, X2, D>(this.val0, this.val1, this.val2, value0, value1, value2, this.val3);
	}

	public <X0, X1, X2> GkSeptet<A, B, C, D, X0, X1, X2> addAt4(final X0 value0, final X1 value1, final X2 value2) {
		return new GkSeptet<A, B, C, D, X0, X1, X2>(this.val0, this.val1, this.val2, this.val3, value0, value1, value2);
	}

	public <X0, X1, X2, X3> GkOctet<X0, X1, X2, X3, A, B, C, D> addAt0(final X0 value0, final X1 value1,
			final X2 value2, final X3 value3) {
		return new GkOctet<X0, X1, X2, X3, A, B, C, D>(value0, value1, value2, value3, this.val0, this.val1, this.val2,
				this.val3);
	}

	public <X0, X1, X2, X3> GkOctet<A, X0, X1, X2, X3, B, C, D> addAt1(final X0 value0, final X1 value1,
			final X2 value2, final X3 value3) {
		return new GkOctet<A, X0, X1, X2, X3, B, C, D>(this.val0, value0, value1, value2, value3, this.val1, this.val2,
				this.val3);
	}

	public <X0, X1, X2, X3> GkOctet<A, B, X0, X1, X2, X3, C, D> addAt2(final X0 value0, final X1 value1,
			final X2 value2, final X3 value3) {
		return new GkOctet<A, B, X0, X1, X2, X3, C, D>(this.val0, this.val1, value0, value1, value2, value3, this.val2,
				this.val3);
	}

	public <X0, X1, X2, X3> GkOctet<A, B, C, X0, X1, X2, X3, D> addAt3(final X0 value0, final X1 value1,
			final X2 value2, final X3 value3) {
		return new GkOctet<A, B, C, X0, X1, X2, X3, D>(this.val0, this.val1, this.val2, value0, value1, value2, value3,
				this.val3);
	}

	public <X0, X1, X2, X3> GkOctet<A, B, C, D, X0, X1, X2, X3> addAt4(final X0 value0, final X1 value1,
			final X2 value2, final X3 value3) {
		return new GkOctet<A, B, C, D, X0, X1, X2, X3>(this.val0, this.val1, this.val2, this.val3, value0, value1,
				value2, value3);
	}

	public <X0, X1, X2, X3, X4> GkEnnead<X0, X1, X2, X3, X4, A, B, C, D> addAt0(final X0 value0, final X1 value1,
			final X2 value2, final X3 value3, final X4 value4) {
		return new GkEnnead<X0, X1, X2, X3, X4, A, B, C, D>(value0, value1, value2, value3, value4, this.val0,
				this.val1, this.val2, this.val3);
	}

	public <X0, X1, X2, X3, X4> GkEnnead<A, X0, X1, X2, X3, X4, B, C, D> addAt1(final X0 value0, final X1 value1,
			final X2 value2, final X3 value3, final X4 value4) {
		return new GkEnnead<A, X0, X1, X2, X3, X4, B, C, D>(this.val0, value0, value1, value2, value3, value4,
				this.val1, this.val2, this.val3);
	}

	public <X0, X1, X2, X3, X4> GkEnnead<A, B, X0, X1, X2, X3, X4, C, D> addAt2(final X0 value0, final X1 value1,
			final X2 value2, final X3 value3, final X4 value4) {
		return new GkEnnead<A, B, X0, X1, X2, X3, X4, C, D>(this.val0, this.val1, value0, value1, value2, value3,
				value4, this.val2, this.val3);
	}

	public <X0, X1, X2, X3, X4> GkEnnead<A, B, C, X0, X1, X2, X3, X4, D> addAt3(final X0 value0, final X1 value1,
			final X2 value2, final X3 value3, final X4 value4) {
		return new GkEnnead<A, B, C, X0, X1, X2, X3, X4, D>(this.val0, this.val1, this.val2, value0, value1, value2,
				value3, value4, this.val3);
	}

	public <X0, X1, X2, X3, X4> GkEnnead<A, B, C, D, X0, X1, X2, X3, X4> addAt4(final X0 value0, final X1 value1,
			final X2 value2, final X3 value3, final X4 value4) {
		return new GkEnnead<A, B, C, D, X0, X1, X2, X3, X4>(this.val0, this.val1, this.val2, this.val3, value0, value1,
				value2, value3, value4);
	}

	public <X0, X1, X2, X3, X4, X5> GkDecade<X0, X1, X2, X3, X4, X5, A, B, C, D> addAt0(final X0 value0,
			final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5) {
		return new GkDecade<X0, X1, X2, X3, X4, X5, A, B, C, D>(value0, value1, value2, value3, value4, value5,
				this.val0, this.val1, this.val2, this.val3);
	}

	public <X0, X1, X2, X3, X4, X5> GkDecade<A, X0, X1, X2, X3, X4, X5, B, C, D> addAt1(final X0 value0,
			final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5) {
		return new GkDecade<A, X0, X1, X2, X3, X4, X5, B, C, D>(this.val0, value0, value1, value2, value3, value4,
				value5, this.val1, this.val2, this.val3);
	}

	public <X0, X1, X2, X3, X4, X5> GkDecade<A, B, X0, X1, X2, X3, X4, X5, C, D> addAt2(final X0 value0,
			final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5) {
		return new GkDecade<A, B, X0, X1, X2, X3, X4, X5, C, D>(this.val0, this.val1, value0, value1, value2, value3,
				value4, value5, this.val2, this.val3);
	}

	public <X0, X1, X2, X3, X4, X5> GkDecade<A, B, C, X0, X1, X2, X3, X4, X5, D> addAt3(final X0 value0,
			final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5) {
		return new GkDecade<A, B, C, X0, X1, X2, X3, X4, X5, D>(this.val0, this.val1, this.val2, value0, value1, value2,
				value3, value4, value5, this.val3);
	}

	public <X0, X1, X2, X3, X4, X5> GkDecade<A, B, C, D, X0, X1, X2, X3, X4, X5> addAt4(final X0 value0,
			final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5) {
		return new GkDecade<A, B, C, D, X0, X1, X2, X3, X4, X5>(this.val0, this.val1, this.val2, this.val3, value0,
				value1, value2, value3, value4, value5);
	}

	public <X0> GkQuintet<X0, A, B, C, D> addAt0(final GkUnit<X0> tuple) {
		return addAt0(tuple.getValue0());
	}

	public <X0> GkQuintet<A, X0, B, C, D> addAt1(final GkUnit<X0> tuple) {
		return addAt1(tuple.getValue0());
	}

	public <X0> GkQuintet<A, B, X0, C, D> addAt2(final GkUnit<X0> tuple) {
		return addAt2(tuple.getValue0());
	}

	public <X0> GkQuintet<A, B, C, X0, D> addAt3(final GkUnit<X0> tuple) {
		return addAt3(tuple.getValue0());
	}

	public <X0> GkQuintet<A, B, C, D, X0> addAt4(final GkUnit<X0> tuple) {
		return addAt4(tuple.getValue0());
	}

	public <X0, X1> GkSextet<X0, X1, A, B, C, D> addAt0(final GkPair<X0, X1> tuple) {
		return addAt0(tuple.getValue0(), tuple.getValue1());
	}

	public <X0, X1> GkSextet<A, X0, X1, B, C, D> addAt1(final GkPair<X0, X1> tuple) {
		return addAt1(tuple.getValue0(), tuple.getValue1());
	}

	public <X0, X1> GkSextet<A, B, X0, X1, C, D> addAt2(final GkPair<X0, X1> tuple) {
		return addAt2(tuple.getValue0(), tuple.getValue1());
	}

	public <X0, X1> GkSextet<A, B, C, X0, X1, D> addAt3(final GkPair<X0, X1> tuple) {
		return addAt3(tuple.getValue0(), tuple.getValue1());
	}

	public <X0, X1> GkSextet<A, B, C, D, X0, X1> addAt4(final GkPair<X0, X1> tuple) {
		return addAt4(tuple.getValue0(), tuple.getValue1());
	}

	public <X0, X1, X2> GkSeptet<X0, X1, X2, A, B, C, D> addAt0(final GkTriplet<X0, X1, X2> tuple) {
		return addAt0(tuple.getValue0(), tuple.getValue1(), tuple.getValue2());
	}

	public <X0, X1, X2> GkSeptet<A, X0, X1, X2, B, C, D> addAt1(final GkTriplet<X0, X1, X2> tuple) {
		return addAt1(tuple.getValue0(), tuple.getValue1(), tuple.getValue2());
	}

	public <X0, X1, X2> GkSeptet<A, B, X0, X1, X2, C, D> addAt2(final GkTriplet<X0, X1, X2> tuple) {
		return addAt2(tuple.getValue0(), tuple.getValue1(), tuple.getValue2());
	}

	public <X0, X1, X2> GkSeptet<A, B, C, X0, X1, X2, D> addAt3(final GkTriplet<X0, X1, X2> tuple) {
		return addAt3(tuple.getValue0(), tuple.getValue1(), tuple.getValue2());
	}

	public <X0, X1, X2> GkSeptet<A, B, C, D, X0, X1, X2> addAt4(final GkTriplet<X0, X1, X2> tuple) {
		return addAt4(tuple.getValue0(), tuple.getValue1(), tuple.getValue2());
	}

	public <X0, X1, X2, X3> GkOctet<X0, X1, X2, X3, A, B, C, D> addAt0(final GkQuartet<X0, X1, X2, X3> tuple) {
		return addAt0(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3());
	}

	public <X0, X1, X2, X3> GkOctet<A, X0, X1, X2, X3, B, C, D> addAt1(final GkQuartet<X0, X1, X2, X3> tuple) {
		return addAt1(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3());
	}

	public <X0, X1, X2, X3> GkOctet<A, B, X0, X1, X2, X3, C, D> addAt2(final GkQuartet<X0, X1, X2, X3> tuple) {
		return addAt2(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3());
	}

	public <X0, X1, X2, X3> GkOctet<A, B, C, X0, X1, X2, X3, D> addAt3(final GkQuartet<X0, X1, X2, X3> tuple) {
		return addAt3(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3());
	}

	public <X0, X1, X2, X3> GkOctet<A, B, C, D, X0, X1, X2, X3> addAt4(final GkQuartet<X0, X1, X2, X3> tuple) {
		return addAt4(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3());
	}

	public <X0, X1, X2, X3, X4> GkEnnead<X0, X1, X2, X3, X4, A, B, C, D> addAt0(
			final GkQuintet<X0, X1, X2, X3, X4> tuple) {
		return addAt0(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3(), tuple.getValue4());
	}

	public <X0, X1, X2, X3, X4> GkEnnead<A, X0, X1, X2, X3, X4, B, C, D> addAt1(
			final GkQuintet<X0, X1, X2, X3, X4> tuple) {
		return addAt1(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3(), tuple.getValue4());
	}

	public <X0, X1, X2, X3, X4> GkEnnead<A, B, X0, X1, X2, X3, X4, C, D> addAt2(
			final GkQuintet<X0, X1, X2, X3, X4> tuple) {
		return addAt2(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3(), tuple.getValue4());
	}

	public <X0, X1, X2, X3, X4> GkEnnead<A, B, C, X0, X1, X2, X3, X4, D> addAt3(
			final GkQuintet<X0, X1, X2, X3, X4> tuple) {
		return addAt3(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3(), tuple.getValue4());
	}

	public <X0, X1, X2, X3, X4> GkEnnead<A, B, C, D, X0, X1, X2, X3, X4> addAt4(
			final GkQuintet<X0, X1, X2, X3, X4> tuple) {
		return addAt4(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3(), tuple.getValue4());
	}

	public <X0, X1, X2, X3, X4, X5> GkDecade<X0, X1, X2, X3, X4, X5, A, B, C, D> addAt0(
			final GkSextet<X0, X1, X2, X3, X4, X5> tuple) {
		return addAt0(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3(), tuple.getValue4(),
				tuple.getValue5());
	}

	public <X0, X1, X2, X3, X4, X5> GkDecade<A, X0, X1, X2, X3, X4, X5, B, C, D> addAt1(
			final GkSextet<X0, X1, X2, X3, X4, X5> tuple) {
		return addAt1(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3(), tuple.getValue4(),
				tuple.getValue5());
	}

	public <X0, X1, X2, X3, X4, X5> GkDecade<A, B, X0, X1, X2, X3, X4, X5, C, D> addAt2(
			final GkSextet<X0, X1, X2, X3, X4, X5> tuple) {
		return addAt2(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3(), tuple.getValue4(),
				tuple.getValue5());
	}

	public <X0, X1, X2, X3, X4, X5> GkDecade<A, B, C, X0, X1, X2, X3, X4, X5, D> addAt3(
			final GkSextet<X0, X1, X2, X3, X4, X5> tuple) {
		return addAt3(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3(), tuple.getValue4(),
				tuple.getValue5());
	}

	public <X0, X1, X2, X3, X4, X5> GkDecade<A, B, C, D, X0, X1, X2, X3, X4, X5> addAt4(
			final GkSextet<X0, X1, X2, X3, X4, X5> tuple) {
		return addAt4(tuple.getValue0(), tuple.getValue1(), tuple.getValue2(), tuple.getValue3(), tuple.getValue4(),
				tuple.getValue5());
	}

	public <X0> GkQuintet<A, B, C, D, X0> add(final X0 value0) {
		return addAt4(value0);
	}

	public <X0> GkQuintet<A, B, C, D, X0> add(final GkUnit<X0> tuple) {
		return addAt4(tuple);
	}

	public <X0, X1> GkSextet<A, B, C, D, X0, X1> add(final X0 value0, final X1 value1) {
		return addAt4(value0, value1);
	}

	public <X0, X1> GkSextet<A, B, C, D, X0, X1> add(final GkPair<X0, X1> tuple) {
		return addAt4(tuple);
	}

	public <X0, X1, X2> GkSeptet<A, B, C, D, X0, X1, X2> add(final X0 value0, final X1 value1, final X2 value2) {
		return addAt4(value0, value1, value2);
	}

	public <X0, X1, X2> GkSeptet<A, B, C, D, X0, X1, X2> add(final GkTriplet<X0, X1, X2> tuple) {
		return addAt4(tuple);
	}

	public <X0, X1, X2, X3> GkOctet<A, B, C, D, X0, X1, X2, X3> add(final X0 value0, final X1 value1, final X2 value2,
			final X3 value3) {
		return addAt4(value0, value1, value2, value3);
	}

	public <X0, X1, X2, X3> GkOctet<A, B, C, D, X0, X1, X2, X3> add(final GkQuartet<X0, X1, X2, X3> tuple) {
		return addAt4(tuple);
	}

	public <X0, X1, X2, X3, X4> GkEnnead<A, B, C, D, X0, X1, X2, X3, X4> add(final X0 value0, final X1 value1,
			final X2 value2, final X3 value3, final X4 value4) {
		return addAt4(value0, value1, value2, value3, value4);
	}

	public <X0, X1, X2, X3, X4> GkEnnead<A, B, C, D, X0, X1, X2, X3, X4> add(
			final GkQuintet<X0, X1, X2, X3, X4> tuple) {
		return addAt4(tuple);
	}

	public <X0, X1, X2, X3, X4, X5> GkDecade<A, B, C, D, X0, X1, X2, X3, X4, X5> add(final X0 value0, final X1 value1,
			final X2 value2, final X3 value3, final X4 value4, final X5 value5) {
		return addAt4(value0, value1, value2, value3, value4, value5);
	}

	public <X0, X1, X2, X3, X4, X5> GkDecade<A, B, C, D, X0, X1, X2, X3, X4, X5> add(
			final GkSextet<X0, X1, X2, X3, X4, X5> tuple) {
		return addAt4(tuple);
	}

	public <X> GkQuartet<X, B, C, D> setAt0(final X value) {
		return new GkQuartet<X, B, C, D>(value, this.val1, this.val2, this.val3);
	}

	public <X> GkQuartet<A, X, C, D> setAt1(final X value) {
		return new GkQuartet<A, X, C, D>(this.val0, value, this.val2, this.val3);
	}

	public <X> GkQuartet<A, B, X, D> setAt2(final X value) {
		return new GkQuartet<A, B, X, D>(this.val0, this.val1, value, this.val3);
	}

	public <X> GkQuartet<A, B, C, X> setAt3(final X value) {
		return new GkQuartet<A, B, C, X>(this.val0, this.val1, this.val2, value);
	}

	public GkTriplet<B, C, D> removeFrom0() {
		return new GkTriplet<B, C, D>(this.val1, this.val2, this.val3);
	}

	public GkTriplet<A, C, D> removeFrom1() {
		return new GkTriplet<A, C, D>(this.val0, this.val2, this.val3);
	}

	public GkTriplet<A, B, D> removeFrom2() {
		return new GkTriplet<A, B, D>(this.val0, this.val1, this.val3);
	}

	public GkTriplet<A, B, C> removeFrom3() {
		return new GkTriplet<A, B, C>(this.val0, this.val1, this.val2);
	}

}
