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
import org.opendcs.ratings.RatingSpec;
import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.io.RatingSetContainer;
import org.opendcs.ratings.io.ReferenceRatingContainer;
import hec.io.TextContainer;
import java.sql.Connection;
import java.util.Arrays;
import mil.army.usace.hec.metadata.VerticalDatumContainer;
import mil.army.usace.hec.metadata.VerticalDatumException;

public final class ReferenceJdbcRatingSet extends JdbcRatingSet {
    /**
     * Rating object for rating by reference
     */
    private ReferenceRating dbrating;

    ReferenceJdbcRatingSet(ConnectionProvider conn, DbInfo dbInfo, ReferenceRating dbrating) {
        super(conn, dbInfo);
        this.dbrating = dbrating;
    }

    /**
     * Sets the database connection for this RatingSet and any constituent RatingSet objects
     *
     * @param conn the connection
     */
    @Override
    public synchronized void setDatabaseConnection(Connection conn) {
        setTransientConnectionProvider(conn);
    }

    /**
     * Sets the database info required to retrieve a database connection
     *
     * @param dbInfo the database info required to retrieve a database connection
     */
    @Override
    public synchronized void setDbInfo(DbInfo dbInfo) {
        this.dbInfo = dbInfo;
    }

    /**
     * Adds multiple ratings to the existing ratings.
     *
     * @param ratings The ratings to add
     * @throws RatingException various errors with the input such a undefined effective dates,
     *                         effective date already exists, number of independent parameters not consistent,
     *                         rating specs not consistent, units incompatible, templates not consistent
     */
    @Override
    public void addRatings(Iterable<AbstractRating> ratings) throws RatingException {
        throw new RatingException("Cannot add to a reference rating");
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
    @Override
    public synchronized double[] rate(double[][] valueSets, long[] valueTimes) throws RatingException {
        return dbrating.rate(valueTimes, valueSets);
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#getRatingUnits()
     */
    @Override
    public synchronized String[] getRatingUnits() {
        return dbrating.getRatingUnits();
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#getDataUnits()
     */
    @Override
    public synchronized String[] getDataUnits() {
        String[] units;
        if (dataUnits != null) {
            units = Arrays.copyOf(dataUnits, dataUnits.length);
        } else {
            units = dbrating.getDataUnits();
        }
        return units;
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#setDataUnits(java.lang.String[])
     */
    @Override
    public synchronized void setDataUnits(String[] units) throws RatingException {
        dbrating.setDataUnits(units);
        dataUnits = units == null ? null : Arrays.copyOf(units, units.length);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getRatingExtents()
     */
    @Override
    public double[][] getRatingExtents() throws RatingException {
        return dbrating.getRatingExtents();
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getRatingExtents(long)
     */
    @Override
    public synchronized double[][] getRatingExtents(long ratingTime) throws RatingException {
        return dbrating.getRatingExtents(ratingTime);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getEffectiveDates()
     */
    @Override
    public synchronized long[] getEffectiveDates() {
        return dbrating.getEffectiveDates();
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getCreateDates()
     */
    @Override
    public synchronized long[] getCreateDates() {
        return dbrating.getCreateDates();
    }

    /* (non-Javadoc)
     * @see org.opendcs.ratings.IRating#reverseRate(long[], double[])
     */
    @Override
    public synchronized double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException {
        return dbrating.reverseRate(valTimes, depVals);
    }

    /**
     * Sets the data from this object from a RatingSetContainer
     *
     * @param rsc The RatingSetContainer with the data
     * @throws RatingException any errors transferring the data
     */
    @Override
    public synchronized void setData(RatingSetContainer rsc) throws RatingException {
        try {
            dbInfo = null;
            removeAllRatings();
            if (rsc.ratingSpecContainer == null) {
                ratingSpec = null;
            } else {
                setRatingSpec(new RatingSpec(rsc.ratingSpecContainer));
            }
            if (rsc instanceof ReferenceRatingContainer) {
                dbrating = new ReferenceRating((ReferenceRatingContainer) rsc);
                dbrating.parent = this;
            } else {
                throw new RatingException("Unable to transform a reference rating into a RatingSetContainer of type: " + rsc.getClass());
            }
            if (rsc.state != null) {
                setState(rsc.state);
            }
            if (observationTarget != null) {
                observationTarget.setChanged();
                observationTarget.notifyObservers();
            }
        } catch (RuntimeException t) {
            throw new RatingException(t);
        }
    }

    @Override
    public synchronized RatingSetContainer getData() {
        RatingSetContainer rsc = new ReferenceRatingContainer();
        if (ratingSpec != null) {
            rsc.ratingSpecContainer = ratingSpec.getData();
        }
        rsc.state = getState();
        return rsc;
    }

    /**
     * Retrieves a TextContainer containing the data of this object, suitable for storing to DSS.
     *
     * @return The TextContainer
     * @throws RatingException any errors reading from dss or processing the data
     */
    @Override
    public TextContainer getDssData() throws RatingException {
        throw new RatingException("Reference ratings cannot return DSS Data.");
    }

    @Override
    public boolean hasVerticalDatum() {
        return getData().hasVerticalDatum();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNativeVerticalDatum()
     */
    @Override
    public String getNativeVerticalDatum() throws VerticalDatumException {
        return dbrating.getNativeVerticalDatum();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getCurrentVerticalDatum()
     */
    @Override
    public String getCurrentVerticalDatum() throws VerticalDatumException {
        return dbrating.getCurrentVerticalDatum();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#isCurrentVerticalDatumEstimated()
     */
    @Override
    public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
        return dbrating.isCurrentVerticalDatumEstimated();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toNativeVerticalDatum()
     */
    @Override
    public boolean toNativeVerticalDatum() throws VerticalDatumException {
        return dbrating.toNativeVerticalDatum();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toNGVD29()
     */
    @Override
    public boolean toNGVD29() throws VerticalDatumException {
        return dbrating.toNGVD29();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toNAVD88()
     */
    @Override
    public boolean toNAVD88() throws VerticalDatumException {
        return dbrating.toNAVD88();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toVerticalDatum(java.lang.String)
     */
    @Override
    public boolean toVerticalDatum(String datum) throws VerticalDatumException {
        return dbrating.toVerticalDatum(datum);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#forceVerticalDatum(java.lang.String)
     */
    @Override
    public boolean forceVerticalDatum(String datum) throws VerticalDatumException {
        return dbrating.forceVerticalDatum(datum);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getCurrentOffset()
     */
    @Override
    public double getCurrentOffset() throws VerticalDatumException {
        return dbrating.getCurrentOffset();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getCurrentOffset(java.lang.String)
     */
    @Override
    public double getCurrentOffset(String unit) throws VerticalDatumException {
        return dbrating.getCurrentOffset(unit);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNGVD29Offset()
     */
    @Override
    public double getNGVD29Offset() throws VerticalDatumException {
        return dbrating.getNGVD29Offset();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNGVD29Offset(java.lang.String)
     */
    @Override
    public double getNGVD29Offset(String unit) throws VerticalDatumException {
        return dbrating.getNGVD29Offset(unit);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNAVD88Offset()
     */
    @Override
    public double getNAVD88Offset() throws VerticalDatumException {
        return dbrating.getNAVD88Offset();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNAVD88Offset(java.lang.String)
     */
    @Override
    public double getNAVD88Offset(String unit) throws VerticalDatumException {
        return dbrating.getNAVD88Offset(unit);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#isNGVD29OffsetEstimated()
     */
    @Override
    public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
        return dbrating.isNGVD29OffsetEstimated();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#isNAVD88OffsetEstimated()
     */
    @Override
    public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
        return dbrating.isNAVD88OffsetEstimated();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getVerticalDatumInfo()
     */
    @Override
    public String getVerticalDatumInfo() throws VerticalDatumException {
        return dbrating.getVerticalDatumInfo();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#setVerticalDatumInfo(java.lang.String)
     */
    @Override
    public void setVerticalDatumInfo(String xmlStr) throws VerticalDatumException {
        dbrating.setVerticalDatumInfo(xmlStr);
    }

    /**
     * Validates the rating set
     *
     * @throws RatingException if the rating set is not valid
     */
    @Override
    protected void validate() throws RatingException {
        if (dbrating == null) {
            throw new RatingException("reference rating is null - cannnot validate");
        }
    }

    /**
     * If dbrating == null, this method returns the first VerticalDatumContainer found in the AbstractRatings.
     * Otherwise it returns the vertical datum container from the dbrating.
     *
     * @return NULL
     */
    @Override
    public VerticalDatumContainer getVerticalDatumContainer() {
        return dbrating.getVerticalDatumContainer();
    }

    /**
     * If dbrating == null, this method sets the VerticalDatumContainer on all AbstractRatings.
     * Otherwise it sets the vertical datum container from the dbrating.
     *
     * @param vdc vertical datum data
     */
    @Override
    public void setVerticalDatumContainer(VerticalDatumContainer vdc) {
        dbrating.setVerticalDatumContainer(vdc);
    }
}