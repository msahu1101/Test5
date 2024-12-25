package com.mgmresorts.booking.room.reservation.search.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.mgmresorts.booking.room.reservation.search.config.AppProperties;
import com.mgmresorts.booking.room.reservation.search.dao.DocumentDao;
import com.mgmresorts.booking.room.reservation.search.dao.DocumentDaoImpl;
import com.mgmresorts.booking.room.reservation.search.dao.OperaDao;
import com.mgmresorts.booking.room.reservation.search.dao.ReservationRepository;
import com.mgmresorts.booking.room.reservation.search.dao.ReservationRepositoryImpl;
import com.mgmresorts.booking.room.reservation.search.feign.FeignEncoder;
import com.mgmresorts.booking.room.reservation.search.feign.FeignErrorDecoder;
import com.mgmresorts.booking.room.reservation.search.service.OperaTokenService;
import com.mgmresorts.booking.room.reservation.search.service.OperaTokenServiceImpl;
import com.mgmresorts.booking.room.reservation.search.service.ReservationService;
import com.mgmresorts.booking.room.reservation.search.service.ReservationServiceImpl;
import com.mgmresorts.booking.room.reservation.search.service.SearchService;
import com.mgmresorts.booking.room.reservation.search.service.SearchServiceImpl;

import feign.Feign;
import feign.Logger.Level;
import feign.gson.GsonDecoder;

/***
 * Guice Injector Class for loading configuration properties and binding
 * services to implementations.
 *
 */
public class ApplicationInjector extends AbstractModule {

	/**
	 * Configures the Guice Bindings.
	 */
	@Override
	protected void configure() {

		AppProperties appProperties = new AppProperties();

		OperaDao operaDao = Feign.builder().logLevel(Level.FULL).encoder(new FeignEncoder()).decoder(new GsonDecoder())
				.errorDecoder(new FeignErrorDecoder()).target(OperaDao.class, appProperties.getOperaCloudUrl());

		bind(AppProperties.class).toInstance(appProperties);
		bind(OperaDao.class).toInstance(operaDao);
		bind(ReservationRepository.class).to(ReservationRepositoryImpl.class).in(Scopes.SINGLETON);
		bind(DocumentDao.class).to(DocumentDaoImpl.class).in(Scopes.SINGLETON);
		bind(SearchService.class).to(SearchServiceImpl.class).in(Scopes.SINGLETON);
		bind(ReservationService.class).to(ReservationServiceImpl.class).in(Scopes.SINGLETON);
		bind(OperaTokenService.class).to(OperaTokenServiceImpl.class).in(Scopes.SINGLETON);
	}
}
