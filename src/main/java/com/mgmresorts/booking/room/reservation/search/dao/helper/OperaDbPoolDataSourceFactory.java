package com.mgmresorts.booking.room.reservation.search.dao.helper;

import java.time.Duration;

import com.mgmresorts.booking.common.error.exception.SystemException;
import com.mgmresorts.booking.room.reservation.search.config.AppProperties;
import com.mgmresorts.booking.room.reservation.search.util.ServiceConstants;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

@UtilityClass
@Log4j2
public class OperaDbPoolDataSourceFactory {

	public static PoolDataSource getPoolDataSource(AppProperties appConfigs) throws SystemException {

		try {
			log.debug("Creating new Opera DB connection pool for Search");

			PoolDataSource poolDataSource = PoolDataSourceFactory.getPoolDataSource();
			poolDataSource.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
			poolDataSource.setUser(System.getenv(ServiceConstants.OPERA_DB_USER_NAME));
			poolDataSource.setPassword(System.getenv(ServiceConstants.OPERA_DB_USER_PASSWORD));
			poolDataSource.setURL(System.getenv(ServiceConstants.OPERA_DB_URL));
			poolDataSource.setMinPoolSize(appConfigs.getDbMinPoolSize());
			poolDataSource.setMaxPoolSize(appConfigs.getDbMaxPoolSize());
			poolDataSource.setInitialPoolSize(appConfigs.getDbInitialPoolSize());
			poolDataSource.setMaxConnectionReuseCount(appConfigs.getDbMaxConnectionReuseCount());
			poolDataSource.setAbandonedConnectionTimeout(appConfigs.getDbAbandonedConnectionTimeoutInSec());
			poolDataSource.setTimeToLiveConnectionTimeout(appConfigs.getDbTimeToLiveConnectionTimeoutInSec());
			poolDataSource.setConnectionWaitDuration(Duration.ofSeconds(appConfigs.getDbConnectionWaitTimeoutInSec()));
			poolDataSource.setInactiveConnectionTimeout(appConfigs.getDbInactiveConnectionTimeoutInSec());
			poolDataSource.setQueryTimeout(appConfigs.getDbQueryTimeoutInSec());
			poolDataSource.setValidateConnectionOnBorrow(true);
			poolDataSource.setSecondsToTrustIdleConnection(appConfigs.getDbSecondsToTrustIdleConnection());
			poolDataSource.setTimeoutCheckInterval(appConfigs.getDbTimeoutCheckInterval());
			poolDataSource.setConnectionPoolName("OCRS_Search_conn_pool");

			log.info("Opera DB connection pool for Search created successfully");
			return poolDataSource;
		}
		catch (Exception e){
			log.error("Error in Opera DB connection pool for Search creation: {}", e.getMessage());
			throw new SystemException("Error in Opera DB connection pool for Search creation", e);
		}
	}
}