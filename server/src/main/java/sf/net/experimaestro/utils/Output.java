/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

package sf.net.experimaestro.utils;

import sf.net.experimaestro.utils.iterators.AbstractIterator;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;


/**
 * Output utilities
 *
 * @author bpiwowar
 */
public class Output {

    private static final class NullFormatter<T> implements Formatter<T> {
        public String format(T t) {
            return t.toString();
        }
    }

    static public interface Formatter<T> {
        String format(T t);
    }

    static public <T> void print(PrintStream out, String separator,
                                 Iterable<T> iterable) {
        print(out, separator, iterable.iterator(), new NullFormatter<T>());
    }


    public static void print(PrintWriter out, String separator,
                             final double[] array) {
        print(out, separator, new AbstractIterator<Double>() {
            int i = 0;

            @Override
            protected boolean storeNext() {
                if (i < array.length) {
                    value = array[i];
                    i++;
                    return true;
                }
                return false;
            }
        }, new NullFormatter<Double>());
    }

    static public <T, U extends Iterator<T>> void print(PrintWriter out, String separator,
                                                        U iterator, Formatter<T> formatter) {
        boolean first = true;
        while (iterator.hasNext()) {
            T t = iterator.next();
            if (first)
                first = false;
            else
                out.print(separator);
            out.print(formatter.format(t));
        }
    }

    /**
     * @param out
     * @param separator
     * @param array
     */
    public static void print(PrintStream out, String separator,
                             final double[] array) {
        print(out, separator, new AbstractIterator<Double>() {
            int i = 0;

            @Override
            protected boolean storeNext() {
                if (i < array.length) {
                    value = array[i];
                    i++;
                    return true;
                }
                return false;
            }
        }, new NullFormatter<Double>());
    }

    static public <T, U extends Iterator<T>> void print(PrintStream out, String separator,
                                                        U iterator, Formatter<T> formatter) {
        boolean first = true;
        while (iterator.hasNext()) {
            T t = iterator.next();
            if (first)
                first = false;
            else
                out.print(separator);
            out.print(formatter.format(t));
        }
    }

    static public <T, U extends Iterable<T>> void print(PrintStream out,
                                                        String separator, U iterable, Formatter<T> formatter) {
        print(out, separator, iterable.iterator(), formatter);
    }

    static public <T> void print(PrintWriter out, String separator,
                                 Iterable<T> iterable) {
        print(out, separator, iterable, new Formatter<T>() {
            public String format(T t) {
                return t.toString();
            }

        });
    }

    static public <T, U extends Iterable<T>> void print(PrintWriter out,
                                                        String separator, U iterable, Formatter<T> formatter) {
        boolean first = true;
        for (T t : iterable) {
            if (first)
                first = false;
            else
                out.print(separator);
            out.print(formatter.format(t));
        }
    }

    /**
     * @param buffer
     * @param string
     * @param x
     */
    public static void print(StringBuilder builder, String separator,
                             Iterable<?> iterable) {
        boolean first = true;
        for (Object t : iterable) {
            if (first)
                first = false;
            else
                builder.append(separator);
            builder.append(t);
        }
    }

    /**
     * @param buffer
     * @param string
     * @param x
     */
    public static <U> void print(StringBuilder builder, String separator,
                                 U[] iterable) {
        boolean first = true;
        for (Object t : iterable) {
            if (first)
                first = false;
            else
                builder.append(separator);
            builder.append(t);
        }
    }

    /**
     * @param buffer
     * @param string
     * @param x
     */
    public static void print(StringBuilder builder, String separator,
                             int[] iterable) {
        boolean first = true;
        for (Object t : iterable) {
            if (first)
                first = false;
            else
                builder.append(separator);
            builder.append(t);
        }
    }

    /**
     * @param buffer
     * @param string
     * @param x
     */
    static public <T, U extends Iterable<T>> void print(StringBuilder builder,
                                                        String separator, U iterable, Formatter<T> formatter) {
        boolean first = true;
        for (T t : iterable) {
            if (first)
                first = false;
            else
                builder.append(separator);
            builder.append(formatter.format(t));
        }
    }

    public static <U> String toString(String separator, U[] iterable, Formatter<U> formatter) {
        StringBuilder sb = new StringBuilder();
        print(sb, separator, Arrays.asList(iterable), formatter);
        return sb.toString();

    }

        /**
         * @param string
         * @param actions
         * @return
         */
    public static <U> String toString(String separator, Iterable<U> iterable) {
        StringBuilder sb = new StringBuilder();
        print(sb, separator, iterable);
        return sb.toString();
    }

    /**
     * @param string
     * @param actions
     * @return
     */
    public static <U> String toString(String separator, U[] iterable) {
        StringBuilder sb = new StringBuilder();
        print(sb, separator, iterable);
        return sb.toString();
    }

    public static <T, U extends Iterable<T>> String toString(String separator,
                                                             U iterable, Formatter<T> formatter) {
        StringBuilder sb = new StringBuilder();
        print(sb, separator, iterable, formatter);
        return sb.toString();
    }

}
