package custom.streams;

import java.util.*;

public abstract class Collectors
{
    private Collectors() {}

    public static <T> Collector<T, ?, List<T>> toList()
    {
        return Collector.<T, List<T>, List<T>>of(
                () -> new ArrayList<>(),
                (lis, el) -> { lis.add(el); },
                (lis1, lis2) -> { lis1.addAll(lis2); return lis1; },
                (lis) -> lis
        );
    }

    public static <T> Collector<T, ?, Set<T>> toSet()
    {
        return Collector.<T, Set<T>, Set<T>>of(
                () -> new HashSet<>(),
                (set, el) -> { set.add(el); },
                (set1, set2) -> { set1.addAll(set2); return set1; },
                (set) -> set
        );
    }

    public static <T, U, A, R> Collector<T, ?, R> mapping(
            Function1<? super T, ? extends U> mapper,
            Collector<? super U, A, R> downstream)
    {
        Consumer2<A, ? super U> accumulator = downstream.accumulator();

        return Collector.of(
                downstream.supplier(),
                (acc, el) -> accumulator.accept(acc, mapper.apply(el)),
                downstream.combiner(),
                downstream.finalizer()
        );
    }

    public static <T, U, A, R> Collector<T, ?, R> flatMapping(
            Function1<? super T, ? extends Stream2<? extends U>> flatMapper,
            Collector<? super U, A, R> downstream)
    {
        Consumer2<A, ? super U> accumulator = downstream.accumulator();

        return Collector.of(
                downstream.supplier(),
                (acc, el) -> flatMapper.apply(el).forEach(subEl -> accumulator.accept(acc, subEl)),
                downstream.combiner(),
                downstream.finalizer()
        );
    }

    public static <T, K, A, V> Collector<T, ?, Map<K, V>> groupingBy(
            Function1<? super T, ? extends K> classifier,
            Collector<? super T, A, V> downstream)
    {
        Supplier<A> supplier = downstream.supplier();
        Consumer2<A, ? super T> accumulator = downstream.accumulator();
        Function2<A, A, A> combiner = downstream.combiner();
        Function1<A, V> finalizer = downstream.finalizer();

        return Collector.of(
                () -> new HashMap<K, A>(),
                (map, el) -> {
                    K key = classifier.apply(el);

                    if (!map.containsKey(key))
                    {
                        map.put(key, supplier.get());
                    }

                    accumulator.accept(map.get(key), el);
                },
                (map1, map2) -> {
                    for (K key : map2.keySet())
                    {
                        if (map1.containsKey(key))
                        {
                            map1.put(key, combiner.apply(map1.get(key), map2.get(key)));
                        }
                        else
                        {
                            map1.put(key, map2.get(key));
                        }
                    }

                    return map1;
                },
                (map) -> {
                    Map<K, V> finalMap = new HashMap<>();

                    for (Map.Entry<K, A> entry : map.entrySet())
                    {
                        finalMap.put(entry.getKey(), finalizer.apply(entry.getValue()));
                    }

                    return finalMap;
                }
        );
    }
}
