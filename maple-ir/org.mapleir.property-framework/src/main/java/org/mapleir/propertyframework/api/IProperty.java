package org.mapleir.propertyframework.api;

/**
 * This is one of the fundamental interfaces for the property framework. It
 * contains information regarding the extensible and type consistent properties.
 * Properties are used along with an {@link IPropertyDictionary} to provide
 * functionality to other modules and systems that can take user specified
 * inputs.
 * <p>
 * A key concept of the property framework is type consistency; that is the
 * concrete type argument when a property value is declared or instantiated
 * should <b>always</b> be representative of the underlying type of the value in
 * the property. This allows client code to interact with the framework
 * intuitively and safely. To enable this, the property implementations must
 * declare a <code>getType</code> method, which is used throughout the framework
 * for consistency checking. Simply put, a property of parameterised type T,
 * must be return T.class for getType and the value of the property must be
 * assignable as a variable of type T.
 * <p>
 * A property may only be 'held' by one dictionary at a time. This restriction
 * is imposed so that complicated parent-child relations do not develop when
 * managing properties and dictionaries, allowing for safer and easier use of
 * the framework.
 * 
 * @see IPropertyDictionary
 * @author Bibl
 * @param <T>
 *            The internal type of the property as described earlier
 */
public interface IProperty<T> {

	String getKey();

	Class<T> getType();

	IPropertyDictionary getDictionary();
	
	void tryLinkDictionary(IPropertyDictionary dict);

	T getValue();

	void setValue(T t);
	
	IProperty<T> clone(IPropertyDictionary newDict);
}