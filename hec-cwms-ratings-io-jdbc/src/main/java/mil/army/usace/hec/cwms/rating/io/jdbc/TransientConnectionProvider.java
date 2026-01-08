/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.jdbc;


import java.sql.Connection;

final class TransientConnectionProvider implements ConnectionProvider {

    private final Connection connection;

    TransientConnectionProvider(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Connection getConnection() throws RatingException {
        return connection;
    }

    @Override
    public void closeConnection(Connection connection) throws RatingException {
        //No-op - the Connection object was passed in as a method parameter. Do not close the connection;
    }
}
