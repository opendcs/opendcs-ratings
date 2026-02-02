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

package org.opendcs.ratings;


import hec.hecmath.TimeSeriesMath;
import hec.io.TimeSeriesContainer;
import java.sql.Connection;
import mil.army.usace.hec.metadata.VerticalDatumException;

public final class ConcreteRatingSet extends AbstractRatingSet {

    ConcreteRatingSet() {
        super();
    }

    @Override
    public void clearDatabaseConnection() {
        //No-op
    }

    @Override
    public void getConcreteRatings() throws RatingException {
        //No-op
    }

    @Override
    public void getConcreteRatings(long date) throws RatingException {
        //No-op
    }

    @Override
    public void getConcreteRatings(Connection conn) throws RatingException {
        //No-op
    }

    @Override
    public String[] getDataUnits(Connection conn) {
        return getDataUnits();
    }

    public long[] getCreateDates(Connection conn) {
        return getCreateDates();
    }

    public long[] getEffectiveDates(Connection conn) {
        return getEffectiveDates();
    }

    @Override
    public String getNativeVerticalDatum(Connection conn) throws VerticalDatumException {
        return getNativeVerticalDatum();
    }

    public double[][] getRatingExtents(Connection conn) throws RatingException {
        return getRatingExtents();
    }

    public double[][] getRatingExtents(Connection conn, long ratingTime) throws RatingException {
        return getRatingExtents(ratingTime);
    }

    @Override
    public synchronized double[][] getRatingExtents(long ratingTime, Connection conn) throws RatingException {
        return getRatingExtents(ratingTime);
    }

    @Override
    public boolean isUpdated(Connection conn) throws Exception {
        return true;
    }

    @Override
    public double rate(Connection conn, double indVal) throws RatingException {
        return rate(indVal);
    }

    @Override
    public double rate(Connection conn, long valTime, double indVal) throws RatingException {
        return rate(valTime, indVal);
    }

    @Override
    public double[] rate(Connection conn, double[] indVals) throws RatingException {
        return rate(indVals);
    }

    @Override
    public double[] rate(Connection conn, double[][] indVals) throws RatingException {
        return rate(indVals);
    }

    @Override
    public double[] rate(Connection conn, long valTime, double[] indVals) throws RatingException {
        return rate(valTime, indVals);
    }

    @Override
    public double[] rate(Connection conn, long valTime, double[][] indVals) throws RatingException {
        return rate(valTime, indVals);
    }

    @Override
    public double[] rate(Connection conn, long[] valTimes, double[] indVals) throws RatingException {
        return rate(valTimes, indVals);
    }

    @Override
    public double[] rate(Connection conn, long[] valTimes, double[][] indVals) throws RatingException {
        return rate(valTimes, indVals);
    }

    @Override
    public TimeSeriesContainer rate(Connection conn, TimeSeriesContainer tsc) throws RatingException {
        return rate(tsc);
    }

    @Override
    public TimeSeriesContainer rate(Connection conn, TimeSeriesContainer[] tscs) throws RatingException {
        return rate(tscs);
    }

    @Override
    public TimeSeriesMath rate(Connection conn, TimeSeriesMath tsm) throws RatingException {
        return rate(tsm);
    }

    @Override
    public TimeSeriesMath rate(Connection conn, TimeSeriesMath[] tsms) throws RatingException {
        return rate(tsms);
    }

    @Override
    public double rateOne(Connection conn, double... indVals) throws RatingException {
        return rateOne(indVals);
    }

    @Override
    public double rateOne(Connection conn, long valTime, double... indVals) throws RatingException {
        return rateOne(valTime, indVals);
    }

    @Override
    public double rateOne2(Connection conn, double[] indVals) throws RatingException {
        return rateOne2(indVals);
    }

    @Override
    public double rateOne2(Connection conn, long valTime, double[] indVals) throws RatingException {
        return rateOne2(valTime, indVals);
    }

    @Override
    public double reverseRate(Connection conn, double depVal) throws RatingException {
        return reverseRate(depVal);
    }

    @Override
    public double reverseRate(Connection conn, long valTime, double depVal) throws RatingException {
        return reverseRate(valTime, depVal);
    }

    @Override
    public double[] reverseRate(Connection conn, double[] depVals) throws RatingException {
        return reverseRate(depVals);
    }

    @Override
    public double[] reverseRate(Connection conn, long valTime, double[] depVals) throws RatingException {
        return reverseRate(valTime, depVals);
    }

    @Override
    public double[] reverseRate(Connection conn, long[] valTimes, double[] depVals) throws RatingException {
        return reverseRate(valTimes, depVals);
    }

    @Override
    public TimeSeriesContainer reverseRate(Connection conn, TimeSeriesContainer tsc) throws RatingException {
        return reverseRate(tsc);
    }

    @Override
    public TimeSeriesMath reverseRate(Connection conn, TimeSeriesMath tsm) throws RatingException {
        return reverseRate(tsm);
    }

    @Override
    public void setDataUnits(Connection conn, String[] units) throws RatingException {
        setDataUnits(units);
    }

    @Deprecated
    @Override
    public void setData(DatabaseLoadMethod loadMethod, Connection conn, String officeId, String ratingSpecId, Long startTime, Long endTime,
                        boolean dataTimes) throws RatingException {
        throw new RatingException(
            "setData(DatabaseLoadMethod, Connection, OfficeId, RatingSpecId, StartTime, EndTime, DataTimes) method unsupported for concrete ratings. Use factory methods instead");
    }

    @Override
    public void storeToDatabase(Connection conn, boolean overwriteExisting) throws RatingException {
        throw new RatingException(
            "storeToDatabase(Connection, OverwriteExisting) method unsupported for concrete ratings. Use factory methods instead");
    }

    @Override
    public void storeToDatabase(Connection conn, boolean overwriteExisting, boolean includeTemplate) throws RatingException {
        throw new RatingException(
            "storeToDatabase(Connection, OverwriteExisting, IncludeTemplate) method unsupported for concrete ratings. Use factory methods instead");
    }
}
