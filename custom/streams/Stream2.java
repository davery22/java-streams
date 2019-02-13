package custom.streams;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Stream2<T>
{
    private static final Spliterator<Object> EMPTY_SPLITERATOR = consumer -> false;

    @SuppressWarnings("unchecked")
    private static <U> Spliterator<U> emptySpliterator()
    {
        return (Spliterator<U>) EMPTY_SPLITERATOR;
    }

    public static <U> Spliterator<U> adaptSpliterator(java.util.Spliterator<U> elements)
    {
        return consumer -> elements.tryAdvance(consumer::accept);
    }

    public static <U> Spliterator<U> toSpliter(java.lang.Iterable<U> elements)
    {
        return adaptSpliterator(elements.spliterator());
    }

    // BEGIN

    final Spliterator<? extends T> elements;

    private Stream2(Spliterator<? extends T> elements)
    {
        this.elements = elements;
    }

    public static <U> Stream2<U> of(Spliterator<? extends U> elements)
    {
        return new Stream2<>(elements);
    }

    public static <U> Stream2<U> of(Iterable<? extends U> elements)
    {
        return new Stream2<>(toSpliter(elements));
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <U> Stream2<U> of(U... elements)
    {
        return new Stream2<>(toSpliter(Arrays.asList(elements)));
    }

    public <U> Stream2<U> map(Function1<? super T, ? extends U> mapper)
    {
//        Spliterator<U> newSpliterator = new Spliterator<U>() {
//            @Override
//            public boolean tryAdvance(Consumer1<? super U> consumer)
//            {
//                Consumer1<T> newConsumer = new Consumer1<T>() {
//                    @Override
//                    public void accept(T input)
//                    {
//                        return consumer.accept(mapper.apply(input));
//                    }
//                };
//
//                return elements.tryAdvance(newConsumer);
//            }
//        };

        Spliterator<U> newSpliterator = consumer ->
                elements.tryAdvance(input ->
                        consumer.accept(mapper.apply(input)));

        return new Stream2<>(newSpliterator);
    }

    public <U> Stream2<U> flatMap(Function1<? super T, ? extends Stream2<? extends U>> flatMapper)
    {
        Spliterator<U> newSpliterator = new Spliterator<U>() {
            AtomicReference<Spliterator<? extends U>> currentSpliterator = new AtomicReference<>(emptySpliterator());

            @Override
            public boolean tryAdvance(Consumer1<? super U> consumer)
            {
                if (!currentSpliterator.get().tryAdvance(consumer))
                {
                    return elements.tryAdvance(input -> { currentSpliterator.set(flatMapper.apply(input).elements); })
                            && currentSpliterator.get().tryAdvance(consumer);
                }

                return true;
            }
        };

        return new Stream2<>(newSpliterator);
    }

    public Stream2<T> filter(Predicate<? super T> predicate)
    {
        return new Stream2<>(consumer ->
            elements.tryAdvance(input -> {
                if (predicate.test(input))
                {
                    consumer.accept(input);
                }
            }));
    }

    public boolean anyMatch(Predicate<T> predicate)
    {
        boolean[] matched = new boolean[]{false};

        while (!matched[0] && this.elements.tryAdvance(input -> { matched[0] = predicate.test(input); }));

        return matched[0];
    }

    public boolean allMatch(Predicate<T> predicate)
    {
        boolean[] matched = new boolean[]{true};

        while (matched[0] && this.elements.tryAdvance(input -> { matched[0] = predicate.test(input); }));

        return matched[0];
    }

    public Optional<T> findFirst()
    {
        AtomicReference<Optional<T>> first = new AtomicReference<>(Optional.empty());

        this.elements.tryAdvance(input -> { first.set(Optional.of(input)); });

        return first.get();
    }

    public Stream2<T> take(long count)
    {
//        Spliterator<T> newSpliterator = new Spliterator<T>() {
//            long takenSoFar = 0;
//
//            @Override
//            public boolean tryAdvance(Consumer<? super T> consumer)
//            {
//                if (this.takenSoFar < count)
//                {
//                    ++this.takenSoFar;
//                    return elements.tryAdvance(consumer);
//                }
//
//                return false;
//            }
//        };
//
//        return new Stream2<>(newSpliterator);

        long[] takenSoFar = new long[]{0};

        return new Stream2<>(consumer -> {
            if (takenSoFar[0] < count)
            {
                ++takenSoFar[0];
                return elements.tryAdvance(consumer);
            }

            return false;
        });
    }

    public Stream2<T> takeWhile(Predicate<T> predicate)
    {
        boolean[] isTaking = new boolean[]{true};

        return new Stream2<>(consumer -> {
            if (!isTaking[0])
            {
                return false;
            }

            boolean didAdvance = elements.tryAdvance(input -> {
                isTaking[0] = predicate.test(input);

                if (isTaking[0])
                {
                    consumer.accept(input);
                }
            });

            isTaking[0] &= didAdvance;
            return isTaking[0];
        });
    }

    public Stream2<T> drop(long count)
    {
        long[] droppedSoFar = new long[]{0};

        return new Stream2<>(consumer -> {
            for (; droppedSoFar[0] < count; ++droppedSoFar[0])
            {
                elements.tryAdvance(input -> {});
            }

            return elements.tryAdvance(consumer);
        });
    }

    public Stream2<T> dropWhile(Predicate<T> predicate)
    {
        boolean[] isDropping = new boolean[]{true};

        return new Stream2<>(consumer -> {
            while (isDropping[0])
            {
                boolean didAdvance = elements.tryAdvance(input -> {
                    isDropping[0] = predicate.test(input);

                    if (!isDropping[0])
                    {
                        consumer.accept(input);
                    }
                });

                // Stop looping if we stop advancing (there are no elements left to drop).
                isDropping[0] &= didAdvance;

                // If we stop dropping, we either hit our first consumption, or we are out of elements.
                if (!isDropping[0])
                {
                    return didAdvance;
                }
            }

            return elements.tryAdvance(consumer);
        });
    }

    public List<T> collect()
    {
        List<T> result = new ArrayList<>();

        while (this.elements.tryAdvance(result::add));

        return result;
    }

    public <A, R> R collect(Collector<? super T, A, R> collector)
    {
        A acc = collector.supplier().get(); // TODO: rename?
        Consumer2<A, ? super T> accumulator = collector.accumulator();

        while (this.elements.tryAdvance(element -> accumulator.accept(acc, element)));

        return collector.finalizer().apply(acc);
    }

    public void forEach(Consumer1<? super T> consumer)
    {
        while (this.elements.tryAdvance(consumer));
    }

    public static void main(String[] args)
    {
//        Spliterator<Integer> spliter = new Spliterator<Integer>() {
//            int i = 0;
//
//            @Override
//            public boolean tryAdvance(Consumer1<? super Integer> consumer)
//            {
//                if (i < 100)
//                {
//                    consumer.accept(i);
//                    ++i;
//                    return true;
//                }
//
//                return false;
//            }
//        };
//
//        Object result = Stream2.of(spliter)
//                .map(i -> i * 2)
//                .collect(
//                        Collectors.mapping(
//                                i -> i - 1,
//                                Collectors.groupingBy(
//                                        i -> (i / 10) % 10,
//                                        Collectors.groupingBy(
//                                                i -> i < 100 ? 0 : 100,
//                                                Collectors.toList()))));
         Spliterator<List<Integer>> spliter = new Spliterator<List<Integer>>() {
            int i = 0;

            @Override
            public boolean tryAdvance(Consumer1<? super List<Integer>> consumer)
            {
                if (i < 100)
                {
                    consumer.accept(Arrays.asList(new Integer[]{i, i+1, i+2}));
                    ++i;
                    return true;
                }

                return false;
            }
        };

        Object result = Stream2.of(spliter)
                .flatMap(lis -> Stream2.of(lis.get(0), lis.get(1), lis.get(2) * 2))
//                .drop(5)
                .dropWhile(i -> i < 12)
                .takeWhile(i -> i <= 26)
                .drop(5)
                .collect();

        System.out.println(result);
    }
}