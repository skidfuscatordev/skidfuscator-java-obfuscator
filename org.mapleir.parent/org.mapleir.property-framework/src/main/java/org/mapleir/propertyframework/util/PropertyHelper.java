package org.mapleir.propertyframework.util;

import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.impl.BasicPropertyDictionary;
import org.mapleir.propertyframework.impl.BasicSynchronisedPropertyDictionary;
import org.mapleir.propertyframework.impl.FixedStoreDictionary;

import com.google.common.eventbus.EventBus;

public class PropertyHelper {
	
	private static final EventBus PROPERTY_FRAMEWORK_BUS = new EventBus();
	
	public static EventBus getFrameworkBus() {
		return PROPERTY_FRAMEWORK_BUS;
	}

	/**
	 * The dictionary should handle operations from multiple threads safely.
	 * 
	 * @see #createDictionary(IPropertyDictionary)
	 */
	public static final String BASIC_SYNCHRONISED_DICTIONARY_OPT = "dictionary.property.threadsafe";
	/**
	 * The dictionary cannot be modified.
	 * @see #createDictionary(IPropertyDictionary)
	 */
	public static final String IMMUTABLE_DICTIONARY_OPT = "dictionary.property.immutable";

	private static final IPropertyDictionary EMPTY_DICTIONARY = new BasicPropertyDictionary() {
		@Override
		public void put(IProperty<?> property) {
			throw new UnsupportedOperationException("Immutable dictionary");
		}
	};

	public static IPropertyDictionary getImmutableDictionary() {
		return EMPTY_DICTIONARY;
	}
	
	public static IPropertyDictionary makeFixedStoreDictionary(IPropertyDictionary dict) {
		return new FixedStoreDictionary(dict);
	}
	
	public static IPropertyDictionary createDictionary() {
		return createDictionary(null);
	}

	private static boolean __has_opt(IPropertyDictionary settings, String key) {
		IProperty<Boolean> opt = settings.find(Boolean.TYPE, key);
		return opt != null && opt.getValue();
	}

	/**
	 * Finds or creates a dictionary implementation based on the given settings
	 * dictionary. The options that may be taken are defined in
	 * {@link PropertyHelper} as String constants. The required value for the
	 * settings are given with their declarations; a <i>group number</i> is also
	 * defined. Options of the same group are not compatible together, i.e. they
	 * should not be passed together when creating a new dictionary. If options of
	 * the same group are received, the resulting dictionary may not be predictable
	 * or match the expectations of the calling code.
	 * 
	 * @param settings
	 *            A {@link IPropertyDictionary} containing the options declared in
	 *            {@link PropertyHelper}.
	 * @return An implementation of a dictionary, depending on the given settings
	 */
	public static IPropertyDictionary createDictionary(IPropertyDictionary settings) {
		if (settings != null) {
			if (__has_opt(settings, BASIC_SYNCHRONISED_DICTIONARY_OPT)) {
				return new BasicSynchronisedPropertyDictionary();
			} else if (__has_opt(settings, IMMUTABLE_DICTIONARY_OPT)) {
				return EMPTY_DICTIONARY;
			}
		}

		return new BasicPropertyDictionary();
	}

	/**
	 * It is common for a property to have a value of a primitive type, for example,
	 * boolean, however, due to the limitations of Java language generics, the
	 * concrete type argument of it's holding {@link IProperty} must be the wrapper
	 * type (in this example java.lang.Boolean). Therefore the defined type of an
	 * property must be Boolean.class instead of boolean.class or Boolean.TYPE
	 * (which are equivalent). This creates confusion between client code and the
	 * structure of the property framework, making it unintuitive to request
	 * properties of primitive types.
	 * <p>
	 * This helper method is used to take the primitive type and find the
	 * corresponding primitive wrapper type. This allows client code to request a
	 * property with type class of boolean.class (for example) and receive a
	 * property in the form <code>IProperty&lt;Boolean&gt;</code>.
	 * <p>
	 * Note that client code is not expected to use this method to request a
	 * property of the primitive type, instead dictionary implementations should, to
	 * promote easy of use.
	 * 
	 * @param t
	 *            A primitive type (x.class or X.TYPE)
	 * @return The primitive wrapper type or null if there is none for the given
	 *         type
	 */
	public static Class<?> rebasePrimitiveType(Class<?> t) {
		if (t == boolean.class) {
			return Boolean.class;
		} else if (t == int.class) {
			return Integer.class;
		} else if (t == short.class) {
			return Short.class;
		} else if (t == byte.class) {
			return Byte.class;
		} else if (t == long.class) {
			return Long.class;
		} else if (t == float.class) {
			return Float.class;
		} else if (t == double.class) {
			return Double.class;
		} else if (t == char.class) {
			return Character.class;
		}

		return null;
	}
	
	public static boolean isSet(IPropertyDictionary dict, String key) {
		return __has_opt(dict, key);
	}
}
