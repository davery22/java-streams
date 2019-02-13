package custom.streams;

public interface Collector<T, A, R>
{
    // T - type of input elements
    // A - type of mutable accumulator
    // R - type of result

    Supplier<A> supplier();
    Consumer2<A, T> accumulator();
    Function2<A, A, A> combiner();
    Function1<A, R> finalizer();

    public static <U, V, W> Collector<U, V, W> of(
            Supplier<V> supplier,
            Consumer2<V, U> accumulator,
            Function2<V, V, V> combiner,
            Function1<V, W> finalizer)
    {
        return new Collector<U, V, W>() {
            @Override
            public Supplier<V> supplier() {
                return supplier;
            }

            @Override
            public Consumer2<V, U> accumulator() {
                return accumulator;
            }

            @Override
            public Function2<V, V, V> combiner() {
                return combiner;
            }

            @Override
            public Function1<V, W> finalizer() {
                return finalizer;
            }
        };
    }
}