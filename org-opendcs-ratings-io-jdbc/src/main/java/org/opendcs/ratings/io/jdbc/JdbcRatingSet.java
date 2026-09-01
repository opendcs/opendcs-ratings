/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/

package org.opendcs.ratings.io.jdbc;


import org.opendcs.ratings.AbstractRatingSet;
import org.opendcs.ratings.RatingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Implements CWMS-style ratings (time series of ratings)
 *
 * @author Mike Perryman
 */
public abstract class JdbcRatingSet extends AbstractRatingSet {
    /**
     * Connection for lazy and reference ratings.
     */
    private ConnectionProvider persistentConnectionProvider;

    /**
     * Connection passed in through method parameters. We do not want to close the connections obtained by this.
     */
    private TransientConnectionProvider transientConnectionProvider;

    /**
     * Connection info for lazy and reference ratings.
     */
    protected DbInfo dbInfo;

    protected JdbcRatingSet(ConnectionProvider conn, DbInfo dbInfo) {
        this.persistentConnectionProvider = conn;
        this.dbInfo = dbInfo;
    }

    protected JdbcRatingSet(DbInfo dbInfo) {
        this(null, dbInfo);
    }    

    /**
     * Retrieves the database info required to retrieve a database connection
     *
     * @return the database info required to retrieve a database connection
     * @throws RatingException any errors retreiving the database information
     */
    public final synchronized DbInfo getDbInfo() throws RatingException {
        if (dbInfo == null) {
            return null;
        }
        return new DbInfo(dbInfo.getUrl(), dbInfo.getUserName(), dbInfo.getOfficeId());
    }

    /**
     * Sets the database info required to retrieve a database connection
     *
     * @param url      the database URL
     * @param userName the database user name
     * @param officeId the database office
     * @throws RatingException any errors
     */
    public final synchronized void setDbInfo(String url, String userName, String officeId) throws RatingException {
        setDbInfo(new DbInfo(url, userName, officeId));
    }

    /**
     * Sets the database info required to retrieve a database connection
     *
     * @param dbInfo the database info required to retrieve a database connection
     * @throws RatingException any errors
     */
    public abstract void setDbInfo(DbInfo dbInfo) throws RatingException;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        JdbcRatingSet that = (JdbcRatingSet) o;
        return Objects.equals(persistentConnectionProvider, that.persistentConnectionProvider) && Objects.equals(dbInfo, that.dbInfo);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.AbstractRating#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dbInfo, persistentConnectionProvider);
    }

    protected final synchronized void setTransientConnectionProvider(Connection connection) {
        this.transientConnectionProvider = new TransientConnectionProvider(connection);
    }

    /**
     * @return a the current database connection plus a flag specifying whether it was retrieved using the DbInfo
     * @throws RatingException on error
     */
    protected final synchronized Connection getConnection() throws org.opendcs.ratings.RatingException {
        if (transientConnectionProvider != null) {
            return transientConnectionProvider.getConnection();
        } else if (persistentConnectionProvider != null) {
            return persistentConnectionProvider.getConnection();
        } else {
            if (dbInfo == null) {
                String msg = String.format("Rating set %s - %s is not currently connected to a database.\n" +
                        "Call setConnection(Connection) first or use a method with a Connection parameter.", getRatingSpec().getRatingSpecId(),
                    System.identityHashCode(this));
                throw new RatingException(msg);
            } else {
                persistentConnectionProvider = new ConnectionProvider() {
                    @Override
                    public Connection getConnection() throws RatingException {
                        try {
                            return (Connection) Class.forName("wcds.dbi.client.JdbcConnection")
                                                     .getMethod("retrieveConnection", String.class, String.class, String.class)
                                                     .invoke(null, dbInfo.getUrl(), dbInfo.getUserName(), dbInfo.getOfficeId());
                        } catch (Exception e) {
                            throw new RatingException(e);
                        }
                    }

                    @Override
                    public void closeConnection(Connection connection) throws RatingException {
                        try {
                            Class.forName("wcds.dbi.client.JdbcConnection").getMethod("closeConnection", Connection.class).invoke(null, connection);
                        } catch (Exception e) {
                            throw new RatingException(e);
                        }
                    }
                };
                return persistentConnectionProvider.getConnection();
            }
        }
    }

    protected synchronized void releaseConnection(Connection connection) throws RatingException {
        try {
            if (transientConnectionProvider != null) {
                transientConnectionProvider.closeConnection(connection);
                transientConnectionProvider = null;
            } else if (persistentConnectionProvider != null) {
                persistentConnectionProvider.closeConnection(connection);
            } else {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RatingException(e);
        }
    }


    /**
     * Class for use in LAZY and REFERENCE ratings to be able to release and re-retrieve connections from the connection pool
     */
    public static final class DbInfo {
        private final String url;
        private final String userName;
        private final String officeId;

        public DbInfo(String url, String userName, String officeId) throws RatingException {
            if (url == null) {
                throw new RatingException("DbInfo.url cannot be null");
            }
            if (userName == null) {
                throw new RatingException("DbInfo.userName cannot be null");
            }
            if (officeId == null) {
                throw new RatingException("DbInfo.officeId cannot be null");
            }
            this.userName = userName;
            this.url = url;
            this.officeId = officeId;
        }

        public String getUserName() {
            return userName;
        }

        public String getUrl() {
            return url;
        }

        public String getOfficeId() {
            return officeId;
        }

        @Override
        public int hashCode() {
            return getClass().getName().hashCode() + 3 * url.toLowerCase().hashCode() + 5 * userName.toLowerCase().hashCode() +
                7 * officeId.toLowerCase().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this ||
                (obj instanceof DbInfo && ((DbInfo) obj).url.equalsIgnoreCase(url) && ((DbInfo) obj).userName.equalsIgnoreCase(userName) &&
                    ((DbInfo) obj).officeId.equalsIgnoreCase(officeId));
        }
    }
}