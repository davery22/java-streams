package custom.streams;

public interface Spliterator<T>
{
    boolean tryAdvance(Consumer1<? super T> consumer);
}