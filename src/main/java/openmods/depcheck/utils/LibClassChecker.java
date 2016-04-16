package openmods.depcheck.utils;

import java.util.Arrays;
import java.util.Queue;

import com.google.common.base.Throwables;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Queues;
import com.google.common.collect.Table;

public class LibClassChecker {

    private static final Table<String, TypedElement, Boolean> cache = HashBasedTable.create();

    private static boolean findElementInClass(String clsName, TypedElement element) {
        final Queue<Class<?>> classes = Queues.newArrayDeque();

        try {
            classes.add(Class.forName(clsName));
        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }

        while (!classes.isEmpty()) {
            final Class<?> cls = classes.poll();
            if (element.isInClass(cls))
                return true;

            {
                final Class<?> superClass = cls.getSuperclass();
                if (superClass != null)
                    classes.add(superClass);
            }

            classes.addAll(Arrays.asList(cls.getInterfaces()));
        }
        return false;
    }

    public static boolean isElementInClass(String clsName, TypedElement element) {
        Boolean result = cache.get(clsName, element);
        if (result != null)
            return result;

        result = findElementInClass(clsName, element);
        cache.put(clsName, element, result);
        return result;
    }

}
