package custom.streams;

public interface Predicate<T>
{
    boolean test(T input);

    public static <U> Predicate<U> not(Predicate<U> predicate)
    {
        return input -> !predicate.test(input);
    }

//    public static Predicate<T> and(Predicate<T> pred1, Predicate<T> pred2, Predicate<T> rest...)
//    {
//        return new Predicate<T>() {
//            @Override
//            public boolean test(T input) {
//                return pred1.test(input)
//                        && pred2.test(input)
//                        &&
//            }
//        }
//    }
}
