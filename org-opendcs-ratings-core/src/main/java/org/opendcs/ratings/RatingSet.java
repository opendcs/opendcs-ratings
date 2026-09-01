/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package org.opendcs.ratings;

import hec.hecmath.TimeSeriesMath;
import hec.io.TimeSeriesContainer;
import mil.army.usace.hec.metadata.DataSetException;
import mil.army.usace.hec.metadata.VerticalDatum;
import mil.army.usace.hec.metadata.VerticalDatumContainer;
import mil.army.usace.hec.metadata.VerticalDatumException;
import org.opendcs.ratings.io.*;

import java.util.HashSet;
import java.util.TreeMap;

import static hec.lang.Const.UNDEFINED_TIME;

/**
 * Implements CWMS-style ratings (time series of ratings)
 *
 * @author Mike Perryman
 */
@SuppressWarnings("java:S1448")
public class RatingSet implements IRating, VerticalDatum {

    private AbstractRatingSet composedRatingSet;

    protected RatingSet() {
    }

    /**
     * Enumeration for specifying the method used to load a RatingSet object from a CWMS database
     * <table border="1">
     *   <caption>Loading Methods</caption>
     *   <tr>
     *     <th>Value</th>
     *     <th>Interpretation</th>
     *   </tr>
     *   <tr>
     *     <td>EAGER</td>
     *     <td>Ratings for all effective times are loaded initially</td>
     *     <td>LAZY</td>
     *     <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *     <td>REFERENCE</td>
     *     <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *   </tr>
     * </table>
     */
    public enum DatabaseLoadMethod {
        EAGER, LAZY, REFERENCE

    }

    /**
     * Returns whether new RatingSet objects will by default allow "risky" behavior such as using mismatched units, unknown parameters, etc.
     *
     * @return A flag specifying whether new RatingSet objects will by default allow "risky" behavior
     */
    public static boolean getAlwaysAllowUnsafe() {
        return AbstractRatingSet.getAlwaysAllowUnsafe();
    }

    /**
     * Sets whether new RatingSet objects will by default allow "risky" behavior such as using mismatched units, unknown parameters, etc.
     *
     * @param alwaysAllowUnsafe A flag specifying whether new RatingSet objects will by default allow "risky" behavior
     */
    public static void setAlwaysAllowUnsafe(Boolean alwaysAllowUnsafe) {
        AbstractRatingSet.setAlwaysAllowUnsafe(alwaysAllowUnsafe);
    }

    /**
     * Returns whether new RatingSet objects will by default output messages about "risky" behavior such as using mismatched units, unknown parameters, etc.
     *
     * @return A flag specifying whether new RatingSet objects will by default output messages about "risky" behavior
     */
    public static boolean getAlwaysWarnUnsafe() {
        return AbstractRatingSet.alwaysWarnUnsafe;
    }

    /**
     * Sets whether new RatingSet objects will by default output messages about "risky" behavior such as using mismatched units, unknown parameters, etc.
     *
     * @param alwaysWarnUnsafe A flag specifying whether new RatingSet objects will by default output messages about "risky" behavior
     */
    public static void setAlwaysWarnUnsafe(Boolean alwaysWarnUnsafe) {
        AbstractRatingSet.setAlwaysWarnUnsafe(alwaysWarnUnsafe);
    }

    /**
     * Adds a single rating to the existing ratings.
     *
     * @param rating The rating to add
     * @throws RatingException @see #addRatings(Iterable)
     */
    public void addRating(AbstractRating rating) throws RatingException {
        this.composedRatingSet.addRating(rating);
    }

    /**
     * Adds multiple ratings to the existing ratings.
     *
     * @param ratings The ratings to add
     * @throws RatingException @see #addRatings(Iterable)
     */
    public void addRatings(AbstractRating[] ratings) throws RatingException {
        this.composedRatingSet.addRatings(ratings);
    }

    /**
     * Adds multiple ratings to the existing ratings.
     *
     * @param ratings The ratings to add
     * @throws RatingException various errors with the input such a undefined effective dates,
     *                         effective date already exists, number of independent parameters not consistent,
     *                         rating specs not consistent, units incompatible, templates not consistent
     */
    public void addRatings(Iterable<AbstractRating> ratings) throws RatingException {
        this.composedRatingSet.addRatings(ratings);
    }

    /**
     * Removes a single rating from the existing ratings.
     *
     * @param effectiveDate The effective date of the rating to remove, in Java milliseconds
     * @throws RatingException on error
     */
    public void removeRating(long effectiveDate) throws RatingException {
        this.composedRatingSet.removeRating(effectiveDate);
    }

    /**
     * Removes all existing ratings.
     */
    public void removeAllRatings() {
        this.composedRatingSet.removeAllRatings();
    }

    /**
     * Replaces a single rating in the existing ratings
     *
     * @param rating The rating to replace an existing one
     * @throws RatingException on error
     */
    public void replaceRating(AbstractRating rating) throws RatingException {
        this.composedRatingSet.replaceRating(rating);
    }

    /**
     * Replaces multiple ratings in the existing ratings.
     *
     * @param ratings The ratings to replace existing ones
     * @throws RatingException on error
     */
    public void replaceRatings(AbstractRating[] ratings) throws RatingException {
        this.composedRatingSet.replaceRatings(ratings);
    }

    /**
     * Replaces multiple ratings in the existing ratings.
     *
     * @param ratings The ratings to replace existing ones
     * @throws RatingException on error
     */
    public void replaceRatings(Iterable<AbstractRating> ratings) throws RatingException {
        this.composedRatingSet.replaceRatings(ratings);
    }

    /**
     * Retrieves a rated value for a specified single input value and time. The rating set must
     * be for a single independent parameter
     *
     * @param value     The value to rate
     * @param valueTime The time associated with the value, in Java milliseconds
     * @return the rated value
     * @throws RatingException on error
     */
    public double rate(double value, long valueTime) throws RatingException {
        return this.composedRatingSet.rate(value, valueTime);
    }

    /**
     * Retrieves rated values for specified multiple input values at a single time. The rating set must
     * be for a single independent parameter
     *
     * @param values    The values to rate
     * @param valueTime The time associated with the values, in Java milliseconds
     * @return the rated value
     * @throws RatingException on error
     */
    public double[] rate(double[] values, long valueTime) throws RatingException {
        return this.composedRatingSet.rate(values, valueTime);
    }

    /**
     * Retrieves rated values for specified multiple input values and times. The rating set must
     * be for a single independent parameter
     *
     * @param values     The values to rate
     * @param valueTimes The times associated with the values, in Java milliseconds
     * @return the rated value
     * @throws RatingException on error
     */
    public double[] rateOne(double[] values, long[] valueTimes) throws RatingException {
        return this.composedRatingSet.rateOne(values, valueTimes);
    }

    /**
     * Retrieves a single rated value for specified input value set at a single time. The rating set must
     * be for as many independent parameters as the length of the value set.
     *
     * @param valueSet  The value set to rate
     * @param valueTime The time associated with the values, in Java milliseconds
     * @return the rated value
     * @throws RatingException on error
     */
    public double rateOne(double[] valueSet, long valueTime) throws RatingException {
        return this.composedRatingSet.rateOne(valueSet, valueTime);
    }

    /**
     * Retrieves rated values for specified multiple input value Sets and times. The rating set must
     * be for as many independent parameter as each value set
     *
     * @param valueSets  The value sets to rate
     * @param valueTimes The times associated with the values, in Java milliseconds
     * @return the rated value
     * @throws RatingException on error
     */
    public double[] rate(double[][] valueSets, long[] valueTimes) throws RatingException {
        return this.composedRatingSet.rate(valueSets, valueTimes);
    }

    /**
     * Rates the values in a TimeSeriesContainer and returns the results in a new TimeSeriesContainer.
     * The rating must be for a single independent parameter.
     *
     * @param tsc The TimeSeriesContainer to rate
     * @return A TimeSeriesContainer of the rated values. The rated unit is the native unit of dependent parameter of the rating.
     * @throws RatingException on error
     */
    @Override
    public TimeSeriesContainer rate(TimeSeriesContainer tsc) throws RatingException {
        return this.composedRatingSet.rate(tsc);
    }

    /**
     * Rates the values in a TimeSeriesContainer and returns the results in a new TimeSeriesContainer with the specified unit.
     * The rating must be for a single independent parameter.
     *
     * @param tsc          The TimeSeriesContainer to rate
     * @param ratedUnitStr The unit to return the rated values in.
     * @return A TimeSeriesContainer of the rated values. The rated unit is the specified unit.
     * @throws RatingException on error
     */
    public TimeSeriesContainer rate(TimeSeriesContainer tsc, String ratedUnitStr) throws RatingException {
        return this.composedRatingSet.rate(tsc, ratedUnitStr);
    }

    /**
     * Rates the values in a set of TimeSeriesContainers and returns the results in a new TimeSeriesContainer.
     * The rating must be for as many independent parameters as the number of TimeSeriesContainers.
     * If all the TimeSeriesContainers have the same interval the rated TimeSeriesContainer will have the same interval, otherwise
     * the rated TimeSeriesContainer will have an interval of 0 (irregular).  The rated TimeSeriesContainer will have values
     * only at times that are common to all the input TimeSeriesContainers.
     *
     * @param tscs The TimeSeriesContainers to rate, in order of the independent parameters of the rating.
     * @return A TimeSeriesContainer of the rated values. The rated unit is the native unit of the dependent parameter of the rating.
     * @throws RatingException on error
     */
    @Override
    public TimeSeriesContainer rate(TimeSeriesContainer[] tscs) throws RatingException {
        return this.composedRatingSet.rate(tscs);
    }

    /**
     * Rates the values in a set of TimeSeriesContainers and returns the results in a new TimeSeriesContainer with the specified unit.
     * The rating must be for as many independent parameters as the number of TimeSeriesContainers.
     * If all the TimeSeriesContainers have the same interval the rated TimeSeriesContainer will have the same interval, otherwise
     * the rated TimeSeriesContainer will have an interval of 0 (irregular).  The rated TimeSeriesContainer will have values
     * only at times that are common to all the input TimeSeriesContainers.
     *
     * @param tscs         The TimeSeriesContainers to rate, in order of the independent parameters of the rating.
     * @param ratedUnitStr The unit to return the rated values in.
     * @return A TimeSeriesContainer of the rated values. The rated unit is the specified unit.
     * @throws RatingException on error
     */
    public TimeSeriesContainer rate(TimeSeriesContainer[] tscs, String ratedUnitStr) throws RatingException {
        return this.composedRatingSet.rate(tscs, ratedUnitStr);
    }

    /* (non-Javadoc)
     * @see org.opendcs.IRating#rate(hec.hecmath.TimeSeriesMath)
     */
    @Override
    public TimeSeriesMath rate(TimeSeriesMath tsm) throws RatingException {
        return this.composedRatingSet.rate(tsm);
    }

    /**
     * Rates the values in a TimeSeriesMath and returns the results in a new TimeSeriesMath with the specified unit.
     * The rating must be for a single independent parameter.
     *
     * @param tsm          The TimeSeriesMath to rate
     * @param ratedUnitStr The unit to return the rated values in.
     * @return A TimeSeriesMath of the rated values. The rated unit is the specified unit.
     * @throws RatingException on error
     */
    public TimeSeriesMath rate(TimeSeriesMath tsm, String ratedUnitStr) throws RatingException {
        return this.composedRatingSet.rate(tsm, ratedUnitStr);
    }

    /* (non-Javadoc)
     * @see org.opendcs.IRating#rate(hec.hecmath.TimeSeriesMath[])
     */
    @Override
    public TimeSeriesMath rate(TimeSeriesMath[] tsms) throws RatingException {
        return this.composedRatingSet.rate(tsms);
    }

    /**
     * Rates the values in a set of TimeSeriesMaths and returns the results in a new TimeSeriesMath with the specified unit.
     * The rating must be for as many independent parameters as the number of TimeSeriesMaths.
     * If all the TimeSeriesMaths have the same interval the rated TimeSeriesMath will have the same interval, otherwise
     * the rated TimeSeriesMath will have an interval of 0 (irregular).  The rated TimeSeriesMath will have values
     * only at times that are common to all the input TimeSeriesMaths.
     *
     * @param tsms         The TimeSeriesMaths to rate, in order of the independent parameters of the rating.
     * @param ratedUnitStr The unit to return the rated values in.
     * @return A TimeSeriesMath of the rated values. The rated unit is the specified unit.
     * @throws RatingException on error
     */
    public TimeSeriesMath rate(TimeSeriesMath[] tsms, String ratedUnitStr) throws RatingException {
        return this.composedRatingSet.rate(tsms, ratedUnitStr);
    }

    /**
     * Retrieves the rating specification including all meta data.
     *
     * @return The rating specification
     */
    public RatingSpec getRatingSpec() {
        return this.composedRatingSet.getRatingSpec();
    }


    /**
     * @return the unique identifying parts for the rating specification.
     * @throws DataSetException on error
     */
    public IRatingSpecification getRatingSpecification() throws DataSetException {
        return this.composedRatingSet.getRatingSpecification();
    }

    /**
     * @return  the unique identifying parts for the rating template.
     * @throws DataSetException on error
     */
    public IRatingTemplate getRatingTemplate() throws DataSetException {
        return this.composedRatingSet.getRatingTemplate();
    }

    /**
     * Sets the rating specification.
     *
     * @param ratingSpec The rating specification
     * @throws RatingException on error
     */
    public void setRatingSpec(RatingSpec ratingSpec) throws RatingException {
        this.composedRatingSet.setRatingSpec(ratingSpec);
    }

    /**
     * Retrieves the times series of ratings.
     *
     * @return The times series of ratings.
     */
    public AbstractRating[] getRatings() {
        return this.composedRatingSet.getRatings();
    }

    public TreeMap<Long, AbstractRating> getRatingsMap() {
        return this.composedRatingSet.getRatingsMap();
    }

    public AbstractRating getRating(Long effectiveDate) {
        return this.composedRatingSet.getRating(effectiveDate);
    }

    public AbstractRating getFloorRating(Long effectiveDate) {
        return this.composedRatingSet.getFloorRating(effectiveDate);
    }

    /**
     * Sets the times series of ratings, replacing any existing ratings.
     *
     * @param ratings The time series of ratings
     * @throws RatingException on error
     */
    public void setRatings(AbstractRating[] ratings) throws RatingException {
        this.composedRatingSet.setRatings(ratings);
    }

    /**
     * Retrieves the number of ratings in this set.
     *
     * @return The number of ratings in this set
     */
    public int getRatingCount() {
        return this.composedRatingSet.getRatingCount();
    }

    /**
     * Retrieves the number of active ratings in this set.
     *
     * @return The number of active ratings in this set
     */
    public int getActiveRatingCount() {
        return this.composedRatingSet.getActiveRatingCount();
    }

    /**
     * Resets the default value time. This is used for rating values that have no inherent times.
     */
    public void resetDefaultValueTime() {
        this.composedRatingSet.resetDefaultValueTime();
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#getRatingTime()
     */
    @Override
    public long getRatingTime() {
        return this.composedRatingSet.getRatingTime();
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#setRatingTime(long)
     */
    @Override
    public void setRatingTime(long ratingTime) {
        this.composedRatingSet.setRatingTime(ratingTime);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#resetRatingtime()
     */
    @Override
    public void resetRatingTime() {
        this.composedRatingSet.resetRatingTime();
    }

    /**
     * Retrieves whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
     *
     * @return A flag specifying whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
     */
    public boolean doesAllowUnsafe() {
        return this.composedRatingSet.doesAllowUnsafe();
    }

    /**
     * Sets whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
     *
     * @param allowUnsafe A flag specifying whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
     */
    public void setAllowUnsafe(boolean allowUnsafe) {
        this.composedRatingSet.setAllowUnsafe(allowUnsafe);
    }

    /**
     * Retrieves whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
     *
     * @return A flag specifying whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
     */
    public boolean doesWarnUnsafe() {
        return this.composedRatingSet.doesWarnUnsafe();
    }

    /**
     * Sets whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
     *
     * @param warnUnsafe A flag specifying whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
     */
    public void setWarnUnsafe(boolean warnUnsafe) {
        this.composedRatingSet.setWarnUnsafe(warnUnsafe);
    }

    /**
     * Retrieves the standard HEC-DSS pathname for this rating set
     *
     * @return The standard HEC-DSS pathname for this rating set
     */
    public String getDssPathname() {
        return this.composedRatingSet.getDssPathname();
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#getName()
     */
    @Override
    public String getName() {
        return this.composedRatingSet.getName();
    }

    /* (non-Javadoc)
     * @see org.opendcs.IRating#setName(java.lang.String)
     */
    @Override
    public void setName(String name) throws RatingException {
        this.composedRatingSet.setName(name);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#getRatingParameters()
     */
    @Override
    public String[] getRatingParameters() {
        return this.composedRatingSet.getRatingParameters();
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#getRatingUnits()
     */
    @Override
    public String[] getRatingUnits() {
        return this.composedRatingSet.getRatingUnits();
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#getDataUnits()
     */
    @Override
    public String[] getDataUnits() {
        return this.composedRatingSet.getDataUnits();
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#setDataUnits(java.lang.String[])
     */
    @Override
    public void setDataUnits(String[] units) throws RatingException {
        this.composedRatingSet.setDataUnits(units);
    }

    /* (non-Javadoc)
     * @see org.opendcs.IRating#getRatingExtents()
     */
    @Override
    public double[][] getRatingExtents() throws RatingException {
        return this.composedRatingSet.getRatingExtents();
    }

    /* (non-Javadoc)
     * @see org.opendcs.IRating#getRatingExtents(long)
     */
    @Override
    public double[][] getRatingExtents(long ratingTime) throws RatingException {
        return this.composedRatingSet.getRatingExtents(ratingTime);
    }

    /* (non-Javadoc)
     * @see org.opendcs.IRating#getEffectiveDates()
     */
    @Override
    public long[] getEffectiveDates() {
        return this.composedRatingSet.getEffectiveDates();
    }

    /* (non-Javadoc)
     * @see org.opendcs.IRating#getCreateDates()
     */
    @Override
    public long[] getCreateDates() {
        return this.composedRatingSet.getCreateDates();
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#getDefaultValueTime()
     */
    @Override
    public long getDefaultValueTime() {
        return this.composedRatingSet.getDefaultValueTime();
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#setDefaultValueTime(long)
     */
    @Override
    public void setDefaultValueTime(long defaultValueTime) {
        this.composedRatingSet.setDefaultValueTime(defaultValueTime);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#rate(double)
     */
    @Override
    public double rate(double indVal) throws RatingException {
        return this.composedRatingSet.rate(indVal);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#rate(double[])
     */
    @Override
    public double rateOne(double... indVals) throws RatingException {
        return this.composedRatingSet.rateOne(indVals);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#rate(double[])
     */
    @Override
    public double rateOne2(double[] indVals) throws RatingException {
        return this.composedRatingSet.rateOne2(indVals);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#rateOne(double[])
     */
    @Override
    public double[] rate(double[] indVals) throws RatingException {
        return this.composedRatingSet.rate(indVals);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#rate(double[][])
     */
    @Override
    public double[] rate(double[][] indVals) throws RatingException {
        return this.composedRatingSet.rate(indVals);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#rate(long, double)
     */
    @Override
    public double rate(long valTime, double indVal) throws RatingException {
        return this.composedRatingSet.rate(valTime, indVal);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#rate(long, double[])
     */
    @Override
    public double rateOne(long valTime, double... indVals) throws RatingException {
        return this.composedRatingSet.rateOne(valTime, indVals);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#rate(long, double[])
     */
    @Override
    public double rateOne2(long valTime, double[] indVals) throws RatingException {
        return this.composedRatingSet.rateOne2(valTime, indVals);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#rateOne(long, double[])
     */
    @Override
    public double[] rate(long valTime, double[] indVals) throws RatingException {
        return this.composedRatingSet.rate(valTime, indVals);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#rateOne(long[], double[])
     */
    @Override
    public double[] rate(long[] valTimes, double[] indVals) throws RatingException {
        return this.composedRatingSet.rate(valTimes, indVals);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#rate(long, double[][])
     */
    @Override
    public double[] rate(long valTime, double[][] indVals) throws RatingException {
        return this.composedRatingSet.rate(valTime, indVals);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#rate(long[], double[][])
     */
    @Override
    public double[] rate(long[] valTimes, double[][] indVals) throws RatingException {
        return this.composedRatingSet.rate(valTimes, indVals);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#reverseRate(double)
     */
    @Override
    public double reverseRate(double depVal) throws RatingException {
        return this.composedRatingSet.reverseRate(depVal);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#reverseRate(double[])
     */
    @Override
    public double[] reverseRate(double[] depVals) throws RatingException {
        return this.composedRatingSet.reverseRate(depVals);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#reverseRate(long, double)
     */
    @Override
    public double reverseRate(long valTime, double depVal) throws RatingException {
        return this.composedRatingSet.reverseRate(valTime, depVal);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#reverseRate(long, double[])
     */
    @Override
    public double[] reverseRate(long valTime, double[] depVals) throws RatingException {
        return this.composedRatingSet.reverseRate(valTime, depVals);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#reverseRate(long[], double[])
     */
    @Override
    public double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException {
        return this.composedRatingSet.reverseRate(valTimes, depVals);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#reverseRate(hec.io.TimeSeriesContainer)
     */
    @Override
    public TimeSeriesContainer reverseRate(TimeSeriesContainer tsc) throws RatingException {
        return this.composedRatingSet.reverseRate(tsc);
    }

    /* (non-Javadoc)
     * @see org.opendcs.IRating#reverseRate(hec.hecmath.TimeSeriesMath)
     */
    @Override
    public TimeSeriesMath reverseRate(TimeSeriesMath tsm) throws RatingException {
        return this.composedRatingSet.reverseRate(tsm);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#getIndParamCount()
     */
    @Override
    public int getIndParamCount() throws RatingException {
        return this.composedRatingSet.getIndParamCount();
    }

    /**
     * @return a container of the object state
     */
    public RatingSetStateContainer getState() {
        return this.composedRatingSet.getState();
    }

    /**
     * Retrieves a RatingSetContainer containing the data of this object.
     *
     * @return The RatingSetContainer
     */
    public RatingSetContainer getData() {
        return this.composedRatingSet.getData();
    }

    /**
     * Sets the data from this object from a RatingSetContainer
     *
     * @param rsc The RatingSetContainer with the data
     * @throws RatingException any errors transferring the data
     */
    public void setData(RatingSetContainer rsc) throws RatingException {
        this.composedRatingSet.setData(rsc);
    }

    /**
     * Returns whether this object has any vertical datum info
     *
     * @return whether this object has any vertical datum info
     */
    public boolean hasVerticalDatum() {
        return this.composedRatingSet.hasVerticalDatum();
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#getNativeVerticalDatum()
     */
    @Override
    public String getNativeVerticalDatum() throws VerticalDatumException {
        return this.composedRatingSet.getNativeVerticalDatum();
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#getCurrentVerticalDatum()
     */
    @Override
    public String getCurrentVerticalDatum() throws VerticalDatumException {
        return this.composedRatingSet.getCurrentVerticalDatum();
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#isCurrentVerticalDatumEstimated()
     */
    @Override
    public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
        return this.composedRatingSet.isCurrentVerticalDatumEstimated();
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#toNativeVerticalDatum()
     */
    @Override
    public boolean toNativeVerticalDatum() throws VerticalDatumException {
        return this.composedRatingSet.toNativeVerticalDatum();
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#toNGVD29()
     */
    @Override
    public boolean toNGVD29() throws VerticalDatumException {
        return this.composedRatingSet.toNGVD29();
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#toNAVD88()
     */
    @Override
    public boolean toNAVD88() throws VerticalDatumException {
        return this.composedRatingSet.toNAVD88();
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#toVerticalDatum(java.lang.String)
     */
    @Override
    public boolean toVerticalDatum(String datum) throws VerticalDatumException {
        return this.composedRatingSet.toVerticalDatum(datum);
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#forceVerticalDatum(java.lang.String)
     */
    @Override
    public boolean forceVerticalDatum(String datum) throws VerticalDatumException {
        return this.composedRatingSet.forceVerticalDatum(datum);
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#getCurrentOffset()
     */
    @Override
    public double getCurrentOffset() throws VerticalDatumException {
        return this.composedRatingSet.getCurrentOffset();
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#getCurrentOffset(java.lang.String)
     */
    @Override
    public double getCurrentOffset(String unit) throws VerticalDatumException {
        return this.composedRatingSet.getCurrentOffset(unit);
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#getNGVD29Offset()
     */
    @Override
    public double getNGVD29Offset() throws VerticalDatumException {
        return this.composedRatingSet.getNGVD29Offset();
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#getNGVD29Offset(java.lang.String)
     */
    @Override
    public double getNGVD29Offset(String unit) throws VerticalDatumException {
        return this.composedRatingSet.getNGVD29Offset(unit);
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#getNAVD88Offset()
     */
    @Override
    public double getNAVD88Offset() throws VerticalDatumException {
        return this.composedRatingSet.getNAVD88Offset();
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#getNAVD88Offset(java.lang.String)
     */
    @Override
    public double getNAVD88Offset(String unit) throws VerticalDatumException {
        return this.composedRatingSet.getNAVD88Offset(unit);
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#isNGVD29OffsetEstimated()
     */
    @Override
    public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
        return this.composedRatingSet.isNGVD29OffsetEstimated();
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#isNAVD88OffsetEstimated()
     */
    @Override
    public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
        return this.composedRatingSet.isNAVD88OffsetEstimated();
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#getVerticalDatumInfo()
     */
    @Override
    public String getVerticalDatumInfo() throws VerticalDatumException {
        return this.composedRatingSet.getVerticalDatumInfo();
    }

    /* (non-Javadoc)
     * @see mil.army.usace.hec.metadata.VerticalDatum#setVerticalDatumInfo(java.lang.String)
     */
    @Override
    public void setVerticalDatumInfo(String xmlStr) throws VerticalDatumException {
        this.composedRatingSet.setVerticalDatumInfo(xmlStr);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof RatingSet && this.composedRatingSet.equals(obj);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.AbstractRating#hashCode()
     */
    @Override
    public int hashCode() {
        return this.composedRatingSet.hashCode();
    }

    /**
     * If dbrating == null, this method returns the first VerticalDatumContainer found in the AbstractRatings.
     * Otherwise it returns the vertical datum container from the dbrating.
     *
     * @return NULL
     */
    @Override
    public VerticalDatumContainer getVerticalDatumContainer() {
        return this.composedRatingSet.getVerticalDatumContainer();
    }

    /**
     * If dbrating == null, this method sets the VerticalDatumContainer on all AbstractRatings.
     * Otherwise it sets the vertical datum container from the dbrating.
     *
     * @param vdc vertical datum data
     */
    public void setVerticalDatumContainer(VerticalDatumContainer vdc) {
        this.composedRatingSet.setVerticalDatumContainer(vdc);
    }


    /**
     * @return the latest creation or effective date for all the included ratings
     */
    protected final long getReferenceTime() {
        synchronized (this) {
            long referenceTime = UNDEFINED_TIME;
            for (AbstractRating rating : getRatings()) {
                long t = getReferenceTime(rating);
                if (t > referenceTime) {
                    referenceTime = t;
                }
            }
            return referenceTime;
        }
    }

    /**
     * @param rating The rating item to return the reference time for
     * @return the latest creation or effective date for this rating or its component parts
     */
    protected final long getReferenceTime(AbstractRating rating) {
        synchronized (this) {
            long referenceTime = Math.max(rating.createDate, rating.effectiveDate);
            if (rating instanceof UsgsStreamTableRating) {
                UsgsStreamTableRating sr = (UsgsStreamTableRating) rating;
                referenceTime = Math.max(referenceTime, getReferenceTime(sr.offsets));
                if (sr.shifts != null) {
                    referenceTime = Math.max(referenceTime, sr.shifts.getReferenceTime());
                }
            } else if (rating instanceof VirtualRating) {
                VirtualRating vr = (VirtualRating) rating;
                if (vr.sourceRatings != null) {
                    for (SourceRating sr : vr.sourceRatings) {
                        if (sr.ratings != null) {
                            referenceTime = Math.max(referenceTime, sr.ratings.getReferenceTime());
                        }
                    }
                }
            } else if (rating instanceof TransitionalRating) {
                TransitionalRating tr = (TransitionalRating) rating;
                if (tr.sourceRatings != null) {
                    for (SourceRating sr : tr.sourceRatings) {
                        if (sr.ratings != null) {
                            referenceTime = Math.max(referenceTime, sr.ratings.getReferenceTime());
                        }
                    }
                }
            }
            return referenceTime;
        }
    }
    /**
     * Collects rating specs used by rating and components
     *
     * @param rating               the rating to inspect
     * @param componentRatingSpecs the set of rating specs to collect into
     */
    protected final void getComponentRatingSpecs(AbstractRating rating, HashSet<String> componentRatingSpecs) {
        synchronized (this) {
            componentRatingSpecs.add(rating.getRatingSpecId());
            if (rating instanceof UsgsStreamTableRating) {
                UsgsStreamTableRating sr = (UsgsStreamTableRating) rating;
                if (sr.offsets != null) {
                    getComponentRatingSpecs(sr.offsets, componentRatingSpecs);
                }
                if (sr.shifts != null) {
                    componentRatingSpecs.addAll(sr.shifts.getComponentRatingSpecs());
                }
            } else if (rating instanceof VirtualRating) {
                VirtualRating vr = (VirtualRating) rating;
                if (vr.sourceRatings != null) {
                    for (SourceRating sr : vr.sourceRatings) {
                        if (sr.ratings != null) {
                            componentRatingSpecs.addAll(sr.ratings.getComponentRatingSpecs());
                        }
                    }
                }
            } else if (rating instanceof TransitionalRating) {
                TransitionalRating tr = (TransitionalRating) rating;
                if (tr.sourceRatings != null) {
                    for (SourceRating sr : tr.sourceRatings) {
                        if (sr.ratings != null) {
                            componentRatingSpecs.addAll(sr.ratings.getComponentRatingSpecs());
                        }
                    }
                }
            }
        }
    }

    /**
     * @return all rating specs used in this rating set
     */
    protected final HashSet<String> getComponentRatingSpecs() {
        synchronized (this) {
            HashSet<String> componentRatingSpecs = new HashSet<>();
            for (AbstractRating rating : getRatings()) {
                getComponentRatingSpecs(rating, componentRatingSpecs);
            }
            return componentRatingSpecs;
        }
    }

    protected final boolean hasNullValues() {
        for (AbstractRating r : getRatings()) {
            if (r instanceof TableRating) {
                TableRating tr = (TableRating) r;
                if (tr.values == null) {
                    return true;
                }
            } else if (r instanceof VirtualRating) {
                for (SourceRating sr : ((VirtualRating) r).getSourceRatings()) {
                    if (sr.ratings != null) {
                        if (sr.ratings.hasNullValues()) {
                            return true;
                        }
                    }
                }
            } else if (r instanceof TransitionalRating) {
                for (SourceRating sr : ((TransitionalRating) r).getSourceRatings()) {
                    if (sr.ratings != null) {
                        if (sr.ratings.hasNullValues()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}