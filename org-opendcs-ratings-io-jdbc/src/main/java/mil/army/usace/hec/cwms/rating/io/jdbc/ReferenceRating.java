/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.jdbc;

import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import static hec.lang.Const.UNDEFINED_LONG;
import static hec.lang.Const.UNDEFINED_TIME;

import hec.data.DataSetIllegalArgumentException;
import hec.data.Parameter;

import hec.data.Units;
import hec.data.cwmsRating.IRating;
import hec.data.cwmsRating.RatingSet;
import hec.data.cwmsRating.RatingException;
import hec.data.cwmsRating.TimeSeriesRater;
import hec.data.cwmsRating.io.ReferenceRatingContainer;
import hec.hecmath.TimeSeriesMath;
import hec.io.TimeSeriesContainer;
import hec.util.TextUtil;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import mil.army.usace.hec.metadata.VerticalDatum;
import mil.army.usace.hec.metadata.VerticalDatumContainer;
import mil.army.usace.hec.metadata.VerticalDatumException;
import mil.army.usace.hec.metadata.constants.NumericalConstants;
import org.jooq.Configuration;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import usace.cwms.db.jooq.codegen.packages.CWMS_LOC_PACKAGE;
import usace.cwms.db.jooq.codegen.packages.CWMS_RATING_PACKAGE;
import usace.cwms.db.jooq.codegen.packages.CWMS_UTIL_PACKAGE;
import usace.cwms.db.jooq.codegen.packages.cwms_rating.GET_RATING_EXTENTS;
import usace.cwms.db.jooq.codegen.tables.AV_RATING;
import usace.cwms.db.jooq.codegen.tables.AV_RATING_SPEC;
import usace.cwms.db.jooq.codegen.udt.records.DATE_TABLE_TYPE;
import usace.cwms.db.jooq.codegen.udt.records.DOUBLE_TAB_T;
import usace.cwms.db.jooq.codegen.udt.records.DOUBLE_TAB_TAB_T;
import usace.cwms.db.jooq.codegen.udt.records.STR_TAB_T;


/**
 * A rating that references rating data and methods in a live database.  The method calls on an object of this class
 * are executed in the database, with parameters and return values being communicated between the object and the
 * database as necessary.  This object is analogous to the RatingSet class in that multiple ratings under the same
 * rating specification may be used, depending on their effective dates.
 *
 * @author Mike Perryman
 */
public class ReferenceRating implements IRating, VerticalDatum {

    protected static final Logger logger = Logger.getLogger(ReferenceRating.class.getPackage().getName());

    long defaultValueTime = UNDEFINED_TIME;

    long ratingTime = Long.MAX_VALUE;

    protected String ratingSpecId = null;

    protected long ratingSpecCode = UNDEFINED_LONG;

    protected String locationId = null;

    protected String templateId = null;

    protected String parametersId = null;

    protected String templateVersion = null;

    protected String specificationVersion = null;

    protected String officeId = null;

    protected String[] parameters = null;

    protected int[] elevPositions = null;

    protected String[] ratingUnits = null;

    protected String[] dataUnits = null;

    protected VerticalDatumContainer vdc = null;

    protected TimeSeriesRater tsRater = null;

    protected JdbcRatingSet parent = null;

    public ReferenceRating(ReferenceRatingContainer rrc) throws RatingException {
        setData(rrc);
        resetRatingTime();
    }

    /**
     * Method to create a reference rating from the database
     *
     * @param conn         The database connection
     * @param officeId     The office identifier
     * @param ratingSpecId The rating specification identifier
     * @return A new ReferenceRating object
     * @throws RatingException
     */
    public static ReferenceRating fromDatabase(Connection conn, String officeId, String ratingSpecId) throws RatingException {
        return new ReferenceRating(conn, officeId, ratingSpecId);
    }

    protected ReferenceRating(Connection conn, String officeId, String ratingSpecId) throws RatingException {
        this.ratingSpecId = ratingSpecId;
        this.officeId = officeId.toUpperCase();
        try {
            Long ratingSpecCode = getRatingSpecCode(conn);
            this.ratingSpecCode = ratingSpecCode;
        } catch (RuntimeException e) {
            throw new RatingException(e);
        }


        String[] parts = TextUtil.split(ratingSpecId, SEPARATOR1);
        if (parts.length != 4) {
            throw new RatingException(String.format("Invalid rating specification: %s", ratingSpecId));
        }
        locationId = parts[0];
        parametersId = parts[1];
        templateVersion = parts[2];
        specificationVersion = parts[3];
        templateId = TextUtil.join(SEPARATOR1, parametersId, templateVersion);
        parameters = TextUtil.split(parametersId.replace(SEPARATOR2, SEPARATOR3), SEPARATOR3);
        ratingUnits = new String[parameters.length];
        try {
            for (int i = 0; i < parameters.length; ++i) {
                ratingUnits[i] = (new Parameter(parameters[i])).getUnitsStringForSystem(Units.ENGLISH_ID);
            }
        } catch (DataSetIllegalArgumentException e) {
            throw new IllegalArgumentException(e);
        }
        List<Integer> elevPosList = new ArrayList<Integer>();
        for (int i = 0; i < parameters.length; ++i) {
            try {
                new Parameter(parameters[i]);
            } catch (DataSetIllegalArgumentException e) {
                throw new RatingException(e);
            }
            if (parameters[i].toUpperCase().startsWith("ELEV")) {
                elevPosList.add(i);
            }
        }
        elevPositions = new int[elevPosList.size()];
        for (int i = 0; i < elevPosList.size(); ++i) {
            elevPositions[i] = elevPosList.get(i);
        }
        if (elevPositions.length > 0) {
            try {
                Configuration configuration = DSL.using(conn, SQLDialect.ORACLE).configuration();
                String verticalDatumInfo = CWMS_LOC_PACKAGE.call_GET_VERTICAL_DATUM_INFO_F__2(configuration, locationId, "ft", officeId);
                if (verticalDatumInfo != null) {
                    vdc = new VerticalDatumContainer(verticalDatumInfo);
                }
            } catch (Exception e) {
                RatingSet.getLogger().warning(String.format("Vertical datum initialzation failed: %s", e.getMessage()));
            }
        }
        resetRatingTime();
    }

    private Long getRatingSpecCode(Connection conn) throws RatingException {
        Result<Record1<Long>> result = DSL.using(conn, SQLDialect.ORACLE)
                                          .select(AV_RATING_SPEC.AV_RATING_SPEC.RATING_SPEC_CODE)
                                          .from(AV_RATING_SPEC.AV_RATING_SPEC)
                                          .where(AV_RATING_SPEC.AV_RATING_SPEC.OFFICE_ID.equalIgnoreCase(this.officeId))
                                          .and(AV_RATING_SPEC.AV_RATING_SPEC.RATING_ID.equalIgnoreCase(this.ratingSpecId))
                                          .getResult();
        if (result.isEmpty()) {
            throw new RatingException(String.format("No such rating: %s/%s", this.officeId, this.ratingSpecId));
        }
        Long ratingSpecCode = result.get(0).component1();
        return ratingSpecCode;
    }

    /**
     * @return a the current database connection plus a flag specifying whether it was retrieved using the DbInfo
     * @throws RatingException
     */
    protected synchronized Connection getConnection() throws RatingException {
        if (parent != null) {
            return parent.getConnection();
        } else {
            throw new RatingException("ReferenceRating object has no parent RatingSet object to use for database connections");
        }
    }

    /**
     * Releases a database connection that was retrieved using the DbInfo
     *
     * @param ci The database connection information
     * @throws RatingException
     */
    protected void releaseConnection(Connection ci) throws RatingException {
        parent.releaseConnection(ci);
    }

    protected void setData(ReferenceRatingContainer rrc) throws RatingException {
        if (rrc.ratingSpecContainer != null) {
            ratingSpecId = rrc.ratingSpecContainer.specId;
            officeId = rrc.ratingSpecContainer.specOfficeId == null ? rrc.ratingSpecContainer.officeId : rrc.ratingSpecContainer.specOfficeId;
            if (rrc.hasVerticalDatum()) {
                try {
                    setVerticalDatumInfo(rrc.getVerticalDatumInfo());
                } catch (Exception e) {
                    throw new RatingException(e);
                }
            }
            String[] parts = TextUtil.split(ratingSpecId, SEPARATOR1);
            if (parts.length != 4) {
                throw new RatingException(String.format("Invalid rating specification: %s", ratingSpecId));
            }
            locationId = parts[0];
            parametersId = parts[1];
            templateVersion = parts[2];
            specificationVersion = parts[3];
            templateId = TextUtil.join(SEPARATOR1, parametersId, templateVersion);
            parameters = TextUtil.split(parametersId.replace(SEPARATOR2, SEPARATOR3), SEPARATOR3);
            ratingUnits = new String[parameters.length];
            try {
                for (int i = 0; i < parameters.length; ++i) {
                    ratingUnits[i] = (new Parameter(parameters[i])).getUnitsStringForSystem(Units.ENGLISH_ID);
                }
            } catch (DataSetIllegalArgumentException e) {
                throw new IllegalArgumentException(e);
            }
            List<Integer> elevPosList = new ArrayList<Integer>();
            for (int i = 0; i < parameters.length; ++i) {
                try {
                    new Parameter(parameters[i]);
                } catch (DataSetIllegalArgumentException e) {
                    throw new RatingException(e);
                }
                if (parameters[i].toUpperCase().startsWith("ELEV")) {
                    elevPosList.add(i);
                }
            }
            elevPositions = new int[elevPosList.size()];
            for (int i = 0; i < elevPosList.size(); ++i) {
                elevPositions[i] = elevPosList.get(i);
            }
        }
    }

    protected synchronized void populateRatingSpecCode() throws RatingException {
        Connection conn = getConnection();
        try {
            ratingSpecCode = getRatingSpecCode(conn);
        } catch (RuntimeException e) {
            throw new RatingException(e);
        } finally {
            releaseConnection(conn);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int hashCode = getClass().getName().hashCode();
        try {
            hashCode += getConnection().hashCode();
        } catch (RatingException e) {
        }
        return hashCode;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        boolean result = obj == this;
        if (!result) {
            if (obj instanceof ReferenceRating) {
                ReferenceRating other = (ReferenceRating) obj;
                try {
                    Connection conn1 = getConnection();
                    Connection conn2 = other.getConnection();
                    if (conn2 == conn1) {
                        return true;
                    }
                    DatabaseMetaData md1 = conn1.getMetaData();
                    DatabaseMetaData md2 = conn2.getMetaData();
                    if (!md1.getURL().equalsIgnoreCase(md2.getURL())) {
                        return false;
                    }
                    if (!md1.getUserName().equalsIgnoreCase(md2.getUserName())) {
                        return false;
                    }
                    return true;
                } catch (Exception e) {
                    result = other.hashCode() == hashCode();
                }
            }
        }
        return result;
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNativeVerticalDatum()
     */
    @Override
    public String getNativeVerticalDatum() throws VerticalDatumException {
        checkVDC();
        return vdc.getNativeVerticalDatum();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getCurrentVerticalDatum()
     */
    @Override
    public String getCurrentVerticalDatum() throws VerticalDatumException {
        checkVDC();
        return vdc.getCurrentVerticalDatum();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#isCurrentVerticalDatumEstimated()
     */
    @Override
    public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
        checkVDC();
        return vdc.isCurrentVerticalDatumEstimated();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toNativeVerticalDatum()
     */
    @Override
    public boolean toNativeVerticalDatum() throws VerticalDatumException {
        checkVDC();
        return vdc.toNativeVerticalDatum();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toNGVD29()
     */
    @Override
    public boolean toNGVD29() throws VerticalDatumException {
        checkVDC();
        return vdc.toNGVD29();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toNAVD88()
     */
    @Override
    public boolean toNAVD88() throws VerticalDatumException {
        checkVDC();
        return vdc.toNAVD88();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toVerticalDatum(java.lang.String)
     */
    @Override
    public boolean toVerticalDatum(String datum) throws VerticalDatumException {
        checkVDC();
        return vdc.toVerticalDatum(datum);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#forceVerticalDatum(java.lang.String)
     */
    @Override
    public boolean forceVerticalDatum(String datum) throws VerticalDatumException {
        checkVDC();
        return vdc.forceVerticalDatum(datum);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getCurrentOffset()
     */
    @Override
    public double getCurrentOffset() throws VerticalDatumException {
        checkVDC();
        return vdc.getCurrentOffset();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getCurrentOffset(java.lang.String)
     */
    @Override
    public double getCurrentOffset(String unit) throws VerticalDatumException {
        checkVDC();
        return vdc.getCurrentOffset(unit);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNGVD29Offset()
     */
    @Override
    public double getNGVD29Offset() throws VerticalDatumException {
        checkVDC();
        return vdc.getNGVD29Offset();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNGVD29Offset(java.lang.String)
     */
    @Override
    public double getNGVD29Offset(String unit) throws VerticalDatumException {
        checkVDC();
        return vdc.getNGVD29Offset(unit);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNAVD88Offset()
     */
    @Override
    public double getNAVD88Offset() throws VerticalDatumException {
        checkVDC();
        return vdc.getNAVD88Offset();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNAVD88Offset(java.lang.String)
     */
    @Override
    public double getNAVD88Offset(String unit) throws VerticalDatumException {
        checkVDC();
        return vdc.getNAVD88Offset(unit);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#isNGVD29OffsetEstimated()
     */
    @Override
    public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
        checkVDC();
        return vdc.isNGVD29OffsetEstimated();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#isNAVD88OffsetEstimated()
     */
    @Override
    public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
        checkVDC();
        return vdc.isNAVD88OffsetEstimated();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getVerticalDatumInfo()
     */
    @Override
    public String getVerticalDatumInfo() throws VerticalDatumException {
        checkVDC();
        return vdc.getVerticalDatumInfo();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#setVerticalDatumInfo(java.lang.String)
     */
    @Override
    public void setVerticalDatumInfo(String initStr) throws VerticalDatumException {
        checkVDC();
        vdc.setVerticalDatumInfo(initStr);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getName()
     */
    @Override
    public String getName() {
        return ratingSpecId;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#setName(java.lang.String)
     */
    @Override
    public void setName(String name) throws RatingException {
        throw new RatingException("Cannot set name of a reference rating");
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getRatingParameters()
     */
    @Override
    public String[] getRatingParameters() {
        return parameters;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getRatingUnits()
     */
    @Override
    public String[] getRatingUnits() {
        return ratingUnits;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getDataUnits()
     */
    @Override
    public String[] getDataUnits() {
        return dataUnits == null ? ratingUnits : dataUnits;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#setDataUnits(java.lang.String[])
     */
    @Override
    public synchronized void setDataUnits(String[] units) throws RatingException {
        try {
            if (units == null) {
                dataUnits = null;
                return;
            }
            if (units.length != parameters.length) {
                throw new RatingException(String.format("Expected %d units, got %d instead.", parameters.length, units.length));
            }
            Connection conn = getConnection();
            try {
                for (int i = 0; i < units.length; ++i) {
                    try {
                        CWMS_UTIL_PACKAGE.call_CONVERT_UNITS(DSL.using(conn, SQLDialect.ORACLE).configuration(), 1.0, ratingUnits[i], units[i]);
                    } catch (RuntimeException e) {
                        if (e.getMessage().contains("Cannot convert")) {
                            throw new RatingException(String.format("Invalid unit \"%s\" specified for parameter \"%s\"", units[i], parameters[i]));
                        }
                    }
                }
            } finally {
                releaseConnection(conn);
            }
            dataUnits = Arrays.copyOf(units, units.length);
        } catch (RuntimeException e) {
            throw new RatingException(e);
        }
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getDefaultValueTime()
     */
    @Override
    public synchronized long getDefaultValueTime() {
        return defaultValueTime == UNDEFINED_TIME ? System.currentTimeMillis() : defaultValueTime;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#setDefaultValueTime(long)
     */
    @Override
    public synchronized void setDefaultValueTime(long defaultValueTime) {
        this.defaultValueTime = defaultValueTime;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#resetDefaultValuetime()
     */
    @Override
    public synchronized void resetDefaultValuetime() {
        defaultValueTime = UNDEFINED_TIME;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getRatingTime()
     */
    @Override
    public synchronized long getRatingTime() {
        return ratingTime;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#setRatingTime(long)
     */
    @Override
    public synchronized void setRatingTime(long ratingTime) {
        this.ratingTime = ratingTime;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#resetRatingTime()
     */
    @Override
    public synchronized void resetRatingTime() {
        ratingTime = System.currentTimeMillis() + 100 * 365 * 86400 * 1000;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getRatingExtents()
     */
    @Override
    public synchronized double[][] getRatingExtents() throws RatingException {
        return getRatingExtents(getRatingTime());
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getRatingExtents(long)
     */
    @Override
    public synchronized double[][] getRatingExtents(long ratingTime) throws RatingException {
        Connection conn = getConnection();
        try {
            String pNativeUnits = "T";
            Timestamp pRatingTime = new Timestamp(ratingTime);
            String pTimeZone = TimeZone.getTimeZone("UTC").getID();
            GET_RATING_EXTENTS ratingExtents = CWMS_RATING_PACKAGE.call_GET_RATING_EXTENTS(
                    DSL.using(conn).configuration(), ratingSpecId, pNativeUnits, pRatingTime,
                    pTimeZone, officeId);
            DOUBLE_TAB_TAB_T extent = ratingExtents.getP_VALUES();
            return conertToDoubleArr(extent);
        } catch (Exception e) {
            throw new RatingException(e);
        } finally {
            releaseConnection(conn);
        }
    }

    public double[][] conertToDoubleArr(DOUBLE_TAB_TAB_T extent)
    {
        double[][] retval = new double[extent.size()][];
        for(int i = 0; i < extent.size(); i++)
        {
            DOUBLE_TAB_T values = extent.get(i);
            double[] retvalValues = new double[values.size()];
            for(int j = 0; j < values.size(); j++)
            {
                retvalValues[j] = values.get(j);
            }
            retval[i] = retvalValues;
        }
        return retval;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getEffectiveDates()
     */
    @Override
    public synchronized long[] getEffectiveDates() {
        long[] dates = null;
        try {
            Connection conn = getConnection();
            try {
                if (ratingSpecCode == UNDEFINED_LONG) {
                    populateRatingSpecCode();
                }
                dates = DSL.using(conn, SQLDialect.ORACLE)
                           .select(DSL.field(CWMS_UTIL_PACKAGE.call_TO_MILLIS(AV_RATING.AV_RATING.EFFECTIVE_DATE)))
                           .from(AV_RATING.AV_RATING)
                           .where(AV_RATING.AV_RATING.ALIASED_ITEM.isNull())
                           .and(AV_RATING.AV_RATING.RATING_SPEC_CODE.equal(ratingSpecCode))
                           .orderBy(1)
                           .getResult()
                           .stream()
                           .map(Record1::component1)
                           .mapToLong(BigDecimal::longValue)
                           .toArray();
            } finally {
                try {
                    releaseConnection(conn);
                } catch (RatingException e) {
                    if (logger.isLoggable(Level.WARNING)) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        logger.log(Level.WARNING, sw.toString());
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isLoggable(Level.WARNING)) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                logger.log(Level.WARNING, sw.toString());
            }
        }
        return dates;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getCreateDates()
     */
    @Override
    public synchronized long[] getCreateDates() {
        long[] dates = null;
        try {
            Connection conn = getConnection();
            try {
                if (conn == null) {
                    throw new RatingException(
                        "Not currently connected to a database. Either use a method with a Connection parameter or call setConnection(Connection)");
                }

                dates = DSL.using(conn, SQLDialect.ORACLE)
                           .select(DSL.field(CWMS_UTIL_PACKAGE.call_TO_MILLIS(AV_RATING.AV_RATING.CREATE_DATE)))
                           .from(AV_RATING.AV_RATING)
                           .where(AV_RATING.AV_RATING.ALIASED_ITEM.isNull())
                           .and(AV_RATING.AV_RATING.RATING_SPEC_CODE.equal(ratingSpecCode))
                           .orderBy(1)
                           .getResult()
                           .stream()
                           .map(Record1::component1)
                           .mapToLong(BigDecimal::longValue)
                           .toArray();
            } finally {
                try {
                    releaseConnection(conn);
                } catch (RatingException e) {
                    if (logger.isLoggable(Level.WARNING)) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        logger.log(Level.WARNING, sw.toString());
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isLoggable(Level.WARNING)) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                logger.log(Level.WARNING, sw.toString());
            }
        }
        return dates;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(double)
     */
    @Override
    public double rate(double indVal) throws RatingException {
        return rate(getDefaultValueTime(), indVal);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rateOne(double[])
     */
    @Override
    public double rateOne(double... indVals) throws RatingException {
        long[] valTimes = new long[] {getDefaultValueTime()};
        double[][] _indVals = new double[indVals.length][];
        for (int i = 0; i < indVals.length; ++i) {
            _indVals[i] = new double[] {indVals[i]};
        }
        return rate(valTimes, _indVals)[0];
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rateOne2(double[])
     */
    @Override
    public double rateOne2(double[] indVals) throws RatingException {
        long[] valTimes = new long[] {getDefaultValueTime()};
        double[][] _indVals = new double[1][indVals.length];
        for (int i = 0; i < indVals.length; ++i) {
            _indVals[0][i] = indVals[i];
        }
        return rate(valTimes, _indVals)[0];
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(double[])
     */
    @Override
    public double[] rate(double[] indVals) throws RatingException {
        return rate(getDefaultValueTime(), indVals);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(double[][])
     */
    @Override
    public double[] rate(double[][] indVals) throws RatingException {
        return rate(getDefaultValueTime(), indVals);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(long, double)
     */
    @Override
    public double rate(long valTime, double indVal) throws RatingException {
        long[] valTimes = new long[] {valTime};
        double[][] indVals = new double[][] {{indVal}};
        return rate(valTimes, indVals)[0];
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rateOne(long, double[])
     */
    @Override
    public double rateOne(long valTime, double... indVals) throws RatingException {
        return rateOne2(valTime, indVals);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rateOne2(long, double[])
     */
    @Override
    public double rateOne2(long valTime, double[] indVals) throws RatingException {
        long[] valTimes = new long[] {valTime};
        double[][] _indVals = new double[1][indVals.length];
        for (int i = 0; i < indVals.length; ++i) {
            _indVals[0][i] = indVals[i];
        }
        return rate(valTimes, _indVals)[0];
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(long, double[])
     */
    @Override
    public double[] rate(long valTime, double[] indVals) throws RatingException {
        long[] valTimes = new long[indVals.length];
        Arrays.fill(valTimes, valTime);
        return rate(valTimes, indVals);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(long[], double[])
     */
    @Override
    public double[] rate(long[] valTimes, double[] indVals) throws RatingException {
        double[][] _indVals = new double[indVals.length][];
        for (int i = 0; i < indVals.length; ++i) {
            _indVals[i] = new double[] {indVals[i]};
        }
        return rate(valTimes, _indVals);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(long, double[][])
     */
    @Override
    public double[] rate(long valTime, double[][] indVals) throws RatingException {
        long[] valTimes = new long[indVals.length];
        Arrays.fill(valTimes, valTime);
        return rate(valTimes, indVals);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(long[], double[][])
     */
    @Override
    public double[] rate(long[] valTimes, double[][] indVals) throws RatingException {
        //------------------------------------//
        // rearrange values for database call //
        //------------------------------------//
        int dim1 = indVals.length;
        int dim2 = indVals[0].length;
        double[][] dbIndVals = new double[dim2][dim1];
        for (int i = 0; i < dim2; ++i) {
            for (int j = 0; j < dim1; ++j) {
                dbIndVals[i][j] = indVals[j][i];
            }
        }
        synchronized (this) {
            Connection conn = getConnection();
            try {
                DOUBLE_TAB_TAB_T pValues = convertDoubleDoubles(indVals);
                STR_TAB_T pUnits = convertStrings(dataUnits == null ? ratingUnits : dataUnits);
                String pRound = "F";
                DATE_TABLE_TYPE pValueTimes = convertDateVals(valTimes);
                Timestamp pRatingTime = new Timestamp(ratingTime);
                String pTimeZone = TimeZone.getTimeZone("UTC").getID();
                String pOfficeId = null;
                DOUBLE_TAB_T rated = CWMS_RATING_PACKAGE.call_RATE(DSL.using(conn).configuration(),
                        ratingSpecId,
                        pValues, pUnits, pRound, pValueTimes, pRatingTime, pTimeZone,
                        pOfficeId);
                return convertDoubleTabTValues(rated);
            } catch (RuntimeException e) {
                throw new RatingException(e);
            } finally {
                releaseConnection(conn);
            }
        }
    }

    private STR_TAB_T convertStrings(String[] units)
    {
        STR_TAB_T retval = new STR_TAB_T();
        Collections.addAll(retval, units);
        return retval;
    }

    private DOUBLE_TAB_TAB_T convertDoubleDoubles(double[][] indVals)
    {
        DOUBLE_TAB_TAB_T retval = new DOUBLE_TAB_TAB_T();
        for(double[] values : indVals)
        {
            DOUBLE_TAB_T doubleTabT = new DOUBLE_TAB_T();
            for(double value : values)
            {
                doubleTabT.add(value);
            }
            retval.add(doubleTabT);
        }
        return retval;
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rate(hec.io.TimeSeriesContainer)
     */
    @Override
    public TimeSeriesContainer rate(TimeSeriesContainer tsc) throws RatingException {
        TimeSeriesContainer[] tscs = {tsc};
        return rate(tscs);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rate(hec.io.TimeSeriesContainer[])
     */
    @Override
    public synchronized TimeSeriesContainer rate(TimeSeriesContainer[] tscs) throws RatingException {
        if (tsRater == null) {
            tsRater = new TimeSeriesRater(this);
        }
        return tsRater.rate(tscs);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(hec.hecmath.TimeSeriesMath)
     */
    @Override
    public TimeSeriesMath rate(TimeSeriesMath tsm) throws RatingException {
        try {
            return new TimeSeriesMath(rate((TimeSeriesContainer) tsm.getData()));
        } catch (Throwable t) {
            if (t instanceof RatingException) {
                throw (RatingException) t;
            }
            throw new RatingException(t);
        }
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(hec.hecmath.TimeSeriesMath[])
     */
    @Override
    public TimeSeriesMath rate(TimeSeriesMath[] tsms) throws RatingException {
        TimeSeriesContainer[] tscs = new TimeSeriesContainer[tsms.length];
        try {
            for (int i = 0; i < tsms.length; ++i) {
                tscs[i] = (TimeSeriesContainer) tsms[i].getData();
            }
            return new TimeSeriesMath(rate(tscs));
        } catch (Throwable t) {
            if (t instanceof RatingException) {
                throw (RatingException) t;
            }
            throw new RatingException(t);
        }
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#reverseRate(double)
     */
    @Override
    public double reverseRate(double depVal) throws RatingException {
        long[] valTimes = {getDefaultValueTime()};
        double[] depVals = {depVal};
        return reverseRate(valTimes, depVals)[0];
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#reverseRate(double[])
     */
    @Override
    public double[] reverseRate(double[] depVals) throws RatingException {
        long[] valTimes = new long[depVals.length];
        Arrays.fill(valTimes, getDefaultValueTime());
        return reverseRate(valTimes, depVals);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#reverseRate(long, double)
     */
    @Override
    public double reverseRate(long valTime, double depVal) throws RatingException {
        long[] valTimes = {valTime};
        double[] depVals = {depVal};
        return reverseRate(valTimes, depVals)[0];
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#reverseRate(long, double[])
     */
    @Override
    public double[] reverseRate(long valTime, double[] depVals) throws RatingException {
        long[] valTimes = new long[depVals.length];
        Arrays.fill(valTimes, valTime);
        return reverseRate(valTimes, depVals);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#reverseRate(long[], double[])
     */
    @Override
    public synchronized double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException {
        Connection conn = getConnection();
        try {
            DOUBLE_TAB_T pValues = convertDoubleVals(depVals);
            STR_TAB_T pUnits = new STR_TAB_T(dataUnits == null ? ratingUnits : dataUnits);
            String pRound = "F";
            DATE_TABLE_TYPE pValueTimes = convertDateVals(valTimes);
            Timestamp pRatingTime = new Timestamp(ratingTime);
            String pTimeZone = TimeZone.getTimeZone("UTC").getID();
            DOUBLE_TAB_T rated = CWMS_RATING_PACKAGE.call_REVERSE_RATE(DSL.using(conn).configuration(),
                    ratingSpecId, pValues, pUnits, pRound, pValueTimes, pRatingTime,
                    pTimeZone, officeId);
            return convertDoubleTabTValues(rated);
        } catch (RuntimeException e) {
            throw new RatingException(e);
        } finally {
            releaseConnection(conn);
        }
    }

    private DATE_TABLE_TYPE convertDateVals(long[] valTimes)
    {
        DATE_TABLE_TYPE dateTableType = new DATE_TABLE_TYPE();
        for(long val : valTimes)
        {
            dateTableType.add(new Timestamp(val));
        }
        return dateTableType;
    }

    private DOUBLE_TAB_T convertDoubleVals(double[] depVals)
    {
        DOUBLE_TAB_T doubleTabT = new DOUBLE_TAB_T();
        for(double val : depVals)
        {
            doubleTabT.add(val);
        }
        return doubleTabT;
    }

    private double[] convertDoubleTabTValues(DOUBLE_TAB_T values)
    {
        double[] retval = new double[values.size()];
        for(int i = 0; i < values.size();i++)
        {
            Double value = values.get(i);
			retval[i] = Objects.requireNonNullElse(value, NumericalConstants.HEC_UNDEFINED_DOUBLE);
        }
        return retval;
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#reverseRate(hec.io.TimeSeriesContainer)
     */
    @Override
    public synchronized TimeSeriesContainer reverseRate(TimeSeriesContainer tsc) throws RatingException {
        if (tsRater == null) {
            tsRater = new TimeSeriesRater(this);
        }
        return tsRater.reverseRate(tsc);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#reverseRate(hec.hecmath.TimeSeriesMath)
     */
    @Override
    public TimeSeriesMath reverseRate(TimeSeriesMath tsm) throws RatingException {
        try {
            return new TimeSeriesMath(reverseRate((TimeSeriesContainer) tsm.getData()));
        } catch (Throwable t) {
            if (t instanceof RatingException) {
                throw (RatingException) t;
            }
            throw new RatingException(t);
        }
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getIndParamCount()
     */
    @Override
    public synchronized int getIndParamCount() throws RatingException {
        return parameters.length - 1;
    }

    protected void checkVDC() throws VerticalDatumException {
        if (vdc == null) {
            throw new VerticalDatumException("Rating has no vertical datum information.");
        }
    }

    /**
     * Returns the VerticalDatumContainer
     *
     * @return
     */
    @Override
    public VerticalDatumContainer getVerticalDatumContainer() {
        return vdc;
    }

    /**
     * Sets the VerticalDatumContainer
     *
     * @param vdc
     */
    public void setVerticalDatumContainer(VerticalDatumContainer vdc) {
        this.vdc = vdc;
    }
}
