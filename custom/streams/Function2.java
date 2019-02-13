package custom.streams;

public interface Function2<T, U, V> // BiFunction<T, U, V>
{
    V apply(T input1, U input2);
}
