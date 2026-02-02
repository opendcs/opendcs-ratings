/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package org.opendcs.ratings;


import hec.data.cwmsRating.io.RatingSetContainer;
import hec.hecmath.TimeSeriesMath;
import hec.io.TimeSeriesContainer;
import java.util.NavigableMap;
import java.util.NavigableSet;
import mil.army.usace.hec.metadata.VerticalDatum;

public interface CwmsRatingSet extends VerticalDatum {
    void addRating(AbstractRating rating)  throws RatingException;

    void addRatings(Iterable<AbstractRating> ratings)  throws RatingException;

    boolean doesAllowUnsafe();

    boolean doesWarnUnsafe();

    int getActiveRatingCount();

    void getConcreteRatings() throws RatingException;

    void getConcreteRatings(long date) throws RatingException;

    long[] getCreateDates();

    RatingSetContainer getData();

    String[] getDataUnits();

    long getDefaultValueTime();

    long[] getEffectiveDates();

    AbstractRating getFloorRating(Long effectiveDate);

    AbstractRating getRating(Long effectiveDate);

    int getRatingCount();

    double[][] getRatingExtents() throws RatingException;

    double[][] getRatingExtents(long ratingTime) throws RatingException;

    NavigableSet<AbstractRating> getRatingsSorted();

    NavigableMap<Long, AbstractRating> getRatingsMap();

    RatingSpec getRatingSpec();

    long getRatingTime();

    String[] getRatingUnits();

    double rate(double indVal)throws RatingException;

    double rate(long valueTime, double indVal)throws RatingException;

    double[] rate(double[] indVals)throws RatingException;

    double[] rate(long valueTime, double[] indVals)throws RatingException;

    double[] rate(long[] valTimes, double[] indVals)throws RatingException;

    double[] rate(double[][] indValis)throws RatingException;

    double[] rate(long valueTime, double[][] indVals)throws RatingException;

    double[] rate(long[] valTimes, double[][] indVals)throws RatingException;

    TimeSeriesContainer rate(TimeSeriesContainer tsc)throws RatingException;

    TimeSeriesContainer rate(TimeSeriesContainer tsc, String ratedUnitStr)throws RatingException;

    TimeSeriesContainer rate(TimeSeriesContainer[] tscs)throws RatingException;

    TimeSeriesContainer rate(TimeSeriesContainer[] tscs, String ratedUnitStr)throws RatingException;

    TimeSeriesMath rate(TimeSeriesMath tsm)throws RatingException;

    TimeSeriesMath rate(TimeSeriesMath tsm, String ratedUnitStr)throws RatingException;

    TimeSeriesMath rate(TimeSeriesMath[] tsms)throws RatingException;

    TimeSeriesMath rate(TimeSeriesMath[] tsms, String ratedUnitStr)throws RatingException;

    double rateOne(double... indVals)throws RatingException;

    double rateOne(long valueTime, double[] indVals)throws RatingException;

    double[] rateOne(long[] valueTimes, double[] indVals) throws RatingException;

    void removeAllRatings();

    void removeRating(long effectiveDate) throws RatingException;

    void replaceRating(AbstractRating rating) throws RatingException;

    void replaceRatings(Iterable<AbstractRating> ratings) throws RatingException;

    void resetDefaultValueTime();

    void resetRatingTime();

    double reverseRate(double depVal) throws RatingException;

    double[] reverseRate(double[] depVals) throws RatingException;

    double reverseRate(long valTime, double depVal) throws RatingException;

    double[] reverseRate(long valTime, double[] depVal) throws RatingException;

    double[] reverseRate(long[] valTime, double[] depVal) throws RatingException;

    TimeSeriesContainer reverseRate(TimeSeriesContainer tsc) throws RatingException;

    TimeSeriesMath reverseRate(TimeSeriesMath tsm) throws RatingException;

    void setAllowUnsafe(boolean allowUnsafe);

    void setDataUnits(String[] units) throws RatingException;

    void setDefaultValueTime(long defaultValueTime);

    void setRatings(AbstractRating[] ratings) throws RatingException;

    void setRatingSpec(RatingSpec ratingSpec) throws RatingException;

    void setRatingTime(long ratingTime) throws RatingException;

    void setWarnUnsafe(boolean warnUnsafe) throws RatingException;
}
