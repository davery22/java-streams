package custom.streams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Stream<T>
{
    private static final Iterator<Object> EMPTY_ITERATOR = new Iterator<Object>() {
        @Override
        public boolean hasNext()
        {
            return false;
        }

        @Override
        public Object next()
        {
            throw new NoSuchElementException();
        }
    };

    @SuppressWarnings("unchecked")
    private static <V> Iterator<V> emptyIterator()
    {
        return (Iterator<V>) EMPTY_ITERATOR;
    }

    public static <U> Iterator<U> adaptIterator(java.util.Iterator<U> iter)
    {
        return new Iterator<U>() {
            @Override
            public boolean hasNext()
            {
                return iter.hasNext();
            }

            @Override
            public U next()
            {
                return iter.next();
            }
        };
    }

    public static <U> Iterator<U> toIter(java.lang.Iterable<U> iter)
    {
        return adaptIterator(iter.iterator());
    }


    // BEGIN

    final Iterator<? extends T> elements;

    private Stream(Iterator<? extends T> elements)
    {
        this.elements = elements;
    }

    public static <U> Stream<U> of(Iterator<? extends U> elements)
    {
        return new Stream<>(elements);
    }

    public static <U> Stream<U> of(Iterable<? extends U> elements)
    {
        return new Stream<>(toIter(elements));
    }

    public <U> Stream<U> map(Function1<? super T, ? extends U> mapper)
    {
        Iterator<U> newIterator = new Iterator<U>() {
            @Override
            public boolean hasNext()
            {
                return elements.hasNext();
            }

            @Override
            public U next()
            {
                return mapper.apply(elements.next());
            }
        };

        return new Stream<>(newIterator);
    }

    public <U> Stream<U> flatMap(Function1<? super T, ? extends Stream<? extends U>> flatMapper)
    {
        Iterator<U> newIterator = new Iterator<U>() {
            private Iterator<? extends U> currentIterator = emptyIterator();

            @Override
            public boolean hasNext()
            {
                if (this.currentIterator.hasNext())
                {
                    return true;
                }

                if (!elements.hasNext())
                {
                    return false;
                }

                this.currentIterator = flatMapper.apply(elements.next()).elements;

                return this.hasNext();
            }

            @Override
            public U next()
            {
                this.hasNext(); // Executing for side-effects!

                return this.currentIterator.next();
            }
        };

        return new Stream<>(newIterator);
    }

    public Stream<T> filter(Predicate<? super T> predicate)
    {
        Iterator<T> newIterator = new Iterator<T>() {
            private T nextElement = null;
            private boolean nextElementIsPresent = false;

            @Override
            public boolean hasNext()
            {
//                if (this.nextElementIsPresent)
//                {
//                    return true;
//                }
//
//                if (!elements.hasNext())
//                {
//                    return false;
//                }
//
//                this.nextElement = elements.next();
//
//                if (predicate.test(this.nextElement))
//                {
//                    this.nextElementIsPresent = true;
//                }
//
//                return this.hasNext();

                // — OR —

                if (this.nextElementIsPresent)
                {
                    return true;
                }

                do
                {
                    if (!elements.hasNext())
                    {
                        return false;
                    }

                    this.nextElement = elements.next();
                }
                while (!predicate.test(this.nextElement));

                this.nextElementIsPresent = true;
                return true;
            }

            @Override
            public T next()
            {
                this.hasNext(); // Executing for side-effects!

                if (!this.nextElementIsPresent)
                {
                    throw new NoSuchElementException(); // Maybe something else
                }

                this.nextElementIsPresent = false;

                return this.nextElement;
            }
        };

        return new Stream<>(newIterator);
    }

    public boolean anyMatch(Predicate<? super T> predicate)
    {
        while (elements.hasNext())
        {
            if (predicate.test(elements.next()))
            {
                return true;
            }
        }

        return false;
    }

    public boolean allMatch(Predicate<? super T> predicate)
    {
        while (elements.hasNext())
        {
            if (!predicate.test(elements.next()))
            {
                return false;
            }
        }

        return true;
    }

    public Optional<T> findFirst()
    {
        if (elements.hasNext())
        {
            return Optional.of(elements.next());
        }

        return Optional.empty();
    }

//    public Tuple2Stream<T, Integer> zipWithIndex()
//    {
//
//    }
//
//    //zip
//
//    public Stream<List<T>> grouped(int size)
//    {
//
//    }

    public Stream<T> take(long count) // limit
    {
        Iterator<T> newIterator = new Iterator<T>() {
            private long takenSoFar = 0;

            @Override
            public boolean hasNext()
            {
                return elements.hasNext() && this.takenSoFar < count;
            }

            @Override
            public T next()
            {
                T element = elements.next();
                ++this.takenSoFar;
                return element;
            }
        };

        return Stream.of(newIterator);
    }

    public Stream<T> takeWhile(Predicate<? super T> predicate)
    {
        Iterator<T> newIterator = new Iterator<T>() {
            private T nextElement = null;
            private boolean nextElementIsPresent = false;
            private boolean isTaking = true;

            @Override
            public boolean hasNext()
            {
                if (this.nextElementIsPresent)
                {
                    return true;
                }

                if (!elements.hasNext() || !this.isTaking)
                {
                    return false;
                }

                this.nextElement = elements.next();

                this.isTaking = predicate.test(this.nextElement);
                this.nextElementIsPresent = this.isTaking;

                return this.nextElementIsPresent;
            }

            @Override
            public T next()
            {
                this.hasNext(); // Executing for side-effects!

                if (!this.nextElementIsPresent)
                {
                    throw new NoSuchElementException();
                }

                this.nextElementIsPresent = false;

                return this.nextElement;
            }
        };

        return Stream.of(newIterator);
    }

    public Stream<T> drop(long count)
    {
        Iterator<T> newIterator = new Iterator<T>() {
            private long droppedSoFar = 0;

            @Override
            public boolean hasNext()
            {
                while (droppedSoFar < count && elements.hasNext())
                {
                    ++this.droppedSoFar;
                    elements.next();
                }

                return elements.hasNext();
            }

            @Override
            public T next()
            {
                this.hasNext(); // Executing for side-effects!

                return elements.next();
            }
        };

        return Stream.of(newIterator);
    }

    public Stream<T> dropWhile(Predicate<? super T> predicate)
    {
        Iterator<T> newIterator = new Iterator<T>() {
            private T nextElement = null;
            private boolean nextElementIsPresent = false;
            private boolean isDropping = true;

            @Override
            public boolean hasNext()
            {
                while (this.isDropping && elements.hasNext())
                {
                    this.nextElement = elements.next();

                    if (!predicate.test(this.nextElement))
                    {
                        this.isDropping = false;
                        this.nextElementIsPresent = true;
                    }
                }

                if (!this.nextElementIsPresent)
                {
                    if (!elements.hasNext())
                    {
                        return false;
                    }

                    this.nextElement = elements.next();
                    this.nextElementIsPresent = true;
                }

                return true;
            }

            @Override
            public T next()
            {
                this.hasNext(); // Executing for side-effects!

                if (!this.nextElementIsPresent)
                {
                    throw new NoSuchElementException();
                }

                this.nextElementIsPresent = false;

                return this.nextElement;
            }
        };

        return Stream.of(newIterator);
    }

    public List<T> collect()
    {
        List<T> result = new ArrayList<>();

        while (elements.hasNext())
        {
            result.add(elements.next());
        }

        return result;
    }

    public <A, R> R collect(Collector<? super T, A, R> collector)
    {
        A acc = collector.supplier().get(); // TODO: rename?
        Consumer2<A, ? super T> accumulator = collector.accumulator();

        while (elements.hasNext())
        {
            accumulator.accept(acc, elements.next());
        }

        return collector.finalizer().apply(acc);
    }

    public static void main(String[] args)
    {
        Iterator<List<Integer>> iter = new Iterator<List<Integer>>() {
            int i = 0;

            @Override
            public boolean hasNext()
            {
                return i < 100;
            }

            @Override
            public List<Integer> next()
            {
                if (hasNext())
                {
                    ++i;
                    return Arrays.asList(new Integer[]{0, 1, 2, 3});
                }

                throw new NoSuchElementException();
            }
        };

        Integer result = Stream.of(iter)
                .flatMap(lis -> Stream.of(lis))
                .filter(i -> i % 2 == 0)
                .map(i -> i + 3)
                .filter(i -> i > 2)
                .flatMap(i -> Stream.of(Arrays.asList(new Integer[]{i, i+1, i+2})))
                .take(10)
                .dropWhile(i -> i > 3)
                .findFirst().get();
//                .collect();

        System.out.println(result);
    }
}
