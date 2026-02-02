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


import org.opendcs.ratings.AbstractRating;
import org.opendcs.ratings.IRating;
import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.RatingConst.RatingMethod;
import org.opendcs.ratings.RatingSet;
import org.opendcs.ratings.SourceRating;
import org.opendcs.ratings.TableRating;
import org.opendcs.ratings.TransitionalRating;
import org.opendcs.ratings.UsgsStreamTableRating;
import org.opendcs.ratings.VirtualRating;
import hec.lang.Const;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.logging.Level;
import org.opendcs.ratings.io.xml.RatingXmlFactory;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import usace.cwms.db.jooq.codegen.packages.CWMS_RATING_PACKAGE;

public final class LazyJdbcRatingSet extends JdbcRatingSet {

    LazyJdbcRatingSet(ConnectionProvider conn, DbInfo dbInfo) throws RatingException {
        super(conn, dbInfo);
        validate();
    }

    @Override
    public void getConcreteRatings(long date) throws RatingException {
        if (activeRatings.containsKey(date)) {
            Entry<Long, AbstractRating> entry = activeRatings.floorEntry(date);
            getConcreteRating(entry);
        }
    }

    /**
     * Loads all rating values from table ratings that haven't already been loaded.
     *
     * @throws RatingException
     */
    @Override
    public void getConcreteRatings(Connection conn) throws RatingException {
        setDatabaseConnection(conn);
        long[] effectiveDates = getEffectiveDates();
        for (long effectiveDate : effectiveDates) {
            Entry<Long, AbstractRating> entry = activeRatings.floorEntry(effectiveDate);
            getConcreteRating(entry);
        }
        clearDatabaseConnection();
    }

    /**
     * Loads all rating values from table ratings that haven't already been loaded.
     *
     * @throws RatingException
     */
    public void getConcreteRatings() throws RatingException {
        long[] effectiveDates = getEffectiveDates();
        for (long effectiveDate : effectiveDates) {
            Entry<Long, AbstractRating> entry = activeRatings.floorEntry(effectiveDate);
            getConcreteRating(entry);
        }
    }

    private synchronized Entry<Long, AbstractRating> getConcreteRating(Entry<Long, AbstractRating> ratingEntry) throws RatingException {
        Entry<Long, AbstractRating> newEntry = ratingEntry;
        try {
            if (ratingEntry != null) {
                Long key = ratingEntry.getKey();
                AbstractRating rating = ratingEntry.getValue();
                if (rating instanceof TableRating && ((TableRating) rating).getRatingValues() == null) {
                    //----------------------------------------//
                    // rating not yet retrieved from database //
                    //----------------------------------------//
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Connection conn = getConnection();
                    try {
                        if (getLogger().isLoggable(Level.FINE)) {
                            getLogger().fine(
                                String.format("Retrieving rating from %s: %s @ %s UTC", conn.getMetaData().getURL(), getName(), sdf.format(key)));
                        }
                        DSLContext dsl = DSL.using(conn);
                        Configuration configuration = dsl.configuration();
                        String xmlText = CWMS_RATING_PACKAGE.call_RETRIEVE_RATINGS_XML(configuration, rating.getRatingSpecId(),
                            new Timestamp(rating.getEffectiveDate()), new Timestamp(rating.getEffectiveDate()), "UTC", rating.getOfficeId());
                        getLogger().log(Level.FINE, "Retrieve XML:\n" + xmlText);
                        int pos = xmlText.indexOf("<ratings ");
                        if (pos == -1 || xmlText.indexOf('<', pos + 1) == -1) {
                            getLogger().log(Level.WARNING, "Cannot get concrete rating for " + rating.getRatingSpecId() + " for effective date " +
                                sdf.format(rating.getEffectiveDate()) + " UTC. Removing effective date from rating set");
                            ratings.remove(rating.getEffectiveDate());
                            if (activeRatings.containsKey(rating.getEffectiveDate())) {
                                activeRatings.remove(rating.getEffectiveDate());
                            }
                        } else {
                            AbstractRating newRating = RatingXmlFactory.abstractRating(xmlText);
                            if (newRating == null) {
                                throw new RatingException("Unexpected rating type: \n" + xmlText);
                            }
                            replaceRating(newRating);

                        }
                        refreshRatings();
                        if (observationTarget != null) {
                            observationTarget.setChanged();
                            observationTarget.notifyObservers();
                        }
                    } finally {
                        releaseConnection(conn);
                    }
                }
            }
        } catch (SQLException | RuntimeException e) {
            throw new RatingException(e);
        }
        return newEntry;
    }

    /**
     * Retrieves rated values for specified multiple input value Sets and times. The rating set must
     * be for as many independent parameter as each value set
     *
     * @param valueSets  The value sets to rate
     * @param valueTimes The times associated with the values, in Java milliseconds
     * @return the rated value
     * @throws RatingException
     */
    @Override
    public synchronized double[] rate(double[][] valueSets, long[] valueTimes) throws RatingException {
        double[] Y = new double[valueSets.length];
        //-----------------//
        // concrete rating //
        //-----------------//
        int activeRatingCount = activeRatings.size();
        if (activeRatingCount == 0) {
            throw new RatingException("No active ratings.");
        }
        if (valueSets.length != valueTimes.length) {
            throw new RatingException("Values and times have different lengths");
        }
        for (int i = 1; i < valueSets.length; ++i) {
            if (valueSets[i].length != valueSets[0].length) {
                throw new RatingException("Value sets are not all of same length.");
            }
            if (valueSets[i].length != ratings.firstEntry().getValue().getIndParamCount()) {
                throw new RatingException("Value sets have different parameter counts than ratings.");
            }
        }
        if (getDataUnits() == null) {
            Entry<Long, AbstractRating> entry1 = ratings.firstEntry();
            Entry<Long, AbstractRating> entry2 = ratings.higherEntry(entry1.getKey());
            while (entry2 != null) {
                if (!(entry1.getValue().getRatingUnitsId().equalsIgnoreCase(entry2.getValue().getRatingUnitsId()))) {
                    throw new RatingException("Data units must be specified when rating set has multiple rating units.");
                }
                entry1 = entry2;
                entry2 = ratings.higherEntry(entry1.getKey());
            }
        }

        Entry<Long, AbstractRating> lowerRating;
        Entry<Long, AbstractRating> upperRating;
        IRating lastUsedRating = null;
        RatingMethod method = null;
        for (int i = 0; i < valueSets.length; ++i) {
            if (i > 0 && valueTimes[i] == valueTimes[i - 1] && lastUsedRating != null) {
                Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                continue;
            } else {
                lowerRating = activeRatings.floorEntry(valueTimes[i]);
                upperRating = activeRatings.ceilingEntry(valueTimes[i]);
                //-------------------------//
                // handle out of range low //
                //-------------------------//
                if (lowerRating == null) {
                    method = ratingSpec.getOutRangeLowMethod();
                    switch (method) {
                        case ERROR:
                            throw new RatingException("Effective date is before earliest rating");
                        case NULL:
                            Y[i] = Const.UNDEFINED_DOUBLE;
                            lastUsedRating = null;
                            continue;
                        case NEXT:
                        case NEAREST:
                        case HIGHER:
                        case CLOSEST:
                            getConcreteRating(activeRatings.firstEntry());
                            lastUsedRating = activeRatings.firstEntry().getValue();
                            Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                            continue;
                        default:
                            break;
                    }
                    if (activeRatings.size() == 1) {
                        throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
                    }
                    lowerRating = activeRatings.firstEntry();
                    upperRating = activeRatings.higherEntry(lowerRating.getKey());
                }
                //--------------------------//
                // handle out of range high //
                //--------------------------//
                if (upperRating == null) {
                    method = ratingSpec.getOutRangeHighMethod();
                    switch (method) {
                        case ERROR:
                            throw new RatingException("Effective date is after latest rating");
                        case NULL:
                            Y[i] = Const.UNDEFINED_DOUBLE;
                            lastUsedRating = null;
                            continue;
                        case PREVIOUS:
                        case NEAREST:
                        case LOWER:
                        case CLOSEST:
                            getConcreteRating(activeRatings.lastEntry());
                            lastUsedRating = activeRatings.lastEntry().getValue();
                            Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                            continue;
                        default:
                            break;
                    }
                    if (activeRatings.size() == 1) {
                        switch (method) {
                            case LINEAR:
                                //-----------------------------------------------------------------//
                                // allow LINEAR out of range high method with single active rating //
                                //-----------------------------------------------------------------//
                                getConcreteRating(activeRatings.lastEntry());
                                lastUsedRating = activeRatings.lastEntry().getValue();
                                Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                                continue;
                            default:
                                throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
                        }
                    }
                    upperRating = activeRatings.lastEntry();
                    lowerRating = activeRatings.lowerEntry(upperRating.getKey());
                }
                //-----------------------------------//
                // handle in-range and extrapolation //
                //-----------------------------------//
                if (lowerRating.getKey() == valueTimes[i]) {
                    lowerRating = getConcreteRating(lowerRating);
                    Y[i] = lowerRating.getValue().rateOne(valueTimes[i], valueSets[i]);
                    continue;
                }
                if (upperRating.getKey() == valueTimes[i]) {
                    upperRating = getConcreteRating(upperRating);
                    lastUsedRating = upperRating.getValue();
                    Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                    continue;
                }
                switch (ratingSpec.getInRangeMethod()) {
                    case ERROR:
                        throw new RatingException("Effective date is between existing rating");
                    case NULL:
                        Y[i] = Const.UNDEFINED_DOUBLE;
                        lastUsedRating = null;
                        continue;
                    case PREVIOUS:
                    case LOWER:
                        lowerRating = getConcreteRating(lowerRating);
                        lastUsedRating = lowerRating.getValue();
                        Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                        continue;
                    case NEXT:
                    case HIGHER:
                        upperRating = getConcreteRating(upperRating);
                        lastUsedRating = upperRating.getValue();
                        Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                        continue;
                    case CLOSEST:
                        if (valueTimes[i] - lowerRating.getKey() < upperRating.getKey() - valueTimes[i]) {
                            lowerRating = getConcreteRating(lowerRating);
                            lastUsedRating = lowerRating.getValue();
                        } else {
                            upperRating = getConcreteRating(upperRating);
                            lastUsedRating = upperRating.getValue();
                        }
                        Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                        continue;
                    default:
                        break;
                }
                //------------------------------------//
                // handle interpolation/extrapolation //
                //------------------------------------//
                method = ratingSpec.getInRangeMethod();
            }
            lastUsedRating = null;
            lowerRating = getConcreteRating(lowerRating);
            upperRating = getConcreteRating(upperRating);
            long transitionStartMillis = upperRating.getValue().getTransitionStartDate();
            long t = valueTimes[i];
            long t1 = lowerRating.getKey();
            long t2 = upperRating.getKey();
            double Y1 = lowerRating.getValue().rateOne(valueTimes[i], valueSets[i]);
            double Y2 = upperRating.getValue().rateOne(valueTimes[i], valueSets[i]);
            if (Y1 == Const.UNDEFINED_DOUBLE || Y2 == Const.UNDEFINED_DOUBLE) {
                Y[i] = Const.UNDEFINED_DOUBLE;
            } else {
                double y1 = Y1;
                double y2 = Y2;
                if (lowerRating.getValue() instanceof UsgsStreamTableRating) {
                    t1 = ((UsgsStreamTableRating) lowerRating.getValue()).getLatestEffectiveDate(t2);
                }
                if (transitionStartMillis > t1 && transitionStartMillis < t2) {
                    t1 = transitionStartMillis;
                }
                Y[i] = y1;
                if (t > t1) {
                    Y[i] += (((double) t - t1) / (t2 - t1)) * (y2 - y1);
                }
            }
        }
        return Y;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getRatingExtents(long)
     */
    @Override
    public synchronized double[][] getRatingExtents(long ratingTime) throws RatingException {
        if (activeRatings.size() == 0) {
            throw new RatingException("No active ratings.");
        }
        Entry<Long, AbstractRating> rating = activeRatings.floorEntry(ratingTime);

        if (rating == null) {
            rating = activeRatings.ceilingEntry(ratingTime);
        }
        rating = getConcreteRating(rating);
        return rating.getValue().getRatingExtents();
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#reverseRate(long[], double[])
     */
    @Override
    public double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException {
        double[] Y = new double[depVals.length];
        if (activeRatings.size() == 0) {
            throw new RatingException("No active ratings.");
        }
        if (getDataUnits() == null) {
            Entry<Long, AbstractRating> entry1 = ratings.firstEntry();
            Entry<Long, AbstractRating> entry2 = ratings.higherEntry(entry1.getKey());
            while (entry2 != null) {
                if (!(entry1.getValue().getRatingUnitsId().equalsIgnoreCase(entry2.getValue().getRatingUnitsId()))) {
                    throw new RatingException("Data units must be specified when rating set has multiple rating units.");
                }
                entry1 = entry2;
                entry2 = ratings.higherEntry(entry1.getKey());
            }
        }
        Entry<Long, AbstractRating> lowerRating;
        Entry<Long, AbstractRating> upperRating;
        IRating lastUsedRating = null;
        RatingMethod method;
        for (int i = 0; i < depVals.length; ++i) {
            if (i > 0 && valTimes[i] == valTimes[i - 1]) {
                if (lastUsedRating == null) {
                    Y[i] = Y[i - 1];
                } else {
                    Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                }
                continue;
            } else {
                lowerRating = activeRatings.floorEntry(valTimes[i]);
                upperRating = activeRatings.ceilingEntry(valTimes[i]);
                //-------------------------//
                // handle out of range low //
                //-------------------------//
                if (lowerRating == null) {
                    method = ratingSpec.getOutRangeLowMethod();
                    switch (method) {
                        case ERROR:
                            throw new RatingException("Effective date is before earliest rating");
                        case NULL:
                            Y[i] = Const.UNDEFINED_DOUBLE;
                            lastUsedRating = null;
                            continue;
                        case NEXT:
                        case NEAREST:
                        case HIGHER:
                        case CLOSEST:
                            getConcreteRating(activeRatings.firstEntry());
                            lastUsedRating = activeRatings.firstEntry().getValue();
                            Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                            continue;
                        default:
                            break;
                    }
                    if (activeRatings.size() == 1) {
                        throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
                    }
                    lowerRating = activeRatings.firstEntry();
                    upperRating = activeRatings.higherEntry(lowerRating.getKey());
                }
                //--------------------------//
                // handle out of range high //
                //--------------------------//
                if (upperRating == null) {
                    method = ratingSpec.getOutRangeHighMethod();
                    switch (method) {
                        case ERROR:
                            throw new RatingException("Effective date is after latest rating");
                        case NULL:
                            Y[i] = Const.UNDEFINED_DOUBLE;
                            lastUsedRating = null;
                            continue;
                        case PREVIOUS:
                        case NEAREST:
                        case LOWER:
                        case CLOSEST:
                            getConcreteRating(activeRatings.lastEntry());
                            lastUsedRating = activeRatings.lastEntry().getValue();
                            Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                            continue;
                        default:
                            break;
                    }
                    if (activeRatings.size() == 1) {
                        switch (method) {
                            case LINEAR:
                                //-----------------------------------------------------------------//
                                // allow LINEAR out of range high method with single active rating //
                                //-----------------------------------------------------------------//
                                getConcreteRating(activeRatings.lastEntry());
                                lastUsedRating = activeRatings.lastEntry().getValue();
                                Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                                continue;
                            default:
                                throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
                        }
                    }
                    upperRating = activeRatings.lastEntry();
                    lowerRating = activeRatings.lowerEntry(upperRating.getKey());
                }
                //-----------------------------------//
                // handle in-range and extrapolation //
                //-----------------------------------//
                if (lowerRating.getKey() == valTimes[i]) {
                    lowerRating = getConcreteRating(lowerRating);
                    Y[i] = lowerRating.getValue().reverseRate(valTimes[i], depVals[i]);
                    continue;
                }
                if (upperRating.getKey() == valTimes[i]) {
                    upperRating = getConcreteRating(upperRating);
                    lastUsedRating = upperRating.getValue();
                    Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                    continue;
                }
                switch (ratingSpec.getInRangeMethod()) {
                    case ERROR:
                        throw new RatingException("Effective date is between existing rating");
                    case NULL:
                        Y[i] = Const.UNDEFINED_DOUBLE;
                        lastUsedRating = null;
                        continue;
                    case PREVIOUS:
                    case LOWER:
                        lowerRating = getConcreteRating(lowerRating);
                        lastUsedRating = lowerRating.getValue();
                        Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                        continue;
                    case NEXT:
                    case HIGHER:
                        upperRating = getConcreteRating(upperRating);
                        lastUsedRating = upperRating.getValue();
                        Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                        continue;
                    case CLOSEST:
                        if (valTimes[i] - lowerRating.getKey() < upperRating.getKey() - valTimes[i]) {
                            lowerRating = getConcreteRating(lowerRating);
                            lastUsedRating = lowerRating.getValue();
                        } else {
                            upperRating = getConcreteRating(upperRating);
                            lastUsedRating = upperRating.getValue();
                        }
                        Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                        continue;
                    default:
                        break;
                }
                //------------------------------------//
                // handle interpolation/extrapolation //
                //------------------------------------//
                method = ratingSpec.getInRangeMethod();
            }
            lastUsedRating = null;
            lowerRating = getConcreteRating(lowerRating);
            upperRating = getConcreteRating(upperRating);
            boolean ind_log = method == RatingMethod.LOGARITHMIC || method == RatingMethod.LIN_LOG;
            boolean dep_log = method == RatingMethod.LOGARITHMIC || method == RatingMethod.LOG_LIN;
            double x = valTimes[i];
            double x1 = lowerRating.getKey();
            double x2 = upperRating.getKey();
            double Y1 = lowerRating.getValue().reverseRate(valTimes[i], depVals[i]);
            double Y2 = upperRating.getValue().reverseRate(valTimes[i], depVals[i]);
            double y1 = Y1;
            double y2 = Y2;
            if (ind_log) {
                x = Math.log10(x);
                x1 = Math.log10(x1);
                x2 = Math.log10(x2);
                if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(x1) || Double.isInfinite(x1) || Double.isNaN(x2) ||
                    Double.isInfinite(x2)) {
                    //-------------------------------------------------//
                    // fall back from LOGARITHMIC or LOG_LIN to LINEAR //
                    //-------------------------------------------------//
                    x = valTimes[i];
                    x1 = lowerRating.getKey();
                    x2 = upperRating.getKey();
                    dep_log = false;
                }
            }
            if (dep_log) {
                y1 = Math.log10(y1);
                y2 = Math.log10(y2);
                if (Double.isNaN(y1) || Double.isInfinite(y1) || Double.isNaN(y2) || Double.isInfinite(y2)) {
                    //-------------------------------------------------//
                    // fall back from LOGARITHMIC or LIN_LOG to LINEAR //
                    //-------------------------------------------------//
                    x = valTimes[i];
                    x1 = lowerRating.getKey();
                    x2 = upperRating.getKey();
                    y1 = Y1;
                    y2 = Y2;
                    dep_log = false;
                }
            }
            double y = y1 + ((x - x1) / (x2 - x1)) * (y2 - y1);
            if (dep_log) {
                y = Math.pow(10, y);
            }
            Y[i] = y;
        }
        return Y;
    }

    /* (non-Javadoc)
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(java.util.Observable arg0, Object arg1) {
        refreshRatings();
        observationTarget.setChanged();
        observationTarget.notifyObservers();
    }

    /**
     * Sets the database info required to retrieve a database connection
     *
     * @param dbInfo the database info required to retrieve a database connection
     * @throws RatingException any errors
     */
    public synchronized void setDbInfo(DbInfo dbInfo) throws RatingException {
        this.dbInfo = dbInfo;
        for (AbstractRating rating : activeRatings.values()) {
            if (rating instanceof UsgsStreamTableRating) {
                try {
                    RatingSet shifts = ((UsgsStreamTableRating) rating).getShifts();
                    if(shifts instanceof JdbcRatingSet) {
                        ((JdbcRatingSet) shifts).setDbInfo(dbInfo);
                    }
                } catch (RatingException e) {
                    getLogger().log(Level.WARNING, "Unable to set database info for shift rating set: " + rating.getName(), e);
                }
            } else if (rating instanceof VirtualRating) {
                SourceRating[] sourceRatings = ((VirtualRating) rating).getSourceRatings();
                if (sourceRatings != null) {
                    for (SourceRating sourceRating : ((VirtualRating) rating).getSourceRatings()) {
                        RatingSet ratingSet = sourceRating.getRatingSet();
                        if(ratingSet instanceof JdbcRatingSet) {
                            ((JdbcRatingSet) ratingSet).setDbInfo(dbInfo);
                        }
                    }
                }
            } else if (rating instanceof TransitionalRating) {
                SourceRating[] sourceRatings = ((TransitionalRating) rating).getSourceRatings();
                if (sourceRatings != null) {
                    for (SourceRating sourceRating : ((TransitionalRating) rating).getSourceRatings()) {
                        RatingSet ratingSet = sourceRating.getRatingSet();
                        if(ratingSet instanceof JdbcRatingSet) {
                            ((JdbcRatingSet) ratingSet).setDbInfo(dbInfo);
                        }
                    }
                }
            }
        }
        for (AbstractRating rating : ratings.values()) {
            if (rating instanceof UsgsStreamTableRating) {
                try {
                    RatingSet shifts = ((UsgsStreamTableRating) rating).getShifts();
                    if(shifts instanceof JdbcRatingSet) {
                        ((JdbcRatingSet) shifts).setDbInfo(dbInfo);
                    }
                } catch (RatingException e) {
                    getLogger().log(Level.WARNING, "Unable to set database info for shift rating set: " + rating.getName(), e);
                }
            } else if (rating instanceof VirtualRating) {
                SourceRating[] sourceRatings = ((VirtualRating) rating).getSourceRatings();
                if (sourceRatings != null) {
                    for (SourceRating sourceRating : ((VirtualRating) rating).getSourceRatings()) {
                        RatingSet ratingSet = sourceRating.getRatingSet();
                        if(ratingSet instanceof JdbcRatingSet) {
                            ((JdbcRatingSet) ratingSet).setDbInfo(dbInfo);
                        }
                    }
                }
            } else if (rating instanceof TransitionalRating) {
                SourceRating[] sourceRatings = ((TransitionalRating) rating).getSourceRatings();
                if (sourceRatings != null) {
                    for (SourceRating sourceRating : ((TransitionalRating) rating).getSourceRatings()) {
                        RatingSet ratingSet = sourceRating.getRatingSet();
                        if(ratingSet instanceof JdbcRatingSet) {
                            ((JdbcRatingSet) ratingSet).setDbInfo(dbInfo);
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets the database connection for this RatingSet and any constituent RatingSet objects
     *
     * @param conn the connection
     */
    public synchronized void setDatabaseConnection(Connection conn) {
        setTransientConnectionProvider(conn);
        for (AbstractRating rating : activeRatings.values()) {
            if (rating instanceof UsgsStreamTableRating) {
                try {
                    RatingSet shifts = ((UsgsStreamTableRating) rating).getShifts();
                    if (shifts != null) {
                        shifts.setDatabaseConnection(conn);
                    }
                } catch (RatingException e) {
                    getLogger().log(Level.WARNING, "Unable to set database connection for shift rating set: " + rating.getName(), e);
                }
            } else if (rating instanceof VirtualRating) {
                SourceRating[] sourceRatings = ((VirtualRating) rating).getSourceRatings();
                if (sourceRatings != null) {
                    for (SourceRating sourceRating : ((VirtualRating) rating).getSourceRatings()) {
                        RatingSet ratingSet = sourceRating.getRatingSet();
                        if (ratingSet != null) {
                            ratingSet.setDatabaseConnection(conn);
                        }
                    }
                }
            } else if (rating instanceof TransitionalRating) {
                SourceRating[] sourceRatings = ((TransitionalRating) rating).getSourceRatings();
                if (sourceRatings != null) {
                    for (SourceRating sourceRating : ((TransitionalRating) rating).getSourceRatings()) {
                        RatingSet ratingSet = sourceRating.getRatingSet();
                        if (ratingSet != null) {
                            ratingSet.setDatabaseConnection(conn);
                        }
                    }
                }
            }
        }
    }

    private synchronized void refreshRatings() {
        //-------------------------------------------------------------------//
        // first update ratings from active ratings (if rating still exists) //
        //-------------------------------------------------------------------//
        for (Long key : activeRatings.keySet()) {
            if (ratings.containsKey(key)) {
                ratings.put(key, activeRatings.get(key));
            }
        }
        //---------------------------------------------//
        // now rebuild both ratings and active ratings //
        //---------------------------------------------//
        AbstractRating[] ratingArray = ratings.values().toArray(new AbstractRating[0]);
        ratings.clear();
        activeRatings.clear();
        for (AbstractRating rating : ratingArray) {
            long effectiveDate = rating.getEffectiveDate();
            ratings.put(effectiveDate, rating);
            if (rating.isActive() && rating.getCreateDate() < ratingTime) {
                activeRatings.put(effectiveDate, rating);
            }
            rating.deleteObserver(this);
            rating.addObserver(this);
        }
        if (observationTarget != null) {
            observationTarget.setChanged();
            observationTarget.notifyObservers();
        }
    }
}