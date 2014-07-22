/*
 * Copyright 2014 the original author or authors.
 * Copyright (C) 2010 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package net.openhft.collect;

import com.google.auto.value.AutoValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;


/**
 * A strategy for determining whether two instances are considered equivalent.
 *
 * <p>This class is inspired and very similar to
 * <a href="http://docs.guava-libraries.googlecode.com/git-history/release/javadoc/com/google/common/base/Equivalence.html">
 * Guava's {@code Equivalence}</a>, with two notable differences.
 *
 * <p>First, this {@code Equivalence}
 * is consistently nullable, i. e. when {@code null} is passed instead of {@code Equivalence}
 * instance anywhere in the library, default Java equality is assumed (see
 * {@link Object#equals(Object)} and {@link Object#hashCode()}). In Guava there is a special
 * instance for such equivalence.
 *
 * <p>Second, this {@code Equivalence} forces the actual implementation to override
 * {@link #equals(Object)} and {@link #hashCode()}. Notice these are {@code Equivalence}'s own
 * equals and hashCode, not the strategy {@link #equivalent(Object, Object)} and
 * {@link #hash(Object)} methods. It is needed because, for example, {@link ObjCollection}'s
 * equality depends on {@code Equivalence} equality.
 *
 * <p>In most cases, when {@code Equivalence} is stateless, you can extend
 * {@link StatelessEquivalence} not to bother with implementing these methods. See examples
 * in the documentation to {@link #identity() identity} and
 * {@link #caseInsensitive() case insensitive} equivalences.
 *
 * @param <T> type of objects compared by this equivalence
 */
public abstract class Equivalence<T> {

    /**
     * Returns the equivalence that uses {@code ==} to compare objects and
     * {@link System#identityHashCode(Object)} to compute the hash code.
     * {@link Equivalence#nullableEquivalent} returns {@code true} if {@code a == b}, including
     * in the case when {@code a} and {@code b} are both {@code null}.
     *
     * <p>This equivalence could be implemented as follows:
     * <pre>{@code
     * final class Identity extends StatelessEquivalence&lt;Object&gt; {
     *     static final Identity INSTANCE = new Identity();
     *     private Identity() {}
     *     &#064;Override
     *     public boolean equivalent(@Nonnull Object a, @Nonnull Object b) {
     *         return a == b;
     *     }
     *     &#064;Override
     *     public int hash(@Nonnull Object t) {
     *         return System.identityHashCode(t);
     *     }
     * }
     * }</pre>
     *
     * @return the identity equivalence
     */
    @Nonnull
    public static Equivalence<Object> identity() {
        return IDENTITY;
    }

    /**
     * Returns the {@link String} equivalence that uses {@link String#equalsIgnoreCase} to compare
     * strings.
     *
     * <p>This equivalence could be implemented as follows:
     * <pre>{@code
     * final class CaseInsensitive extends StatelessEquivalence&lt;String&gt; {
     *     static final CaseInsensitive INSTANCE = new CaseInsensitive();
     *     private CaseInsensitive() {}
     *     &#064;Override
     *     public boolean equivalent(@Nonnull String a, @Nonnull String b) {
     *         return a.equalsIgnoreCase(b);
     *     }
     *     &#064;Override
     *     public int hash(@Nonnull String s) {
     *         return s.toLowerCase().hashCode();
     *     }
     * }
     * }</pre>
     *
     * @return the case-insensitive {@link String} equivalence
     */
    @Nonnull
    public static Equivalence<String> caseInsensitive() {
        return CASE_INSENSITIVE;
    }

    /**
     * Returns a {@link java.util.Map.Entry} equivalence for the given key and value equivalences.
     *
     * @param keyEquivalence the entry key equivalence
     * @param valueEquivalence the entry value equivalence
     * @param <K> the entry key type
     * @param <V> the entry value type
     * @return a {@link java.util.Map.Entry} equivalence for the given key and value equivalences
     */
    @Nullable
    public static <K, V> Equivalence<Map.Entry<K, V>> entryEquivalence(
            @Nullable Equivalence<K> keyEquivalence, @Nullable Equivalence<V> valueEquivalence) {
        if (keyEquivalence == null && valueEquivalence == null)
            return null;
        return new AutoValue_Equivalence_EntryEquivalence<K, V>(keyEquivalence, valueEquivalence);
    }

    @AutoValue
    static abstract class EntryEquivalence<K, V> extends Equivalence<Map.Entry<K, V>> {

        @Nullable
        abstract Equivalence<K> keyEquivalence();

        @Nullable
        abstract Equivalence<V> valueEquivalence();

        @Override
        public boolean equivalent(@Nonnull Map.Entry<K, V> a, @Nonnull Map.Entry<K, V> b) {
            if (a == b)
                return true;
            K aKey = a.getKey();
            K bKey = b.getKey();
            Equivalence<K> keyEquivalence = keyEquivalence();
            if (keyEquivalence == null) {
                if (!NullableObjects.equals(aKey, bKey))
                    return false;
            } else if (!keyEquivalence.nullableEquivalent(aKey, bKey)) {
                return false;
            }
            V aVal = a.getValue();
            V bVal = b.getValue();
            Equivalence<V> valueEquivalence = valueEquivalence();
            if (valueEquivalence == null) {
                return NullableObjects.equals(aVal, bVal);
            } else {
                return valueEquivalence.nullableEquivalent(aVal, bVal);
            }
        }

        @Override
        public int hash(@Nonnull Map.Entry<K, V> entry) {
            Equivalence<K> keyEquivalence = keyEquivalence();
            int keyHash = keyEquivalence == null ?
                    NullableObjects.hashCode(entry.getKey()) :
                    keyEquivalence.nullableHash(entry.getKey());
            Equivalence<V> valueEquivalence = valueEquivalence();
            int valHash = valueEquivalence == null ?
                    NullableObjects.hashCode(entry.getValue()) :
                    valueEquivalence.nullableHash(entry.getValue());
            return keyHash ^ valHash;
        }
    }

    private static final Equivalence<Object> IDENTITY = new Identity();

    private static class Identity extends StatelessEquivalence<Object> {

        @Override
        public boolean nullableEquivalent(@Nullable Object a, @Nullable Object b) {
            return a == b;
        }

        @Override
        public boolean equivalent(@Nonnull Object a, @Nonnull Object b) {
            return a == b;
        }

        @Override
        public int nullableHash(@Nullable Object o) {
            return System.identityHashCode(o);
        }

        @Override
        public int hash(@Nonnull Object o) {
            return System.identityHashCode(o);
        }
    }

    private static final Equivalence<String> CASE_INSENSITIVE = new CaseInsensitive();

    private static class CaseInsensitive extends StatelessEquivalence<String> {

        @Override
        public boolean equivalent(@Nonnull String a, @Nonnull String b) {
            return a.equalsIgnoreCase(b);
        }

        @Override
        public int hash(@Nonnull String s) {
            return s.toLowerCase().hashCode();
        }
    }

    /**
     * Constructor for use by subclasses.
     */
    protected Equivalence() {}

    /**
     * Returns {@code true} if {@code a} and {@code b} are considered equivalent,
     * {@code false} otherwise. {@code a} and {@code b} both might be {@code null}.
     *
     * <p>If the implementation overrides this method, it <em>must</em> ensure that it returns
     * {@code true} if both the given objects are nulls and {@code false}, if only one of them
     * is {@code null}. If both {@code a} and {@code b} are non-null, this method should perform
     * just the same as {@link #equivalent(Object, Object)} method does.
     *
     * @param a the first object to compare
     * @param b the second object to compare
     * @return {@code true} if {@code a} and {@code b} are considered equivalent,
     *         {@code false} otherwise
     */
    public boolean nullableEquivalent(@Nullable T a, @Nullable T b) {
        return a == b || (a != null && b != null && equivalent(a, b));
    }

    /**
     * Returns {@code true} if {@code a} and {@code b} are considered equivalent,
     * {@code false} otherwise. {@code a} and {@code b} are assumed to be non-null.
     *
     * <p>This method implements an equivalence relation on object references:
     *
     * <ul>
     * <li>It is <i>reflexive</i>: for any reference {@code x}, {@code equivalent(x, x)}
     *     returns {@code true}.
     * <li>It is <i>symmetric</i>: for any references {@code x} and {@code y},
     *     {@code equivalent(x, y) == equivalent(y, x)}.
     * <li>It is <i>transitive</i>: for any references {@code x}, {@code y}, and {@code z},
     *     if {@code equivalent(x, y)} returns {@code true} and {@code equivalent(y, z)} returns
     *     {@code true}, then {@code equivalent(x, z)} returns {@code true}.
     * <li>It is <i>consistent</i>: for any references {@code x} and {@code y}, multiple invocations
     *     of {@code equivalent(x, y)} consistently return {@code true} or consistently return
     *     {@code false} (provided that neither {@code x} nor {@code y} is modified).
     * </ul>
     *
     * <p>This method is called by {@link #nullableEquivalent(Object, Object)}.
     *
     * @param a the first object to compare
     * @param b the second object to compare
     * @return {@code true} if {@code a} and {@code b} are considered equivalent,
     *         {@code false} otherwise
     */
    public abstract boolean equivalent(@Nonnull T a, @Nonnull T b);

    /**
     * Returns a hash code for the given object. The {@code t} object might be {@code null}.
     *
     * <p>If the implementation overrides this method, it <em>must</em> ensure that it returns
     * {@code 0} if the given object is {@code null}. Otherwise this method should perform just
     * the same as {@link #hash(Object)} method does.
     *
     * @param t the object to compute hash code for
     * @return a hash code for the given object
     */
    public int nullableHash(@Nullable T t) {
        return t != null ? hash(t) : 0;
    }

    /**
     * Returns a hash code for the given object. The {@code t} object is assumed to be non-null.
     *
     * <p>This method has the following properties:
     * <ul>
     * <li>It is <i>consistent</i>: for any reference {@code x}, multiple invocations of
     *     {@code hash(x)} consistently return the same value provided {@code x} remains unchanged
     *     according to the definition of the equivalence. The hash need not remain consistent from
     *     one execution of an application to another execution of the same application.
     * <li>It is <i>distributable across equivalence</i>: for any references {@code x} and
     *     {@code y}, if {@code equivalent(x, y)}, then {@code hash(x) == hash(y)}. It is <i>not</i>
     *     necessary that the hash be distributable across <i>inequivalence</i>.
     *     If {@code equivalence(x, y)} is false, {@code hash(x) == hash(y)} may still be true.
     * </ul>
     *
     * <p>This method is called by {@link #nullableHash(Object)}.
     *
     * @param t the object to compute hash code for
     * @return a hash code for the given object
     */
    public abstract int hash(@Nonnull T t);

    /**
     * {@inheritDoc}
     *
     * This method is made {@code abstract} to force the final implementation to override it.
     * It is needed because, for example, {@link net.openhft.collect.map.ObjObjMap}'s equality
     * depends on key and value {@code Equivalence} equality.
     */
    @Override
    public abstract boolean equals(Object o);

    /**
     * {@inheritDoc}
     *
     * This method is made {@code abstract} to force the final implementation to override it.
     * It is needed because, {@link #equals(Object)} is needed to be overridden, and in Java
     * {@link Object#equals(Object)} and {@link Object#hashCode()} should <em>always</em>
     * be overridden simultaneously.
     */
    @Override
    public abstract int hashCode();
}