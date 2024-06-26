/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.utilities.java.support.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.Beta;

/**
 * Map implementation which allows subsets of entries to be retrieved based on the type of the entry value.
 * 
 * @param <KeyType> the type of object used as keys
 * @param <ValueType> the type of object stored as values
 */
@Beta
public class ValueTypeIndexedMap<KeyType, ValueType> implements Map<KeyType, ValueType> {

    /** Class to represent null values. */
    private static class NullValue {}

    /** Storage for index of class -&gt; members. */
    private Map<Class<?>, Map<KeyType, ValueType>> index;

    /** Storage for map members. */
    private Map<KeyType, ValueType> map;

    /** Set of valid types for this map. */
    private Set<Class<?>> types;

    /** Constructor. */
    public ValueTypeIndexedMap() {
        this(new HashSet<Class<?>>());
    }

    /**
     * Constructor.
     * 
     * @param newMap existing map to build from.
     * @param newTypes collection of value types to index
     */
    public ValueTypeIndexedMap(final Map<KeyType, ValueType> newMap, final Collection<Class<?>> newTypes) {
        map = newMap;
        types = new HashSet<>(newTypes);
        index = new HashMap<>();
        rebuildIndex();
    }

    /**
     * Constructor.
     * 
     * @param newTypes collection of value types to index
     */
    public ValueTypeIndexedMap(final Collection<Class<?>> newTypes) {
        this(new HashMap<KeyType, ValueType>(), newTypes);
    }

    /** {@inheritDoc} */
    public void clear() {
        map.clear();
        rebuildIndex();
    }

    /** {@inheritDoc} */
    public boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    /** {@inheritDoc} */
    public boolean containsValue(final Object value) {
        return map.containsValue(value);
    }

    /** {@inheritDoc} */
    public Set<java.util.Map.Entry<KeyType, ValueType>> entrySet() {
        return map.entrySet();
    }

    /** {@inheritDoc} */
    public ValueType get(final Object key) {
        return map.get(key);
    }

    /**
     * Get the value types that are indexed.
     * 
     * @return which value types are indexed
     */
    public Set<Class<?>> getTypes() {
        return types;
    }

    /** {@inheritDoc} */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /** {@inheritDoc} */
    public Set<KeyType> keySet() {
        return map.keySet();
    }

    /**
     * Check if the object is of the specified type, taking null into account as well.
     * 
     * @param type type to check for
     * @param object object to check
     * @return true if the object is of the specified type
     */
    private Boolean matchType(final Class<?> type, final Object object) {
        return type.isInstance(object) || (type == NullValue.class && object == null);
    }

    /** {@inheritDoc} */
    public ValueType put(final KeyType key, final ValueType value) {
        final ValueType oldValue = map.put(key, value);

        for (final Class<?> clazz : index.keySet()) {
            final Class<?> type;
            if (clazz == null) {
                type = NullValue.class;
            } else {
                type = clazz;
            }

            if (matchType(type, value)) {
                index.get(type).put(key, value);
            } else if (matchType(type, oldValue)) {
                index.get(type).remove(key);
            }
        }

        return oldValue;
    }

    /** {@inheritDoc} */
    public void putAll(final Map<? extends KeyType, ? extends ValueType> t) {
        // this is probably not the most efficient way to do this
        for (final KeyType key : t.keySet()) {
            put(key, t.get(key));
        }
    }

    /**
     * Rebuild internal index.
     */
    public void rebuildIndex() {
        index.clear();
        ValueType value;

        for (final Class<?> clazz : types) {
            final Class<?> type;
            if (clazz == null) {
                type = NullValue.class;
            } else {
                type = clazz;
            }

            index.put(type, new HashMap<KeyType, ValueType>());
            for (final KeyType key : map.keySet()) {
                value = map.get(key);
                if (matchType(type, value)) {
                    index.get(type).put(key, value);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public ValueType remove(final Object key) {
        final ValueType value = map.remove(key);

        for (final Class<?> type : index.keySet()) {
            if (type.isInstance(value)) {
                index.get(type).remove(key);
            }
        }

        return value;
    }

    /**
     * Set which value types are indexed.
     * 
     * @param newTypes which value types are indexed
     */
    public void setTypes(final Collection<Class<?>> newTypes) {
        types = new HashSet<>(newTypes);
    }

    /** {@inheritDoc} */
    public int size() {
        return map.size();
    }

    /**
     * Returns an unmodifiable map of the entries whose value is of the specified type.
     * 
     * @param <SubType> type of values to include in the returned map
     * @param type type of values to return
     * @return sub map of entries whose value is of type SubType or empty if the specified type is not a valid type for
     *         this map.
     */
    @SuppressWarnings("unchecked")
    public <SubType extends ValueType> Map<KeyType, SubType> subMap(final Class<SubType> type) {
        Class<?> key = type;
        if (key == null) {
            key = NullValue.class;
        }
        if (index.containsKey(key)) {
            return Collections.unmodifiableMap((Map<KeyType, SubType>) index.get(key));
        }
        return Collections.emptyMap();
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return map.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        return map.equals(((ValueTypeIndexedMap<?,?>) obj).map);
    }
    
    /** {@inheritDoc} */
    public String toString() {
        return map.toString();
    }

    /** {@inheritDoc} */
    public Collection<ValueType> values() {
        return map.values();
    }
}