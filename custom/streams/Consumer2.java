package custom.streams;

public interface Consumer2<T, U> // BiConsumer<T, U>
{
    void accept(T input1, U input2);
}