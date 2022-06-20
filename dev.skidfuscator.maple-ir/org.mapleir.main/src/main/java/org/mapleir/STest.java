package org.mapleir;

import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.impl.BooleanProperty;
import org.mapleir.propertyframework.impl.StringProperty;
import org.mapleir.propertyframework.util.PropertyHelper;
import org.mapleir.serviceframework.api.IServiceContext;
import org.mapleir.serviceframework.api.IServiceFactory;
import org.mapleir.serviceframework.api.IServiceReference;
import org.mapleir.serviceframework.api.IServiceRegistry;
import org.mapleir.serviceframework.util.ServiceHelper;

public class STest {

	public static interface Person {}
	public static abstract class PersonBase implements Person {
		private final String name;
		public PersonBase(String name) {this.name = name;}
		@Override public String toString() {return name + " is a " + getClass().getSimpleName().toLowerCase();}
	}
	public static class Male extends PersonBase {public Male(String name) {super(name);}}
	public static class Female extends PersonBase {public Female(String name) {super(name);}}
	
	public static void main(String[] args) {
		IServiceRegistry reg = ServiceHelper.getGlobalServiceRegistry();
		IServiceContext cxt = ServiceHelper.getGlobalServiceContext();

		IServiceFactory<Person> personFactory = new IServiceFactory<Person>() {
			@Override
			public Person create(IPropertyDictionary dict) {
				IProperty<String> nameProp = dict.find(String.class, "name");
				IProperty<Boolean> genderProp = dict.find(boolean.class, "ismale");
				String name = nameProp.getValue();
				
				if(genderProp.getValue()) {
					return new Male(name);
				} else {
					return new Female(name);
				}
			}
		};
		
		reg.registerServiceFactory(cxt, Person.class, personFactory);
		
		// some other module that doesnt have impl (male/female/base) classes, only Person
		IServiceReference<Person> ref = reg.getServiceReference(cxt, Person.class);
		
		IPropertyDictionary settings = PropertyHelper.createDictionary();
		settings.put(new BooleanProperty(PropertyHelper.BASIC_SYNCHRONISED_DICTIONARY_OPT, true));
		{
			IPropertyDictionary dict = PropertyHelper.createDictionary(settings);
			System.out.println(dict);
			dict.put(new StringProperty("name", "", "bilb"));
			dict.put(new BooleanProperty("ismale", true));
			Person p = reg.getService(ref, dict);
			System.out.println(p);
		}
		
		{
			IPropertyDictionary dict = PropertyHelper.createDictionary();
			System.out.println(dict);
			dict.put(new StringProperty("name", "", "theresa NAY"));
			dict.put(new BooleanProperty("ismale", false));
			Person p = reg.getService(ref, dict);
			System.out.println(p);
		}
		
		reg.ungetService(ref);
	}
}