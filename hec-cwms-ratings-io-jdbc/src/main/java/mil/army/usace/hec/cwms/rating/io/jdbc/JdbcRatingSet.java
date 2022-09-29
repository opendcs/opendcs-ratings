/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.jdbc;

import hec.data.IRating;
import hec.data.RatingException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.AbstractRatingSet;
import hec.data.cwmsRating.CwmsRatingSet;
import hec.data.cwmsRating.RatingSet;
import hec.data.cwmsRating.SourceRating;
import hec.data.cwmsRating.TransitionalRating;
import hec.data.cwmsRating.UsgsStreamTableRating;
import hec.data.cwmsRating.VirtualRating;
import hec.data.cwmsRating.io.RatingSetStateContainer;
import hec.hecmath.TimeSeriesMath;
import hec.io.TimeSeriesContainer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.logging.Level;
import mil.army.usace.hec.metadata.VerticalDatumException;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import usace.cwms.db.jooq.codegen.tables.AV_RATING;

/**
 * Implements CWMS-style ratings (time series of ratings)
 *
 * @author Mike Perryman
 */
public abstract class JdbcRatingSet extends AbstractRatingSet implements CwmsRatingSet {
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
     * Retrieves the rating extents for a specified time
     *
     * @param ratingTime The time for which to retrieve the rating extents
     * @param conn       The database connection to use if the rating was lazily loaded
     * @return The rating extents
     * @throws RatingException any errors calcualting the value
     * @see IRating#getRatingExtents(long)
     */
    @Override
    public final double[][] getRatingExtents(long ratingTime, Connection conn) throws RatingException {
        synchronized (this) {
            setTransientConnectionProvider(conn);
            try {
                return getRatingExtents(ratingTime);
            } finally {
                clearDatabaseConnection();
            }
        }
    }

    /**
     * Retrieves the data ratingUnits. These are the ratingUnits expected for independent parameters and the unit produced
     * for the dependent parameter.  If the underlying rating uses different ratingUnits, the rating must perform unit
     * conversions.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @return The ratingUnits identifier, one unit for each parameter
     */
    @Override
    public final String[] getDataUnits(Connection conn) {
        setTransientConnectionProvider(conn);
        try {
            return getDataUnits();
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Sets the data ratingUnits. These are  the ratingUnits expected for independent parameters and the unit produced
     * for the dependent parameter.  If the underlying rating uses different ratingUnits, the rating must perform unit
     * conversions.
     *
     * @param conn  The database connection to use for lazy ratings and reference ratings
     * @param units The ratingUnits, one unit for each parameter
     * @throws RatingException any errors calcualting the value
     */
    @Override
    public final void setDataUnits(Connection conn, String[] units) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            setDataUnits(units);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Retrieves the min and max value for each parameter of the rating, in the current ratingUnits.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @return The min and max values for each parameter. The outer (Connection conn, first) dimension will be 2, with the first containing
     * min values and the second containing max values. The inner (Connection conn, second) dimension will be the number of independent
     * parameters for the rating plus one. The first value will be the extent for the first independent parameter, and
     * the last value will be the extent for the dependent parameter.
     * @throws RatingException any errors getting the table information
     */
    @Override
    public final double[][] getRatingExtents(Connection conn) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return getRatingExtents();
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Retrieves the min and max value for each parameter of the rating, in the current ratingUnits.
     *
     * @param conn       The database connection to use for lazy ratings and reference ratings
     * @param ratingTime The time to use in determining the rating extents
     * @return The min and max values for each parameter. The outer (Connection conn, first) dimension will be 2, with the first containing
     * min values and the second containing max values. The inner (Connection conn, second) dimension will be the number of independent
     * parameters for the rating plus one. The first value will be the extent for the first independent parameter, and
     * the last value will be the extent for the dependent parameter.
     * @throws RatingException any issues getting the table data
     */
    @Override
    public final double[][] getRatingExtents(Connection conn, long ratingTime) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return getRatingExtents(ratingTime);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Retrieves the effective dates of the rating in milliseconds, one for each contained rating
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @return simple array of longs
     */
    @Override
    public final long[] getEffectiveDates(Connection conn) {
        setTransientConnectionProvider(conn);
        try {
            return getEffectiveDates();
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Retrieves the creation dates of the rating in milliseconds, one for each contained rating
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @return simple array of start dates
     */
    @Override
    public final long[] getCreateDates(Connection conn) {
        setTransientConnectionProvider(conn);
        try {
            return getCreateDates();
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds the dependent value for a single independent value.  The rating must be for a single independent parameter.
     *
     * @param conn   The database connection to use for lazy ratings and reference ratings
     * @param indVal The independent value to rate.
     * @return The dependent value
     * @throws RatingException any errors calcualting the value
     */
    @Override
    public final double rate(Connection conn, double indVal) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rate(indVal);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param indVals The independent parameters to rate
     * @return The dependent value
     * @throws RatingException any errors retrieving or calculating a value.
     */
    @Override
    public final double rateOne(Connection conn, double... indVals) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rateOne(indVals);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param indVals The independent parameters to rate
     * @return The dependent value
     * @throws RatingException any issues calculating the value
     */
    @Override
    public final double rateOne2(Connection conn, double[] indVals) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rateOne(indVals);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds multiple dependent values for multiple single independent values.  The rating must be for a single independent parameter.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param indVals The independent values to rate
     * @return The dependent values
     * @throws RatingException any issues calculating the value
     */
    @Override
    public final double[] rate(Connection conn, double[] indVals) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rate(indVals);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds multiple dependent values for multiple sets of independent values.  The rating must be for as many independent
     * parameters as the length of each independent parameter set.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param indVals The independent values to rate. Each set of independent values must be the same length.
     * @return The dependent values
     * @throws RatingException any issues calculating the values
     */
    @Override
    public final double[] rate(Connection conn, double[][] indVals) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rate(indVals);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds the dependent value for a single independent value at a specified time.  The rating must be for a single independent parameter.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param valTime The time associated with the value to rate, in Java milliseconds
     * @param indVal  The independent value to rate
     * @return The dependent value
     * @throws RatingException any errors calcualting the value
     */
    @Override
    public final double rate(Connection conn, long valTime, double indVal) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rate(valTime, indVal);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param valTime The time associated with the set of value to rate, in Java milliseconds
     * @param indVals The independent parameters to rate
     * @return The dependent value
     * @throws RatingException any errors calculating the value
     */
    @Override
    public final double rateOne(Connection conn, long valTime, double... indVals) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rateOne(valTime, indVals);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param valTime The time associated with the set of value to rate, in Java milliseconds
     * @param indVals The independent parameters to rate
     * @return The dependent value
     * @throws RatingException any errors calculating the value
     */
    @Override
    public final double rateOne2(Connection conn, long valTime, double[] indVals) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rateOne(valTime, indVals);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds multiple dependent values for multiple single independent values at a specified time.  The rating must be for a single independent parameter.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param valTime The time associated with the values to rate, in Java milliseconds
     * @param indVals The independent values to rate
     * @return The dependent values
     * @throws RatingException any errors calculating the values
     */
    @Override
    public final double[] rate(Connection conn, long valTime, double[] indVals) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rate(valTime, indVals);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds multiple dependent values for multiple single independent and times.  The rating must be for a single independent parameter.
     *
     * @param conn     The database connection to use for lazy ratings and reference ratings
     * @param valTimes The times associated with the values to rate, in Java milliseconds
     * @param indVals  The independent values to rate
     * @return The dependent values
     * @throws RatingException any errors calculating the values
     */
    @Override
    public final double[] rate(Connection conn, long[] valTimes, double[] indVals) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rate(valTimes, indVals);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds multiple dependent values for multiple sets of independent values at a specified time.  The rating must be for as many independent
     * parameters as the length of each independent parameter set.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param valTime The time associated with the values to rate, in Java milliseconds
     * @param indVals The independent values to rate. Each set of independent values must be the same length.
     * @return The dependent values
     * @throws RatingException any errors calculating the values
     */
    @Override
    public final double[] rate(Connection conn, long valTime, double[][] indVals) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rate(valTime, indVals);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds multiple dependent values for multiple sets of independent values and times.  The rating must be for as many independent
     * parameters as the length of each independent parameter set.
     *
     * @param conn     The database connection to use for lazy ratings and reference ratings
     * @param valTimes The time associated with the values to rate, in Java milliseconds
     * @param indVals  The independent values to rate. Each set of independent values must be the same length.
     * @return The dependent values
     * @throws RatingException any errors calculating the values
     */
    @Override
    public final double[] rate(Connection conn, long[] valTimes, double[][] indVals) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rate(valTimes, indVals);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Rates the values in the specified TimeSeriesContainer to generate a resulting TimeSeriesContainer. The rating must be for a single independent parameter.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @param tsc  The TimeSeriesContainer of independent values.
     * @return The TimeSeriesContainer of dependent values.
     * @throws RatingException any errors calculating the values
     */
    @Override
    public final TimeSeriesContainer rate(Connection conn, TimeSeriesContainer tsc) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rate(tsc);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Rates the values in the specified TimeSeriesContainer objects to generate a resulting TimeSeriesContainer. The rating must be for as many independent parameters as the length of tscs.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @param tscs The TimeSeriesContainers of independent values, one for each independent parameter.
     * @return The TimeSeriesContainer of dependent values.
     * @throws RatingException any errors calculating the values
     */
    @Override
    public final TimeSeriesContainer rate(Connection conn, TimeSeriesContainer[] tscs) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rate(tscs);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Rates the values in the specified TimeSeriesMath to generate a resulting TimeSeriesMath. The rating must be for a single independent parameter.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @param tsm  The TimeSeriesMath of independent values.
     * @return The TimeSeriesMath of dependent values.
     * @throws RatingException any errors calculating the values
     */
    @Override
    public final TimeSeriesMath rate(Connection conn, TimeSeriesMath tsm) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rate(tsm);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Rates the values in the specified TimeSeriesMath objects to generate a resulting TimeSeriesMath. The rating must be for as many independent parameters as the length of tscs.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @param tsms The TimeSeriesMaths of independent values, one for each independent parameter.
     * @return The TimeSeriesMath of dependent values.
     * @throws RatingException any errors calculating the value
     */
    @Override
    public final TimeSeriesMath rate(Connection conn, TimeSeriesMath[] tsms) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return rate(tsms);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds the independent value for a single independent value.  The rating must be for a single independent parameter.
     *
     * @param conn   The database connection to use for lazy ratings and reference ratings
     * @param depVal The dependent value to rate.
     * @return The independent value
     * @throws RatingException any errors calculating the value
     */
    @Override
    public final double reverseRate(Connection conn, double depVal) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return reverseRate(depVal);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds multiple independent values for multiple single independent values.  The rating must be for a single independent parameter.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param depVals The dependent values to rate
     * @return The independent values
     * @throws RatingException any errors calculating the value
     */
    @Override
    public final double[] reverseRate(Connection conn, double[] depVals) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return reverseRate(depVals);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds the independent value for a single independent value at a specified time.  The rating must be for a single independent parameter.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param valTime The time associated with the value to rate, in Java milliseconds
     * @param depVal  The dependent value to rate
     * @return The independent value
     * @throws RatingException any errors calculating the value
     */
    @Override
    public final double reverseRate(Connection conn, long valTime, double depVal) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return reverseRate(valTime, depVal);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds multiple independent values for multiple single independent values at a specified time.  The rating must be for a single independent parameter.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param valTime The time associated with the values to rate, in Java milliseconds
     * @param depVals The dependent values to rate
     * @return The independent values
     * @throws RatingException any errors calculating the values
     */
    @Override
    public final double[] reverseRate(Connection conn, long valTime, double[] depVals) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return reverseRate(valTime, depVals);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Finds multiple independent values for multiple single independent and times.  The rating must be for a single independent parameter.
     *
     * @param conn     The database connection to use for lazy ratings and reference ratings
     * @param valTimes The times associated with the values to rate, in Java milliseconds
     * @param depVals  The dependent values to rate
     * @return The independent values
     * @throws RatingException any errors calculating the value
     */
    @Override
    public final double[] reverseRate(Connection conn, long[] valTimes, double[] depVals) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return reverseRate(valTimes, depVals);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Rates the values in the specified TimeSeriesContainer to generate a resulting TimeSeriesContainer. The rating must be for a single independent parameter.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @param tsc  The TimeSeriesContainer of dependent values.
     * @return The TimeSeriesContainer of independent values.
     * @throws RatingException any errors calculating the value
     */
    @Override
    public final TimeSeriesContainer reverseRate(Connection conn, TimeSeriesContainer tsc) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return reverseRate(tsc);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * Rates the values in the specified TimeSeriesMath to generate a resulting TimeSeriesMath. The rating must be for a single independent parameter.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @param tsm  The TimeSeriesMath of dependent values.
     * @return The TimeSeriesMath of independent values.
     * @throws RatingException any errors calculating the value
     */
    @Override
    public final TimeSeriesMath reverseRate(Connection conn, TimeSeriesMath tsm) throws RatingException {
        setTransientConnectionProvider(conn);
        try {
            return reverseRate(tsm);
        } finally {
            clearDatabaseConnection();
        }
    }

    /**
     * @return a container of the object state
     */
    @Override
    public final synchronized RatingSetStateContainer getState() {
        RatingSetStateContainer rssc = new RatingSetStateContainer();
        if (dbInfo != null) {
            rssc.dbUrl = dbInfo.getUrl();
            rssc.dbUserName = dbInfo.getUserName();
            rssc.dbOfficeId = dbInfo.getOfficeId();
        }
        if (persistentConnectionProvider != null) {
            rssc.conn = new JdbcRatingConnectionInfo(persistentConnectionProvider);
        }
        rssc.dataUnits = getDataUnits();
        rssc.allowUnsafe = doesAllowUnsafe();
        rssc.warnUnsafe = doesWarnUnsafe();
        rssc.defaultValueTime = defaultValueTime;
        rssc.ratingTime = ratingTime;
        return rssc;
    }

    /**
     * Sets the state of this object from a container
     *
     * @param rssc the state container
     * @throws RatingException any errors transferring data
     */
    @Override
    public final void setState(RatingSetStateContainer rssc) throws RatingException {
        synchronized (this) {
            if (rssc.conn != null) {
                this.persistentConnectionProvider = rssc.conn.getConnectionInfo(ConnectionProvider.class);
            }
            if (rssc.dbUrl != null || rssc.dbUserName != null || rssc.dbOfficeId != null) {
                dbInfo = new DbInfo(rssc.dbUrl, rssc.dbUserName, rssc.dbOfficeId);
            } else {
                dbInfo = null;
            }
            setDataUnits(rssc.dataUnits);
            setAllowUnsafe(rssc.allowUnsafe);
            setWarnUnsafe(rssc.warnUnsafe);
            defaultValueTime = rssc.defaultValueTime;
            ratingTime = rssc.ratingTime;
        }
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
    public void clearDatabaseConnection() {
        this.transientConnectionProvider = null;

        if (activeRatings != null) {
            for (AbstractRating rating : activeRatings.values()) {
                if (rating instanceof UsgsStreamTableRating) {
                    try {
                        JdbcRatingSet shifts = (JdbcRatingSet) ((UsgsStreamTableRating) rating).getShifts();
                        if (shifts != null) {
                            shifts.clearDatabaseConnection();
                        }
                    } catch (RatingException e) {
                        getLogger().log(Level.WARNING, "Unable to clear database connection for shift rating set: " + rating.getName(), e);
                    }
                } else if (rating instanceof VirtualRating) {
                    SourceRating[] sourceRatings = ((VirtualRating) rating).getSourceRatings();
                    if (sourceRatings != null) {
                        for (SourceRating sourceRating : ((VirtualRating) rating).getSourceRatings()) {
                            RatingSet ratingSet = sourceRating.getRatingSet();
                            if (ratingSet != null) {
                                ((JdbcRatingSet) ratingSet).clearDatabaseConnection();
                            }
                        }
                    }
                } else if (rating instanceof TransitionalRating) {
                    SourceRating[] sourceRatings = ((TransitionalRating) rating).getSourceRatings();
                    if (sourceRatings != null) {
                        for (SourceRating sourceRating : ((TransitionalRating) rating).getSourceRatings()) {
                            RatingSet ratingSet = sourceRating.getRatingSet();
                            if (ratingSet != null) {
                                ratingSet.clearDatabaseConnection();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public final String getNativeVerticalDatum(Connection conn) throws VerticalDatumException {
        setTransientConnectionProvider(conn);
        try {
            return getNativeVerticalDatum();
        } finally {
            clearDatabaseConnection();
        }
    }

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
     * @see hec.data.cwmsRating.AbstractRating#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dbInfo, persistentConnectionProvider);
    }

    /**
     * @param conn The database connection to use
     * @return whether this rating set has been updated in the database
     * @throws RatingException
     */
    @Override
    public final boolean isUpdated(Connection conn) throws RatingException {
        try {
            long referenceTime = getReferenceTime();
            for (String ratingSpec : getComponentRatingSpecs()) {
                Field<Timestamp> greatest = DSL.greatest(DSL.max(AV_RATING.AV_RATING.EFFECTIVE_DATE), DSL.max(AV_RATING.AV_RATING.CREATE_DATE));
                Result<Record1<Timestamp>> result = DSL.using(conn, SQLDialect.ORACLE).select(greatest).from(AV_RATING.AV_RATING)
                                                       .where(AV_RATING.AV_RATING.RATING_ID.equalIgnoreCase(ratingSpec)).getResult();
                if (!result.isEmpty() && result.get(0).component1().getTime() > referenceTime) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException e) {
            throw new RatingException(e);
        }
    }

    /**
     * Sets the database connection for this RatingSet and any constituent RatingSet objects
     *
     * @param conn the connection
     */
    @Override
    public abstract void setDatabaseConnection(Connection conn);

    protected final synchronized void setTransientConnectionProvider(Connection connection) {
        this.transientConnectionProvider = new TransientConnectionProvider(connection);
    }

    /**
     * @return a the current database connection plus a flag specifying whether it was retrieved using the DbInfo
     * @throws RatingException
     */
    protected final synchronized Connection getConnection() throws RatingException {
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