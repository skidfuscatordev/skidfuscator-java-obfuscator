package org.mapleir.propertyframework.api;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.eventbus.EventBus;

/**
 * This class acts as the generic base of the property framework. The
 * functionality is similar to that of the {@link Map} interface in the standard
 * Java collections library, but extra methods for increased type consistency
 * are added here. This interface, along with {@link IProperty} are designed to
 * be the only exposed classes to client applications, with no references to the
 * underlying implementations of either.
 * <p>
 * Dictionaries must make sure that they do not capture a property that another
 * dictionary is currently holding, as there should only ever be at most one
 * dictionary associated with a property.
 * 
 * @author Bibl
 */
public interface IPropertyDictionary extends Iterable<Entry<String, IProperty<?>>> {

	/**
	 * Performs some sort of lookup or traversal of values to find the
	 * {@link IProperty} that is associated with the String key.
	 * <p>
	 * Note that although this can look up the property, it does not perform any
	 * form of type checking or type consistency checking, making the result
	 * potentially dangerous.
	 * 
	 * @see #find(Class, String)
	 * @param key
	 *            The key associated with the requested property.
	 * @return The property
	 */
	<T> IProperty<T> find(String key);

	/**
	 * Performs of a lookup identical to that of {@link #find(String)} except the
	 * resulting {@link IProperty} must be typechecked against the input to ensure
	 * that the property's value can be casted to the received type.
	 * <p>
	 * Note that the specification for {@link IProperty} ensures that the type
	 * declared by the property properly represents the type of it's underlying
	 * value.
	 * <p>
	 * <b>The implementation is required to not throw an exception if the type does
	 * not match the property's type</b> but it is acceptable for it to throw if it
	 * receives other erroneous inputs or encounters exceptional behaviour.
	 * 
	 * @see #find(String)
	 * @param type
	 *            The type that the property must be checked against. It may be the
	 *            actual property type or a covariant mutation that is desired by
	 *            the application code.
	 * @param key
	 *            The key associated with the requested property.
	 * @return The typechecked property or null
	 */
	<T> IProperty<T> find(Class<T> type, String key);

	/**
	 * Puts the associated {@link IProperty} into the dictionary's internal storage.
	 * The default behaviour uses the property's key as the store key, however, this
	 * can be changed. Note that this is also the recommended behaviour, but
	 * dictionaries not need to comply with this.
	 * 
	 * @see #put(String, IProperty)
	 * @param property
	 *            The property
	 */
	default void put(IProperty<?> property) {
		if (property != null) {
			put(property.getKey(), property);
		} else {
			throw new NullPointerException("Null property");
		}
	}

	/**
	 * Stores the given {@link IProperty} in the dictionary's internal storage. The
	 * given property may or may not be mapped depending on the current state of the
	 * dictionary and/or property.
	 * 
	 * @see #put(IProperty)
	 * @param key
	 *            The key associated with the requested property.
	 * @param property
	 *            The property
	 */
	void put(String key, IProperty<?> property);

	EventBus getContainerEventBus();
}