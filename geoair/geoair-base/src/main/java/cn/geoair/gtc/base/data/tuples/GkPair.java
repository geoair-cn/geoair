package cn.geoair.gtc.base.data.tuples;

import java.util.Collection;
import java.util.Iterator;

import cn.geoair.gtc.base.data.tuples.valueintf.GkiValue0;
import cn.geoair.gtc.base.data.tuples.valueintf.GkiValue1;

/**
 * <p>
 * A tuple of two elements.
 * </p>
 *
 *
 */
public final class GkPair<A,B>
        extends GkTuple
        implements GkiValue0<A>,
        GkiValue1<B> {

    private static final long serialVersionUID = 2438099850625502138L;

    private static final int SIZE = 2;

    private final A val0;
    private final B val1;



    public static <A,B> GkPair<A,B> with(final A value0, final B value1) {
        return new GkPair<A,B>(value0,value1);
    }


    /**
     * <p>
     * Create tuple from array. Array has to have exactly two elements.
     * </p>
     *
     * @param <X> the array component type
     * @param array the array to be converted to a tuple
     * @return the tuple
     */
    public static <X> GkPair<X,X> fromArray(final X[] array) {
        if (array == null) {
            throw new IllegalArgumentException("Array cannot be null");
        }
        if (array.length != 2) {
            throw new IllegalArgumentException("Array must have exactly 2 elements in order to create a Pair. Size is " + array.length);
        }
        return new GkPair<X,X>(array[0],array[1]);
    }


    /**
     * <p>
     * Create tuple from collection. Collection has to have exactly two elements.
     * </p>
     *
     * @param <X> the collection component type
     * @param collection the collection to be converted to a tuple
     * @return the tuple
     */
    public static <X> GkPair<X,X> fromCollection(final Collection<X> collection) {
        return fromIterable(collection);
    }



    /**
     * <p>
     * Create tuple from iterable. Iterable has to have exactly two elements.
     * </p>
     *
     * @param <X> the iterable component type
     * @param iterable the iterable to be converted to a tuple
     * @return the tuple
     */
    public static <X> GkPair<X,X> fromIterable(final Iterable<X> iterable) {
        return fromIterable(iterable, 0, true);
    }



    /**
     * <p>
     * Create tuple from iterable, starting from the specified index. Iterable
     * can have more (or less) elements than the tuple to be created.
     * </p>
     *
     * @param <X> the iterable component type
     * @param iterable the iterable to be converted to a tuple
     * @return the tuple
     */
    public static <X> GkPair<X,X> fromIterable(final Iterable<X> iterable, int index) {
        return fromIterable(iterable, index, false);
    }





    private static <X> GkPair<X,X> fromIterable(final Iterable<X> iterable, int index, final boolean exactSize) {

        if (iterable == null) {
            throw new IllegalArgumentException("Iterable cannot be null");
        }

        boolean tooFewElements = false;

        X element0 = null;
        X element1 = null;

        final Iterator<X> iter = iterable.iterator();

        int i = 0;
        while (i < index) {
            if (iter.hasNext()) {
                iter.next();
            } else {
                tooFewElements = true;
            }
            i++;
        }

        if (iter.hasNext()) {
            element0 = iter.next();
        } else {
            tooFewElements = true;
        }

        if (iter.hasNext()) {
            element1 = iter.next();
        } else {
            tooFewElements = true;
        }

        if (tooFewElements && exactSize) {
            throw new IllegalArgumentException("Not enough elements for creating a Pair (2 needed)");
        }

        if (iter.hasNext() && exactSize) {
            throw new IllegalArgumentException("Iterable must have exactly 2 available elements in order to create a Pair.");
        }

        return new GkPair<X,X>(element0, element1);

    }





    public GkPair(
            final A value0,
            final B value1) {
        super(value0, value1);
        this.val0 = value0;
        this.val1 = value1;
    }


    public A getValue0() {
        return this.val0;
    }


    public B getValue1() {
        return this.val1;
    }


    @Override
    public int getSize() {
        return SIZE;
    }











    public <X0> GkTriplet<X0,A,B> addAt0(final X0 value0) {
        return new GkTriplet<X0,A,B>(
                value0, this.val0, this.val1);
    }

    public <X0> GkTriplet<A,X0,B> addAt1(final X0 value0) {
        return new GkTriplet<A,X0,B>(
                this.val0, value0, this.val1);
    }

    public <X0> GkTriplet<A,B,X0> addAt2(final X0 value0) {
        return new GkTriplet<A,B,X0>(
                this.val0, this.val1, value0);
    }





    public <X0,X1> GkQuartet<X0,X1,A,B> addAt0(final X0 value0, final X1 value1) {
        return new GkQuartet<X0,X1,A,B>(
                value0, value1, this.val0, this.val1);
    }

    public <X0,X1> GkQuartet<A,X0,X1,B> addAt1(final X0 value0, final X1 value1) {
        return new GkQuartet<A,X0,X1,B>(
                this.val0, value0, value1, this.val1);
    }

    public <X0,X1> GkQuartet<A,B,X0,X1> addAt2(final X0 value0, final X1 value1) {
        return new GkQuartet<A,B,X0,X1>(
                this.val0, this.val1, value0, value1);
    }







    public <X0,X1,X2> GkQuintet<X0,X1,X2,A,B> addAt0(final X0 value0, final X1 value1, final X2 value2) {
        return new GkQuintet<X0,X1,X2,A,B>(
                value0, value1, value2, this.val0, this.val1);
    }

    public <X0,X1,X2> GkQuintet<A,X0,X1,X2,B> addAt1(final X0 value0, final X1 value1, final X2 value2) {
        return new GkQuintet<A,X0,X1,X2,B>(
                this.val0, value0, value1, value2, this.val1);
    }

    public <X0,X1,X2> GkQuintet<A,B,X0,X1,X2> addAt2(final X0 value0, final X1 value1, final X2 value2) {
        return new GkQuintet<A,B,X0,X1,X2>(
                this.val0, this.val1, value0, value1, value2);
    }







    public <X0,X1,X2,X3> GkSextet<X0,X1,X2,X3,A,B> addAt0(final X0 value0, final X1 value1, final X2 value2, final X3 value3) {
        return new GkSextet<X0,X1,X2,X3,A,B>(
                value0, value1, value2, value3, this.val0, this.val1);
    }

    public <X0,X1,X2,X3> GkSextet<A,X0,X1,X2,X3,B> addAt1(final X0 value0, final X1 value1, final X2 value2, final X3 value3) {
        return new GkSextet<A,X0,X1,X2,X3,B>(
                this.val0, value0, value1, value2, value3, this.val1);
    }

    public <X0,X1,X2,X3> GkSextet<A,B,X0,X1,X2,X3> addAt2(final X0 value0, final X1 value1, final X2 value2, final X3 value3) {
        return new GkSextet<A,B,X0,X1,X2,X3>(
                this.val0, this.val1, value0, value1, value2, value3);
    }






    public <X0,X1,X2,X3,X4> GkSeptet<X0,X1,X2,X3,X4,A,B> addAt0(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4) {
        return new GkSeptet<X0,X1,X2,X3,X4,A,B>(
                value0, value1, value2, value3, value4, this.val0, this.val1);
    }

    public <X0,X1,X2,X3,X4> GkSeptet<A,X0,X1,X2,X3,X4,B> addAt1(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4) {
        return new GkSeptet<A,X0,X1,X2,X3,X4,B>(
                this.val0, value0, value1, value2, value3, value4, this.val1);
    }

    public <X0,X1,X2,X3,X4> GkSeptet<A,B,X0,X1,X2,X3,X4> addAt2(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4) {
        return new GkSeptet<A,B,X0,X1,X2,X3,X4>(
                this.val0, this.val1, value0, value1, value2, value3, value4);
    }






    public <X0,X1,X2,X3,X4,X5> GkOctet<X0,X1,X2,X3,X4,X5,A,B> addAt0(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5) {
        return new GkOctet<X0,X1,X2,X3,X4,X5,A,B>(
                value0, value1, value2, value3, value4, value5, this.val0, this.val1);
    }

    public <X0,X1,X2,X3,X4,X5> GkOctet<A,X0,X1,X2,X3,X4,X5,B> addAt1(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5) {
        return new GkOctet<A,X0,X1,X2,X3,X4,X5,B>(
                this.val0, value0, value1, value2, value3, value4, value5, this.val1);
    }

    public <X0,X1,X2,X3,X4,X5> GkOctet<A,B,X0,X1,X2,X3,X4,X5> addAt2(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5) {
        return new GkOctet<A,B,X0,X1,X2,X3,X4,X5>(
                this.val0, this.val1, value0, value1, value2, value3, value4, value5);
    }






    public <X0,X1,X2,X3,X4,X5,X6> GkEnnead<X0,X1,X2,X3,X4,X5,X6,A,B> addAt0(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5, final X6 value6) {
        return new GkEnnead<X0,X1,X2,X3,X4,X5,X6,A,B>(
                value0, value1, value2, value3, value4, value5, value6, this.val0, this.val1);
    }

    public <X0,X1,X2,X3,X4,X5,X6> GkEnnead<A,X0,X1,X2,X3,X4,X5,X6,B> addAt1(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5, final X6 value6) {
        return new GkEnnead<A,X0,X1,X2,X3,X4,X5,X6,B>(
                this.val0, value0, value1, value2, value3, value4, value5, value6, this.val1);
    }

    public <X0,X1,X2,X3,X4,X5,X6> GkEnnead<A,B,X0,X1,X2,X3,X4,X5,X6> addAt2(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5, final X6 value6) {
        return new GkEnnead<A,B,X0,X1,X2,X3,X4,X5,X6>(
                this.val0, this.val1, value0, value1, value2, value3, value4, value5, value6);
    }






    public <X0,X1,X2,X3,X4,X5,X6,X7> GkDecade<X0,X1,X2,X3,X4,X5,X6,X7,A,B> addAt0(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5, final X6 value6, final X7 value7) {
        return new GkDecade<X0,X1,X2,X3,X4,X5,X6,X7,A,B>(
                value0, value1, value2, value3, value4, value5, value6, value7, this.val0, this.val1);
    }

    public <X0,X1,X2,X3,X4,X5,X6,X7> GkDecade<A,X0,X1,X2,X3,X4,X5,X6,X7,B> addAt1(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5, final X6 value6, final X7 value7) {
        return new GkDecade<A,X0,X1,X2,X3,X4,X5,X6,X7,B>(
                this.val0, value0, value1, value2, value3, value4, value5, value6, value7, this.val1);
    }

    public <X0,X1,X2,X3,X4,X5,X6,X7> GkDecade<A,B,X0,X1,X2,X3,X4,X5,X6,X7> addAt2(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5, final X6 value6, final X7 value7) {
        return new GkDecade<A,B,X0,X1,X2,X3,X4,X5,X6,X7>(
                this.val0, this.val1, value0, value1, value2, value3, value4, value5, value6, value7);
    }








    public <X0> GkTriplet<X0,A,B> addAt0(final GkUnit<X0> tuple) {
        return addAt0(tuple.getValue0());
    }

    public <X0> GkTriplet<A,X0,B> addAt1(final GkUnit<X0> tuple) {
        return addAt1(tuple.getValue0());
    }

    public <X0> GkTriplet<A,B,X0> addAt2(final GkUnit<X0> tuple) {
        return addAt2(tuple.getValue0());
    }







    public <X0,X1> GkQuartet<X0,X1,A,B> addAt0(final GkPair<X0,X1> tuple) {
        return addAt0(tuple.getValue0(),tuple.getValue1());
    }

    public <X0,X1> GkQuartet<A,X0,X1,B> addAt1(final GkPair<X0,X1> tuple) {
        return addAt1(tuple.getValue0(),tuple.getValue1());
    }

    public <X0,X1> GkQuartet<A,B,X0,X1> addAt2(final GkPair<X0,X1> tuple) {
        return addAt2(tuple.getValue0(),tuple.getValue1());
    }








    public <X0,X1,X2> GkQuintet<X0,X1,X2,A,B> addAt0(final GkTriplet<X0,X1,X2> tuple) {
        return addAt0(tuple.getValue0(),tuple.getValue1(),tuple.getValue2());
    }

    public <X0,X1,X2> GkQuintet<A,X0,X1,X2,B> addAt1(final GkTriplet<X0,X1,X2> tuple) {
        return addAt1(tuple.getValue0(),tuple.getValue1(),tuple.getValue2());
    }

    public <X0,X1,X2> GkQuintet<A,B,X0,X1,X2> addAt2(final GkTriplet<X0,X1,X2> tuple) {
        return addAt2(tuple.getValue0(),tuple.getValue1(),tuple.getValue2());
    }









    public <X0,X1,X2,X3> GkSextet<X0,X1,X2,X3,A,B> addAt0(final GkQuartet<X0,X1,X2,X3> tuple) {
        return addAt0(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3());
    }

    public <X0,X1,X2,X3> GkSextet<A,X0,X1,X2,X3,B> addAt1(final GkQuartet<X0,X1,X2,X3> tuple) {
        return addAt1(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3());
    }

    public <X0,X1,X2,X3> GkSextet<A,B,X0,X1,X2,X3> addAt2(final GkQuartet<X0,X1,X2,X3> tuple) {
        return addAt2(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3());
    }








    public <X0,X1,X2,X3,X4> GkSeptet<X0,X1,X2,X3,X4,A,B> addAt0(final GkQuintet<X0,X1,X2,X3,X4> tuple) {
        return addAt0(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3(),tuple.getValue4());
    }

    public <X0,X1,X2,X3,X4> GkSeptet<A,X0,X1,X2,X3,X4,B> addAt1(final GkQuintet<X0,X1,X2,X3,X4> tuple) {
        return addAt1(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3(),tuple.getValue4());
    }

    public <X0,X1,X2,X3,X4> GkSeptet<A,B,X0,X1,X2,X3,X4> addAt2(final GkQuintet<X0,X1,X2,X3,X4> tuple) {
        return addAt2(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3(),tuple.getValue4());
    }








    public <X0,X1,X2,X3,X4,X5> GkOctet<X0,X1,X2,X3,X4,X5,A,B> addAt0(final GkSextet<X0,X1,X2,X3,X4,X5> tuple) {
        return addAt0(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3(),tuple.getValue4(),tuple.getValue5());
    }

    public <X0,X1,X2,X3,X4,X5> GkOctet<A,X0,X1,X2,X3,X4,X5,B> addAt1(final GkSextet<X0,X1,X2,X3,X4,X5> tuple) {
        return addAt1(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3(),tuple.getValue4(),tuple.getValue5());
    }

    public <X0,X1,X2,X3,X4,X5> GkOctet<A,B,X0,X1,X2,X3,X4,X5> addAt2(final GkSextet<X0,X1,X2,X3,X4,X5> tuple) {
        return addAt2(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3(),tuple.getValue4(),tuple.getValue5());
    }








    public <X0,X1,X2,X3,X4,X5,X6> GkEnnead<X0,X1,X2,X3,X4,X5,X6,A,B> addAt0(final GkSeptet<X0,X1,X2,X3,X4,X5,X6> tuple) {
        return addAt0(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3(),tuple.getValue4(),tuple.getValue5(),tuple.getValue6());
    }

    public <X0,X1,X2,X3,X4,X5,X6> GkEnnead<A,X0,X1,X2,X3,X4,X5,X6,B> addAt1(final GkSeptet<X0,X1,X2,X3,X4,X5,X6> tuple) {
        return addAt1(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3(),tuple.getValue4(),tuple.getValue5(),tuple.getValue6());
    }

    public <X0,X1,X2,X3,X4,X5,X6> GkEnnead<A,B,X0,X1,X2,X3,X4,X5,X6> addAt2(final GkSeptet<X0,X1,X2,X3,X4,X5,X6> tuple) {
        return addAt2(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3(),tuple.getValue4(),tuple.getValue5(),tuple.getValue6());
    }








    public <X0,X1,X2,X3,X4,X5,X6,X7> GkDecade<X0,X1,X2,X3,X4,X5,X6,X7,A,B> addAt0(final GkOctet<X0,X1,X2,X3,X4,X5,X6,X7> tuple) {
        return addAt0(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3(),tuple.getValue4(),tuple.getValue5(),tuple.getValue6(),tuple.getValue7());
    }

    public <X0,X1,X2,X3,X4,X5,X6,X7> GkDecade<A,X0,X1,X2,X3,X4,X5,X6,X7,B> addAt1(final GkOctet<X0,X1,X2,X3,X4,X5,X6,X7> tuple) {
        return addAt1(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3(),tuple.getValue4(),tuple.getValue5(),tuple.getValue6(),tuple.getValue7());
    }

    public <X0,X1,X2,X3,X4,X5,X6,X7> GkDecade<A,B,X0,X1,X2,X3,X4,X5,X6,X7> addAt2(final GkOctet<X0,X1,X2,X3,X4,X5,X6,X7> tuple) {
        return addAt2(tuple.getValue0(),tuple.getValue1(),tuple.getValue2(),tuple.getValue3(),tuple.getValue4(),tuple.getValue5(),tuple.getValue6(),tuple.getValue7());
    }









    public <X0> GkTriplet<A,B,X0> add(final X0 value0) {
        return addAt2(value0);
    }


    public <X0> GkTriplet<A,B,X0> add(final GkUnit<X0> tuple) {
        return addAt2(tuple);
    }




    public <X0,X1> GkQuartet<A,B,X0,X1> add(final X0 value0, final X1 value1) {
        return addAt2(value0, value1);
    }


    public <X0,X1> GkQuartet<A,B,X0,X1> add(final GkPair<X0,X1> tuple) {
        return addAt2(tuple);
    }




    public <X0,X1,X2> GkQuintet<A,B,X0,X1,X2> add(final X0 value0, final X1 value1, final X2 value2) {
        return addAt2(value0, value1, value2);
    }


    public <X0,X1,X2> GkQuintet<A,B,X0,X1,X2> add(final GkTriplet<X0,X1,X2> tuple) {
        return addAt2(tuple);
    }




    public <X0,X1,X2,X3> GkSextet<A,B,X0,X1,X2,X3> add(final X0 value0, final X1 value1, final X2 value2, final X3 value3) {
        return addAt2(value0, value1, value2, value3);
    }


    public <X0,X1,X2,X3> GkSextet<A,B,X0,X1,X2,X3> add(final GkQuartet<X0,X1,X2,X3> tuple) {
        return addAt2(tuple);
    }





    public <X0,X1,X2,X3,X4> GkSeptet<A,B,X0,X1,X2,X3,X4> add(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4) {
        return addAt2(value0, value1, value2, value3, value4);
    }


    public <X0,X1,X2,X3,X4> GkSeptet<A,B,X0,X1,X2,X3,X4> add(final GkQuintet<X0,X1,X2,X3,X4> tuple) {
        return addAt2(tuple);
    }





    public <X0,X1,X2,X3,X4,X5> GkOctet<A,B,X0,X1,X2,X3,X4,X5> add(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5) {
        return addAt2(value0, value1, value2, value3, value4, value5);
    }


    public <X0,X1,X2,X3,X4,X5> GkOctet<A,B,X0,X1,X2,X3,X4,X5> add(final GkSextet<X0,X1,X2,X3,X4,X5> tuple) {
        return addAt2(tuple);
    }





    public <X0,X1,X2,X3,X4,X5,X6> GkEnnead<A,B,X0,X1,X2,X3,X4,X5,X6> add(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5, final X6 value6) {
        return addAt2(value0, value1, value2, value3, value4, value5, value6);
    }


    public <X0,X1,X2,X3,X4,X5,X6> GkEnnead<A,B,X0,X1,X2,X3,X4,X5,X6> add(final GkSeptet<X0,X1,X2,X3,X4,X5,X6> tuple) {
        return addAt2(tuple);
    }





    public <X0,X1,X2,X3,X4,X5,X6,X7> GkDecade<A,B,X0,X1,X2,X3,X4,X5,X6,X7> add(final X0 value0, final X1 value1, final X2 value2, final X3 value3, final X4 value4, final X5 value5, final X6 value6, final X7 value7) {
        return addAt2(value0, value1, value2, value3, value4, value5, value6, value7);
    }


    public <X0,X1,X2,X3,X4,X5,X6,X7> GkDecade<A,B,X0,X1,X2,X3,X4,X5,X6,X7> add(final GkOctet<X0,X1,X2,X3,X4,X5,X6,X7> tuple) {
        return addAt2(tuple);
    }









    public <X> GkPair<X,B> setAt0(final X value) {
        return new GkPair<X,B>(
                value, this.val1);
    }

    public <X> GkPair<A,X> setAt1(final X value) {
        return new GkPair<A,X>(
                this.val0, value);
    }









    public GkUnit<B> removeFrom0() {
        return new GkUnit<B>(
                this.val1);
    }

    public GkUnit<A> removeFrom1() {
        return new GkUnit<A>(
                this.val0);
    }



}
