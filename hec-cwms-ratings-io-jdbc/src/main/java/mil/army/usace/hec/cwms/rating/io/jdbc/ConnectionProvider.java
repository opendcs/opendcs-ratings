/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.jdbc;

import hec.data.RatingException;
import java.sql.Connection;

public interface ConnectionProvider {

    Connection getConnection() throws RatingException;

    void closeConnection(Connection connection) throws RatingException;
}
