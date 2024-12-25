package com.mgmresorts.booking.room.reservation.search.inject;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * InjectionContext for Dependecy-injection and bean instantiation.
 */
public final class InjectionContext {

	/**
	 * InjectionContext.
	 */
	private static InjectionContext context;
	/**
	 * Injector.
	 */
	private static Injector injector;

	/**
	 * Get InjectionContext instance, repeated calls returns same singleton
	 * instance.
	 * 
	 * @return
	 */
	public static synchronized InjectionContext get() {
		if (context == null) {
			context = new InjectionContext(Guice.createInjector(new ApplicationInjector()));
		}
		return context;
	}

	/**
	 * Initialize InjectionContext.
	 * 
	 * @param inputInjector
	 */
	private InjectionContext(final Injector inputInjector) {
		injector = inputInjector;
	}

	/**
	 * Override injection-context instance.
	 * 
	 * @param inputInjector
	 * @return
	 */
	public static InjectionContext override(final Injector inputInjector) {
		context = new InjectionContext(inputInjector);
		return context;
	}

	/**
	 * Get instance of bean from InjectionContext, returns null if not found.
	 * 
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	public <T> T instanceOf(final Class<T> clazz) {
		return injector.getInstance(clazz);
	}

	/**
	 * Get instance of bean with bean-name from InjectionContext, 
	 * else return null.
	 * 
	 * @param <T>
	 * @param clazz
	 * @param beanName
	 * @return
	 */
	public static <T> T instanceOf(final Class<T> clazz, final String beanName) {
		final Key<T> nameKey = Key.get(clazz, Names.named(beanName));
		final Binding<T> binding = injector.getExistingBinding(nameKey);
		if (binding != null) {
			return binding.getProvider().get();
		}
		return null;
	}
}
