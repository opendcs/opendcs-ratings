/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package org.opendcs.ratings.io.jdbc;


import org.opendcs.ratings.io.RatingConnectionInfo;
import org.opendcs.ratings.RatingException;

final class JdbcRatingConnectionInfo implements RatingConnectionInfo {

    private final ConnectionProvider connectionProvider;

    JdbcRatingConnectionInfo(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public <T> T getConnectionInfo(Class<T> type) throws RatingException {
        if(type.isAssignableFrom(ConnectionProvider.class)) {
            return (T) connectionProvider;
        } else {
            throw new RatingException("getClass() does not support connection provider type: " + type);
        }
    }
}
