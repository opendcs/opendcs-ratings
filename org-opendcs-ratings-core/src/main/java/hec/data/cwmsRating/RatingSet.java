/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package org.opendcs.ratings;

import static hec.lang.Const.UNDEFINED_TIME;

import hec.data.DataSetException;

import hec.data.RoundingException;
import org.opendcs.ratings.io.RatingJdbcCompatUtil;
import org.opendcs.ratings.io.RatingSetContainer;
import org.opendcs.ratings.io.RatingSetStateContainer;
import org.opendcs.ratings.io.RatingSpecContainer;
import org.opendcs.ratings.io.RatingXmlCompatUtil;
import hec.data.rating.IRatingSpecification;
import hec.data.rating.IRatingTemplate;
import hec.hecmath.TextMath;
import hec.hecmath.TimeSeriesMath;
import hec.io.TextContainer;
import hec.io.TimeSeriesContainer;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Observer;
import java.util.TreeMap;
import java.util.logging.Logger;
import mil.army.usace.hec.metadata.VerticalDatum;
import mil.army.usace.hec.metadata.VerticalDatumContainer;
import mil.army.usace.hec.metadata.VerticalDatumException;

/**
 * Implements CWMS-style ratings (time series of ratings)
 *
 * @author Mike Perryman
 */
public class RatingSet implements IRating, Observer, VerticalDatum {

    protected static final Logger logger = Logger.getLogger(RatingSet.class.getPackage().getName());

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
        EAGER, LAZY, REFERENCE;

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
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param conn         The connection to a CWMS database
     * @param ratingSpecId The rating specification identifier
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabase(Connection conn, String ratingSpecId) throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(null, conn, null, ratingSpecId, null, null, false);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param conn         The connection to a CWMS database
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabase(Connection conn, String ratingSpecId, Long startTime, Long endTime) throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(null, conn, null, ratingSpecId, startTime, endTime, false);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabase(Connection conn, String officeId, String ratingSpecId) throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(null, conn, officeId, ratingSpecId, null, null, false);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param conn         The connection to a CWMS database
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabaseEffective(Connection conn, String ratingSpecId, Long startTime, Long endTime) throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(null, conn, null, ratingSpecId, startTime, endTime, true);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabase(Connection conn, String officeId, String ratingSpecId, Long startTime, Long endTime) throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(null, conn, officeId, ratingSpecId, startTime, endTime, false);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The time of the earliest data to rate, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The time of the latest data to rate, in milliseconds.  If null, no latest limit is set.
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabaseEffective(Connection conn, String officeId, String ratingSpecId, Long startTime, Long endTime)
        throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(null, conn, officeId, ratingSpecId, startTime, endTime, true);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no latest limit is set.
     * @param dataTimes    Determines how startTime and endTime are interpreted.
     *                         <table border="1">
     *                     <caption>Start/End Time values</caption>
     *                           <tr>
     *                             <th>Value</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>false</td>
     *                             <td>The time window specifies the extent of when the ratings became effective</td>
     *                             <td>true</td>
     *                             <td>Time time window specifies the time extent of data rate</td>
     *                           </tr>
     *                         </table>
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabase(Connection conn, String officeId, String ratingSpecId, Long startTime, Long endTime, boolean dataTimes)
        throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(null, conn, null, ratingSpecId, startTime, endTime, dataTimes);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Loading Method</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param ratingSpecId The rating specification identifier
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabase(DatabaseLoadMethod loadMethod, Connection conn, String ratingSpecId) throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(loadMethod, conn, null, ratingSpecId, null, null, false);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Loading Method</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabase(DatabaseLoadMethod loadMethod, Connection conn, String ratingSpecId, Long startTime, Long endTime)
        throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(loadMethod, conn, null, ratingSpecId, startTime, endTime, false);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Loading Method</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabase(DatabaseLoadMethod loadMethod, Connection conn, String officeId, String ratingSpecId)
        throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(loadMethod, conn, officeId, ratingSpecId, null, null, false);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Loading Method</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabaseEffective(DatabaseLoadMethod loadMethod, Connection conn, String ratingSpecId, Long startTime, Long endTime)
        throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(loadMethod, conn, null, ratingSpecId, startTime, endTime, false);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Loading Methods</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabase(DatabaseLoadMethod loadMethod, Connection conn, String officeId, String ratingSpecId, Long startTime,
                                         Long endTime) throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(loadMethod, conn, officeId, ratingSpecId, startTime, endTime, false);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Loading Methods</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The time of the earliest data to rate, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The time of the latest data to rate, in milliseconds.  If null, no latest limit is set.
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabaseEffective(DatabaseLoadMethod loadMethod, Connection conn, String officeId, String ratingSpecId,
                                                  Long startTime, Long endTime) throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(loadMethod, conn, officeId, ratingSpecId, startTime, endTime, true);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Loading Methods</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no latest limit is set.
     * @param dataTimes    Determines how startTime and endTime are interpreted.
     *                         <table border="1">
     *                     <caption>Start/End Time values</caption>
     *                           <tr>
     *                             <th>Value</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>false</td>
     *                             <td>The time window specifies the extent of when the ratings became effective</td>
     *                             <td>true</td>
     *                             <td>Time time window specifies the time extent of data rate</td>
     *                           </tr>
     *                         </table>
     * @return The new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static RatingSet fromDatabase(DatabaseLoadMethod loadMethod, Connection conn, String officeId, String ratingSpecId, Long startTime,
                                         Long endTime, boolean dataTimes) throws RatingException {
        return RatingJdbcCompatUtil.getInstance().fromDatabase(loadMethod, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
    }

    /**
     * Generates a new RatingSet object from an XML instance.
     *
     * @param xmlText The XML instance to construct the RatingSet object from. The document (root) node is expected to be
     *                &lt;ratings&gt;, which is expected to have one or more &lt;rating&gt; or &lt;usgs-stream-rating&gt; child nodes, all of the same
     *                rating specification.  Appropriate &lt;rating-template&gt; and &lt;rating-spec&gt; nodes are required for the rating set;
     *                any other template and specification nodes are ignored. The XML instance may be compressed (gzip+base64).
     * @return A new RatingSet object
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#ratingSet(String) instead
     */
    @Deprecated
    public static RatingSet fromXml(String xmlText) throws RatingException {
        return RatingXmlCompatUtil.getInstance().createRatingSet(xmlText);
    }

    /**
     * Returns the XML required to generate a new RatingSet object based on specified criteria
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Loading Methods</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no latest limit is set.
     * @param dataTimes    Determines how startTime and endTime are interpreted.
     *                         <table border="1">
     *                     <caption>Start/End Time values</caption>
     *                           <tr>
     *                             <th>Value</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>false</td>
     *                             <td>The time window specifies the extent of when the ratings became effective</td>
     *                             <td>true</td>
     *                             <td>Time time window specifies the time extent of data rate</td>
     *                           </tr>
     *                         </table>
     * @return The XML instance
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#getXmlFromDatabase(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public static String getXmlFromDatabase(DatabaseLoadMethod loadMethod, Connection conn, String officeId, String ratingSpecId, Long startTime,
                                            Long endTime, boolean dataTimes) throws RatingException {
        return RatingJdbcCompatUtil.getInstance().getXmlFromDatabase(loadMethod, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
    }

    /**
     * Public constructor from a CWMS database connection
     *
     * @param conn         The connection to a CWMS database
     * @param ratingSpecId The rating specification identifier
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public RatingSet(Connection conn, String ratingSpecId) throws RatingException {
        this.composedRatingSet = RatingJdbcCompatUtil.getInstance().fromDatabase(null, conn, null, ratingSpecId, null, null, false);
    }

    /**
     * Public constructor from a CWMS database connection
     *
     * @param conn         The connection to a CWMS database
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public RatingSet(Connection conn, String ratingSpecId, Long startTime, Long endTime) throws RatingException {
        this.composedRatingSet = RatingJdbcCompatUtil.getInstance().fromDatabase(null, conn, null, ratingSpecId, startTime, endTime, false);
    }

    /**
     * Public constructor from a CWMS database connection
     *
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public RatingSet(Connection conn, String officeId, String ratingSpecId) throws RatingException {
        this.composedRatingSet = RatingJdbcCompatUtil.getInstance().fromDatabase(null, conn, officeId, ratingSpecId, null, null, false);
    }

    /**
     * Public constructor from a CWMS database connection
     *
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public RatingSet(Connection conn, String officeId, String ratingSpecId, Long startTime, Long endTime) throws RatingException {
        this.composedRatingSet = RatingJdbcCompatUtil.getInstance().fromDatabase(null, conn, officeId, ratingSpecId, startTime, endTime, false);
    }

    /**
     * Generates a new RatingSet object from a CWMS database connection
     *
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no latest limit is set.
     * @param dataTimes    Determines how startTime and endTime are interpreted.
     *                         <table border="1">
     *                     <caption>Start/End Time values</caption>
     *                           <tr>
     *                             <th>Value</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>false</td>
     *                             <td>The time window specifies the extent of when the ratings became effective</td>
     *                             <td>true</td>
     *                             <td>Time time window specifies the time extent of data rate</td>
     *                           </tr>
     *                         </table>
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public RatingSet(Connection conn, String officeId, String ratingSpecId, Long startTime, Long endTime, boolean dataTimes) throws RatingException {
        this.composedRatingSet = RatingJdbcCompatUtil.getInstance().fromDatabase(null, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
    }

    /**
     * Public constructor from a CWMS database connection
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Loading Methods</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param ratingSpecId The rating specification identifier
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public RatingSet(DatabaseLoadMethod loadMethod, Connection conn, String ratingSpecId) throws RatingException {
        this.composedRatingSet = RatingJdbcCompatUtil.getInstance().fromDatabase(loadMethod, conn, null, ratingSpecId, null, null, false);

    }

    /**
     * Public constructor from a CWMS database connection
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Loading Methods</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public RatingSet(DatabaseLoadMethod loadMethod, Connection conn, String ratingSpecId, Long startTime, Long endTime) throws RatingException {
        this.composedRatingSet = RatingJdbcCompatUtil.getInstance().fromDatabase(loadMethod, conn, null, ratingSpecId, startTime, endTime, false);
    }

    /**
     * Public constructor from a CWMS database connection
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Loading Methods</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public RatingSet(DatabaseLoadMethod loadMethod, Connection conn, String officeId, String ratingSpecId) throws RatingException {
        this.composedRatingSet = RatingJdbcCompatUtil.getInstance().fromDatabase(loadMethod, conn, officeId, ratingSpecId, null, null, false);
    }

    /**
     * Public constructor from a CWMS database connection
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Loading Methods</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public RatingSet(DatabaseLoadMethod loadMethod, Connection conn, String officeId, String ratingSpecId, Long startTime, Long endTime)
        throws RatingException {
        this.composedRatingSet = RatingJdbcCompatUtil.getInstance().fromDatabase(loadMethod, conn, officeId, ratingSpecId, startTime, endTime, false);
    }

    /**
     * @param conn The database connection to use
     * @return whether this rating set has been updated in the database
     * @throws Exception
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public boolean isUpdated(Connection conn) throws Exception {
        return composedRatingSet.isUpdated(conn);
    }

    /**
     * Public Constructor - sets rating specification only
     *
     * @param officeId              The office that owns the rating specification
     * @param ratingSpecId          The rating specification identifier
     * @param sourceAgencyId        The identifier of the agency that maintains ratings using this specification
     * @param inRangeMethod         The prescribed behavior for when the time of the value to rate falls within the range of rating effective dates
     * @param outRangeLowMethod     The prescribed behavior for when the time of the value to rate falls before the earliest rating effective date
     * @param outRangeHighMethod    The prescribed behavior for when the time of the value to rate falls after the latest rating effective date
     * @param active                Specifies whether to utilize any ratings using this specification
     * @param autoUpdate            Specifies whether ratings using this specification should be automatically loaded when new ratings are available
     * @param autoActivate          Specifies whether ratings using this specification should be automatically activated when new ratings are available
     * @param autoMigrateExtensions Specifies whether existing should be automatically applied to ratings using this specification when new ratings are loaded
     * @param indRoundingSpecs      The USGS-style rounding specifications for each independent parameter
     * @param depRoundingSpec       The USGS-style rounding specifications for the dependent parameter
     * @param description           The description of this rating specification
     * @throws RatingException
     * @throws RoundingException
     * @deprecated Use {@link hec.data.cwmsRating.RatingSetFactory#ratingSet(RatingSpec)} instead
     */
    @Deprecated
    public RatingSet(String officeId, String ratingSpecId, String sourceAgencyId, String inRangeMethod, String outRangeLowMethod,
                     String outRangeHighMethod, boolean active, boolean autoUpdate, boolean autoActivate, boolean autoMigrateExtensions,
                     String[] indRoundingSpecs, String depRoundingSpec, String description) throws RatingException, RoundingException {
        this(new RatingSpec(officeId, ratingSpecId, sourceAgencyId, inRangeMethod, outRangeLowMethod, outRangeHighMethod, active, autoUpdate,
            autoActivate, autoMigrateExtensions, indRoundingSpecs, depRoundingSpec, description));
    }

    /**
     * Public Constructor from RatingSpecContainer
     *
     * @param rsc The RatingSpecContainer object to initialize from
     * @throws RatingException
     * @deprecated Use {@link hec.data.cwmsRating.RatingSetFactory#ratingSet(RatingSpec)} instead
     */
    @Deprecated
    public RatingSet(RatingSpecContainer rsc) throws RatingException {
        this(new RatingSpec(rsc));
    }

    /**
     * Public constructor a CWMS database connection
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Loading Methods</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no latest limit is set.
     * @param dataTimes    Determines how startTime and endTime are interpreted.
     *                         <table border="1">
     *                     <caption>Start/End Time values</caption>
     *                           <tr>
     *                             <th>Value</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>false</td>
     *                             <td>The time window specifies the extent of when the ratings became effective</td>
     *                             <td>true</td>
     *                             <td>Time time window specifies the time extent of data rate</td>
     *                           </tr>
     *                         </table>
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSet(DatabaseLoadMethod, Connection, String, String, Long, Long, boolean) instead
     */
    @Deprecated
    public RatingSet(DatabaseLoadMethod loadMethod, Connection conn, String officeId, String ratingSpecId, Long startTime, Long endTime,
                     boolean dataTimes) throws RatingException {
        this.composedRatingSet = RatingJdbcCompatUtil.getInstance().fromDatabase(loadMethod, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
    }

    /**
     * Public Constructor - sets rating specification only
     *
     * @param ratingSpec The rating specification
     * @throws RatingException
     * @deprecated Use {@link hec.data.cwmsRating.RatingSetFactory#ratingSet(RatingSpec)} instead
     */
    @Deprecated
    public RatingSet(RatingSpec ratingSpec) throws RatingException {
        this.composedRatingSet = RatingSetFactory.ratingSet(ratingSpec);
    }

    /**
     * Public Constructor - sets rating specification and single rating
     *
     * @param ratingSpec The rating specification
     * @param rating     The rating
     * @throws RatingException
     * @deprecated Use {@link hec.data.cwmsRating.RatingSetFactory#ratingSet(RatingSpec, AbstractRating)} instead
     */
    @Deprecated
    public RatingSet(RatingSpec ratingSpec, AbstractRating rating) throws RatingException {
        this.composedRatingSet = RatingSetFactory.ratingSet(ratingSpec, rating);
    }

    /**
     * Public Constructor - sets rating specification and a time series of ratings
     *
     * @param ratingSpec The rating specification
     * @param ratings    The time series of ratings
     * @throws RatingException
     * @deprecated Use {@link hec.data.cwmsRating.RatingSetFactory#ratingSet(RatingSpec, AbstractRating[])} instead
     */
    @Deprecated
    public RatingSet(RatingSpec ratingSpec, AbstractRating[] ratings) throws RatingException {
        this.composedRatingSet = RatingSetFactory.ratingSet(ratingSpec, ratings);
    }

    /**
     * Public Constructor - sets rating specification and a time series of ratings
     *
     * @param ratingSpec The rating specification
     * @param ratings    The time series of ratings
     * @throws RatingException
     * @deprecated Use {@link hec.data.cwmsRating.RatingSetFactory#ratingSet(RatingSpec, Iterable)} instead
     */
    @Deprecated
    public RatingSet(RatingSpec ratingSpec, Iterable<AbstractRating> ratings) throws RatingException {
        this.composedRatingSet = RatingSetFactory.ratingSet(ratingSpec, ratings);
    }

    /**
     * Public Constructor from an uncompressed XML instance
     *
     * @param xmlStr The XML instance to initialize from
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#(String) instead
     */
    @Deprecated
    public RatingSet(String xmlStr) throws RatingException {
        this(xmlStr, false);
    }

    /**
     * Public Constructor from an XML instance
     *
     * @param xmlStr       The XML instance to initialize from
     * @param isCompressed Flag specifying whether the string is a compressed XML string
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#ratingSet(String, boolean) instead
     */
    @Deprecated
    public RatingSet(String xmlStr, boolean isCompressed) throws RatingException {
        this.composedRatingSet = RatingXmlCompatUtil.getInstance().createRatingSet(xmlStr, isCompressed);
    }

    /**
     * Public Constructor from RatingSetContainer
     *
     * @param rsc The RatingSetContainer object to initialize from
     * @throws RatingException
     * @deprecated Use {@link hec.data.cwmsRating.RatingSetFactory#ratingSet(RatingSetContainer)} instead
     */
    @Deprecated
    public RatingSet(RatingSetContainer rsc) throws RatingException {
        this.composedRatingSet = RatingSetFactory.ratingSet(rsc);
    }

    /**
     * Public Constructor from TextContainer (as read from DSS)
     *
     * @param tc The TextContainer object to initialize from
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#ratingSet(TextContainer) instead
     */
    @Deprecated
    public RatingSet(TextContainer tc) throws RatingException {
        this.composedRatingSet = RatingXmlCompatUtil.getInstance().createRatingSet(tc);
    }

    /**
     * Public Constructor from TextMath (as read from DSS)
     *
     * @param tm The TextMath object to initialize from
     * @throws RatingException
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#ratingSet(TextMath) instead
     */
    @Deprecated
    public RatingSet(TextMath tm) throws RatingException {
        this.composedRatingSet = RatingXmlCompatUtil.getInstance().createRatingSet(tm);
    }

    public static Logger getLogger() {
        return logger;
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
     * @throws RatingException
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
     * @throws RatingException
     */
    public void replaceRating(AbstractRating rating) throws RatingException {
        this.composedRatingSet.replaceRating(rating);
    }

    /**
     * Replaces multiple ratings in the existing ratings.
     *
     * @param ratings The ratings to replace existing ones
     * @throws RatingException
     */
    public void replaceRatings(AbstractRating[] ratings) throws RatingException {
        this.composedRatingSet.replaceRatings(ratings);
    }

    /**
     * Replaces multiple ratings in the existing ratings.
     *
     * @param ratings The ratings to replace existing ones
     * @throws RatingException
     */
    public void replaceRatings(Iterable<AbstractRating> ratings) throws RatingException {
        this.composedRatingSet.replaceRatings(ratings);
    }

    /**
     *
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public void getConcreteRatings(long date) throws RatingException {
        this.composedRatingSet.getConcreteRatings(date);
    }

    /**
     * Loads all rating values from table ratings that haven't already been loaded.
     *
     * @throws RatingException
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public void getConcreteRatings(Connection conn) throws RatingException {
        this.composedRatingSet.getConcreteRatings(conn);
    }

    /**
     * Loads all rating values from table ratings that haven't already been loaded.
     *
     * @throws RatingException
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public void getConcreteRatings() throws RatingException {
        this.composedRatingSet.getConcreteRatings();
    }

    /**
     * Retrieves a rated value for a specified single input value and time. The rating set must
     * be for a single independent parameter
     *
     * @param value     The value to rate
     * @param valueTime The time associated with the value, in Java milliseconds
     * @return the rated value
     * @throws RatingException
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
     * @throws RatingException
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
     * @throws RatingException
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
     * @throws RatingException
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
     * @throws RatingException
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
     * @throws RatingException
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
     * @throws RatingException
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
     * @throws RatingException
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
     * @throws RatingException
     */
    public TimeSeriesContainer rate(TimeSeriesContainer[] tscs, String ratedUnitStr) throws RatingException {
        return this.composedRatingSet.rate(tscs, ratedUnitStr);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(hec.hecmath.TimeSeriesMath)
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
     * @throws RatingException
     */
    public TimeSeriesMath rate(TimeSeriesMath tsm, String ratedUnitStr) throws RatingException {
        return this.composedRatingSet.rate(tsm, ratedUnitStr);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(hec.hecmath.TimeSeriesMath[])
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
     * @throws RatingException
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
     * Returns the unique identifying parts for the rating specification.
     *
     * @return
     * @throws DataSetException
     */
    public IRatingSpecification getRatingSpecification() throws DataSetException {
        return this.composedRatingSet.getRatingSpecification();
    }

    /**
     * Returns the unique identifying parts for the rating template.
     *
     * @return
     * @throws DataSetException
     */
    public IRatingTemplate getRatingTemplate() throws DataSetException {
        return this.composedRatingSet.getRatingTemplate();
    }

    /**
     * Sets the rating specification.
     *
     * @param ratingSpec The rating specification
     * @throws RatingException
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
     * @throws RatingException
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
     * Retrieves the default value time. This is used for rating values that have no inherent times.
     *
     * @return The default value time
     * @deprecated Reference {@link RatingSet#getDefaultValueTime()} instead
     */
    @Deprecated
    public long getDefaultValuetime() {
        return this.composedRatingSet.getDefaultValuetime();
    }

    /**
     * Resets the default value time. This is used for rating values that have no inherent times.
     */
    @Override
    public void resetDefaultValuetime() {
        this.composedRatingSet.resetDefaultValuetime();
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
     * @see hec.data.IRating#setName(java.lang.String)
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
     * @see hec.data.IRating#getRatingExtents()
     */
    @Override
    public double[][] getRatingExtents() throws RatingException {
        return this.composedRatingSet.getRatingExtents();
    }

    /**
     * Retrieves the rating extents for a specified time
     *
     * @param ratingTime The time for which to retrieve the rating extents
     * @param conn       The database connection to use if the rating was lazily loaded
     * @return The rating extents
     * @throws RatingException any errors calcualting the value
     * @see hec.data.IRating#getRatingExtents(long)
     */
    public double[][] getRatingExtents(long ratingTime, Connection conn) throws RatingException {
        return this.composedRatingSet.getRatingExtents(ratingTime, conn);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getRatingExtents(long)
     */
    @Override
    public double[][] getRatingExtents(long ratingTime) throws RatingException {
        return this.composedRatingSet.getRatingExtents(ratingTime);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getEffectiveDates()
     */
    @Override
    public long[] getEffectiveDates() {
        return this.composedRatingSet.getEffectiveDates();
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getCreateDates()
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
    public double rateOne2(long valTime, double... indVals) throws RatingException {
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
     * @see hec.data.IRating#reverseRate(hec.hecmath.TimeSeriesMath)
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
     * Retrieves the data ratingUnits. These are the ratingUnits expected for independent parameters and the unit produced
     * for the dependent parameter.  If the underlying rating uses different ratingUnits, the rating must perform unit
     * conversions.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @return The ratingUnits identifier, one unit for each parameter
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public String[] getDataUnits(Connection conn) {
        return this.composedRatingSet.getDataUnits(conn);
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
    public void setDataUnits(Connection conn, String[] units) throws RatingException {
        this.composedRatingSet.setDataUnits(conn, units);
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
    public double[][] getRatingExtents(Connection conn) throws RatingException {
        return this.composedRatingSet.getRatingExtents(conn);
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
    public double[][] getRatingExtents(Connection conn, long ratingTime) throws RatingException {
        return this.composedRatingSet.getRatingExtents(conn, ratingTime);
    }

    /**
     * Retrieves the effective dates of the rating in milliseconds, one for each contained rating
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @return simple array of longs
     */
    public long[] getEffectiveDates(Connection conn) {
        return this.composedRatingSet.getEffectiveDates(conn);
    }

    /**
     * Retrieves the creation dates of the rating in milliseconds, one for each contained rating
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @return simple array of start dates
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public long[] getCreateDates(Connection conn) {
        return this.composedRatingSet.getCreateDates(conn);
    }

    /**
     * Finds the dependent value for a single independent value.  The rating must be for a single independent parameter.
     *
     * @param conn   The database connection to use for lazy ratings and reference ratings
     * @param indVal The independent value to rate.
     * @return The dependent value
     * @throws RatingException any errors calcualting the value
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public double rate(Connection conn, double indVal) throws RatingException {
        return this.composedRatingSet.rate(conn, indVal);
    }

    /**
     * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param indVals The independent parameters to rate
     * @return The dependent value
     * @throws RatingException any errors retrieving or calculating a value.
     */
    public double rateOne(Connection conn, double... indVals) throws RatingException {
        return this.composedRatingSet.rateOne(conn, indVals);
    }

    /**
     * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param indVals The independent parameters to rate
     * @return The dependent value
     * @throws RatingException any issues calculating the value
     */
    public double rateOne2(Connection conn, double[] indVals) throws RatingException {
        return this.composedRatingSet.rateOne2(conn, indVals);
    }

    /**
     * Finds multiple dependent values for multiple single independent values.  The rating must be for a single independent parameter.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param indVals The independent values to rate
     * @return The dependent values
     * @throws RatingException any issues calculating the value
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public double[] rate(Connection conn, double[] indVals) throws RatingException {
        return this.composedRatingSet.rate(conn, indVals);
    }

    /**
     * Finds multiple dependent values for multiple sets of independent values.  The rating must be for as many independent
     * parameters as the length of each independent parameter set.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param indVals The independent values to rate. Each set of independent values must be the same length.
     * @return The dependent values
     * @throws RatingException any issues calculating the values
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public double[] rate(Connection conn, double[][] indVals) throws RatingException {
        return this.composedRatingSet.rate(conn, indVals);
    }

    /**
     * Finds the dependent value for a single independent value at a specified time.  The rating must be for a single independent parameter.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param valTime The time associated with the value to rate, in Java milliseconds
     * @param indVal  The independent value to rate
     * @return The dependent value
     * @throws RatingException any errors calcualting the value
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public double rate(Connection conn, long valTime, double indVal) throws RatingException {
        return this.composedRatingSet.rate(conn, valTime, indVal);
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
    public double rateOne(Connection conn, long valTime, double... indVals) throws RatingException {
        return this.composedRatingSet.rateOne(conn, valTime, indVals);
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
    public double rateOne2(Connection conn, long valTime, double[] indVals) throws RatingException {
        return this.composedRatingSet.rateOne2(conn, valTime, indVals);
    }

    /**
     * Finds multiple dependent values for multiple single independent values at a specified time.  The rating must be for a single independent parameter.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param valTime The time associated with the values to rate, in Java milliseconds
     * @param indVals The independent values to rate
     * @return The dependent values
     * @throws RatingException any errors calculating the values
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public double[] rate(Connection conn, long valTime, double[] indVals) throws RatingException {
        return this.composedRatingSet.rate(conn, valTime, indVals);
    }

    /**
     * Finds multiple dependent values for multiple single independent and times.  The rating must be for a single independent parameter.
     *
     * @param conn     The database connection to use for lazy ratings and reference ratings
     * @param valTimes The times associated with the values to rate, in Java milliseconds
     * @param indVals  The independent values to rate
     * @return The dependent values
     * @throws RatingException any errors calculating the values
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public double[] rate(Connection conn, long[] valTimes, double[] indVals) throws RatingException {
        return this.composedRatingSet.rate(conn, valTimes, indVals);
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
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public double[] rate(Connection conn, long valTime, double[][] indVals) throws RatingException {
        return this.composedRatingSet.rate(conn, valTime, indVals);
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
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public double[] rate(Connection conn, long[] valTimes, double[][] indVals) throws RatingException {
        return this.composedRatingSet.rate(conn, valTimes, indVals);
    }

    /**
     * Rates the values in the specified TimeSeriesContainer to generate a resulting TimeSeriesContainer. The rating must be for a single independent parameter.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @param tsc  The TimeSeriesContainer of independent values.
     * @return The TimeSeriesContainer of dependent values.
     * @throws RatingException any errors calculating the values
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public TimeSeriesContainer rate(Connection conn, TimeSeriesContainer tsc) throws RatingException {
        return this.composedRatingSet.rate(conn, tsc);
    }

    /**
     * Rates the values in the specified TimeSeriesContainer objects to generate a resulting TimeSeriesContainer. The rating must be for as many independent parameters as the length of tscs.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @param tscs The TimeSeriesContainers of independent values, one for each independent parameter.
     * @return The TimeSeriesContainer of dependent values.
     * @throws RatingException any errors calculating the values
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public TimeSeriesContainer rate(Connection conn, TimeSeriesContainer[] tscs) throws RatingException {
        return this.composedRatingSet.rate(conn, tscs);
    }

    /**
     * Rates the values in the specified TimeSeriesMath to generate a resulting TimeSeriesMath. The rating must be for a single independent parameter.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @param tsm  The TimeSeriesMath of independent values.
     * @return The TimeSeriesMath of dependent values.
     * @throws RatingException any errors calculating the values
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public TimeSeriesMath rate(Connection conn, TimeSeriesMath tsm) throws RatingException {
        return this.composedRatingSet.rate(conn, tsm);
    }

    /**
     * Rates the values in the specified TimeSeriesMath objects to generate a resulting TimeSeriesMath. The rating must be for as many independent parameters as the length of tscs.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @param tsms The TimeSeriesMaths of independent values, one for each independent parameter.
     * @return The TimeSeriesMath of dependent values.
     * @throws RatingException any errors calculating the value
     * @deprecated Reference mil.army.usace.hec.cwms.rating.io.jdbc.JdbcRatingSet instead
     */
    @Deprecated
    public TimeSeriesMath rate(Connection conn, TimeSeriesMath[] tsms) throws RatingException {
        return this.composedRatingSet.rate(conn, tsms);
    }

    /**
     * Finds the independent value for a single independent value.  The rating must be for a single independent parameter.
     *
     * @param conn   The database connection to use for lazy ratings and reference ratings
     * @param depVal The dependent value to rate.
     * @return The independent value
     * @throws RatingException any errors calculating the value
     */
    public double reverseRate(Connection conn, double depVal) throws RatingException {
        return this.composedRatingSet.reverseRate(conn, depVal);
    }

    /**
     * Finds multiple independent values for multiple single independent values.  The rating must be for a single independent parameter.
     *
     * @param conn    The database connection to use for lazy ratings and reference ratings
     * @param depVals The dependent values to rate
     * @return The independent values
     * @throws RatingException any errors calculating the value
     */
    public double[] reverseRate(Connection conn, double[] depVals) throws RatingException {
        return this.composedRatingSet.reverseRate(conn, depVals);
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
    public double reverseRate(Connection conn, long valTime, double depVal) throws RatingException {
        return this.composedRatingSet.reverseRate(conn, valTime, depVal);
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
    public double[] reverseRate(Connection conn, long valTime, double[] depVals) throws RatingException {
        return this.composedRatingSet.reverseRate(conn, valTime, depVals);
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
    public double[] reverseRate(Connection conn, long[] valTimes, double[] depVals) throws RatingException {
        return this.composedRatingSet.reverseRate(conn, valTimes, depVals);
    }

    /**
     * Rates the values in the specified TimeSeriesContainer to generate a resulting TimeSeriesContainer. The rating must be for a single independent parameter.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @param tsc  The TimeSeriesContainer of dependent values.
     * @return The TimeSeriesContainer of independent values.
     * @throws RatingException any errors calculating the value
     */
    public TimeSeriesContainer reverseRate(Connection conn, TimeSeriesContainer tsc) throws RatingException {
        return this.composedRatingSet.reverseRate(conn, tsc);
    }

    /**
     * Rates the values in the specified TimeSeriesMath to generate a resulting TimeSeriesMath. The rating must be for a single independent parameter.
     *
     * @param conn The database connection to use for lazy ratings and reference ratings
     * @param tsm  The TimeSeriesMath of dependent values.
     * @return The TimeSeriesMath of independent values.
     * @throws RatingException any errors calculating the value
     */
    public TimeSeriesMath reverseRate(Connection conn, TimeSeriesMath tsm) throws RatingException {
        return this.composedRatingSet.reverseRate(conn, tsm);
    }

    /**
     * Outputs the rating set as an XML instance
     *
     * @param indent the text use for indentation
     * @return the XML text
     * @throws RatingException any errors rendering to XML
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#toXml instead
     */
    @Deprecated
    public String toXmlString(CharSequence indent) throws RatingException {
        return RatingXmlCompatUtil.getInstance().toXml(this, indent);
    }

    /**
     * Outputs the rating set in a compress XML instance suitable for storing in DSS
     *
     * @return The compressed XML text
     * @throws RatingException any error shrinking the data
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#toCompressedXml instead
     */
    @Deprecated
    public String toCompressedXmlString() throws RatingException {
        return RatingXmlCompatUtil.getInstance().toCompressedXml(this);
    }

    /**
     * Stores the rating set to a CWMS database
     *
     * @param conn              The connection to the CWMS database
     * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
     * @throws RatingException any issues processing the table into the database.
     */
    @Deprecated
    public void storeToDatabase(Connection conn, boolean overwriteExisting) throws RatingException {
        this.composedRatingSet.storeToDatabase(conn, overwriteExisting);
    }

    /**
     * Stores the rating set to a CWMS database
     *
     * @param conn              The connection to the CWMS database
     * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
     * @param includeTemplate   Flag specifying whether to include the rating template in the XML
     * @throws RatingException any errors storing to the database
     */
    @Deprecated
    public void storeToDatabase(Connection conn, boolean overwriteExisting, boolean includeTemplate) throws RatingException {
        this.composedRatingSet.storeToDatabase(conn, overwriteExisting, includeTemplate);
    }

    /* (non-Javadoc)
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(java.util.Observable arg0, Object arg1) {
        this.composedRatingSet.update(arg0, arg1);
    }

    /**
     * Adds an Observer to this RatingSet. The Observer will be notified of any changes to this RatingSet
     *
     * @param o The Observer object to add
     * @see java.util.Observer
     */
    public void addObserver(Observer o) {
        this.composedRatingSet.addObserver(o);
    }

    /**
     * Deletes an Observer from this RatingSet. The Observer will no longer be notified of any changes to this RatingSet
     *
     * @param o The Observer object to delete
     * @see java.util.Observer
     */
    public void deleteObserver(Observer o) {
        this.composedRatingSet.deleteObserver(o);
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
     * Sets the state of this object from a container
     *
     * @param rssc the state container
     * @throws RatingException any errors transferring data
     */
    @Deprecated
    public void setState(RatingSetStateContainer rssc) throws RatingException {
        this.composedRatingSet.setState(rssc);
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
     * Sets the data from this object from an XML instance
     *
     * @param xmlText The RatingSetContainer xml string with the data
     * @throws RatingException any errors processing the xml data
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#ratingSet(String) instead
     */
    @Deprecated
    public void setData(String xmlText) throws RatingException {
        RatingSet ratingSet = RatingXmlCompatUtil.getInstance().createRatingSet(xmlText);
        setData(ratingSet.getData());
    }

    /**
     * Sets the data from this object from a CWMS database connection
     *
     * @param loadMethod   The method used to load the object from the database. If null, the value of the property
     *                     "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                     are null (or if an invalid value is specified) the Lazy method will be used.
     *                         <table border="1">
     *                     <caption>Load Methods</caption>
     *                           <tr>
     *                             <th>Value (case insensitive)</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>Eager</td>
     *                             <td>Ratings for all effective times are loaded initially</td>
     *                             <td>Lazy</td>
     *                             <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                             <td>Reference</td>
     *                             <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                           </tr>
     *                         </table>
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @param startTime    The earliest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no earliest limit is set.
     * @param endTime      The latest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no latest limit is set.
     * @param dataTimes    Determines how startTime and endTime are interpreted.
     *                         <table border="1">
     *                     <caption>Start/End Time</caption>
     *                           <tr>
     *                             <th>Value</th>
     *                             <th>Interpretation</th>
     *                           </tr>
     *                           <tr>
     *                             <td>false</td>
     *                             <td>The time window specifies the extent of when the ratings became effective</td>
     *                             <td>true</td>
     *                             <td>Time time window specifies the time extent of data rate</td>
     *                           </tr>
     *                         </table>
     * @throws RatingException any errors with data retrieval or processing
     */
    @Deprecated
    public void setData(DatabaseLoadMethod loadMethod, Connection conn, String officeId, String ratingSpecId, Long startTime, Long endTime,
                        boolean dataTimes) throws RatingException {
        this.composedRatingSet = RatingJdbcCompatUtil.getInstance().fromDatabase(loadMethod, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
    }

    /**
     * Sets the database connection for this RatingSet and any constituent RatingSet objects
     *
     * @param conn the connection
     */
    @Deprecated
    public synchronized void setDatabaseConnection(Connection conn) {
        this.composedRatingSet.setDatabaseConnection(conn);
    }

    /**
     * Clears the database connection for this RatingSet and any constituent RatingSet objects
     */
    @Deprecated
    public void clearDatabaseConnection() {
        this.composedRatingSet.clearDatabaseConnection();
    }

    /**
     * Retrieves a TextContainer containing the data of this object, suitable for storing to DSS.
     *
     * @return The TextContainer
     * @throws RatingException any errors reading from dss or processing the data
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#getDssData() instead
     */
    @Deprecated
    public TextContainer getDssData() throws RatingException {
        return RatingXmlCompatUtil.getInstance().getDssData(this.composedRatingSet);
    }

    /**
     * Sets the data from this object from a TextContainer (as read from DSS)
     *
     * @param tc The TextContainer with the data
     * @throws RatingException any errors processing the data.
     * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#ratingSet(TextContainer) instead
     */
    @Deprecated
    public void setData(TextContainer tc) throws RatingException {
        this.composedRatingSet = RatingXmlCompatUtil.getInstance().createRatingSet(tc);
    }

    /**
     * Returns whether this object has any vertical datum info
     *
     * @return whether this object has any vertical datum info
     */
    public boolean hasVerticalDatum() {
        return this.composedRatingSet.hasVerticalDatum();
    }

    public String getNativeVerticalDatum(Connection conn) throws VerticalDatumException {
        return this.composedRatingSet.getNativeVerticalDatum(conn);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNativeVerticalDatum()
     */
    @Override
    public String getNativeVerticalDatum() throws VerticalDatumException {
        return this.composedRatingSet.getNativeVerticalDatum();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getCurrentVerticalDatum()
     */
    @Override
    public String getCurrentVerticalDatum() throws VerticalDatumException {
        return this.composedRatingSet.getCurrentVerticalDatum();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#isCurrentVerticalDatumEstimated()
     */
    @Override
    public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
        return this.composedRatingSet.isCurrentVerticalDatumEstimated();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toNativeVerticalDatum()
     */
    @Override
    public boolean toNativeVerticalDatum() throws VerticalDatumException {
        return this.composedRatingSet.toNativeVerticalDatum();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toNGVD29()
     */
    @Override
    public boolean toNGVD29() throws VerticalDatumException {
        return this.composedRatingSet.toNGVD29();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toNAVD88()
     */
    @Override
    public boolean toNAVD88() throws VerticalDatumException {
        return this.composedRatingSet.toNAVD88();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toVerticalDatum(java.lang.String)
     */
    @Override
    public boolean toVerticalDatum(String datum) throws VerticalDatumException {
        return this.composedRatingSet.toVerticalDatum(datum);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#forceVerticalDatum(java.lang.String)
     */
    @Override
    public boolean forceVerticalDatum(String datum) throws VerticalDatumException {
        return this.composedRatingSet.forceVerticalDatum(datum);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getCurrentOffset()
     */
    @Override
    public double getCurrentOffset() throws VerticalDatumException {
        return this.composedRatingSet.getCurrentOffset();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getCurrentOffset(java.lang.String)
     */
    @Override
    public double getCurrentOffset(String unit) throws VerticalDatumException {
        return this.composedRatingSet.getCurrentOffset(unit);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNGVD29Offset()
     */
    @Override
    public double getNGVD29Offset() throws VerticalDatumException {
        return this.composedRatingSet.getNGVD29Offset();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNGVD29Offset(java.lang.String)
     */
    @Override
    public double getNGVD29Offset(String unit) throws VerticalDatumException {
        return this.composedRatingSet.getNGVD29Offset(unit);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNAVD88Offset()
     */
    @Override
    public double getNAVD88Offset() throws VerticalDatumException {
        return this.composedRatingSet.getNAVD88Offset();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNAVD88Offset(java.lang.String)
     */
    @Override
    public double getNAVD88Offset(String unit) throws VerticalDatumException {
        return this.composedRatingSet.getNAVD88Offset(unit);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#isNGVD29OffsetEstimated()
     */
    @Override
    public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
        return this.composedRatingSet.isNGVD29OffsetEstimated();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#isNAVD88OffsetEstimated()
     */
    @Override
    public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
        return this.composedRatingSet.isNAVD88OffsetEstimated();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getVerticalDatumInfo()
     */
    @Override
    public String getVerticalDatumInfo() throws VerticalDatumException {
        return this.composedRatingSet.getVerticalDatumInfo();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#setVerticalDatumInfo(java.lang.String)
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
        return this.composedRatingSet.equals(obj);
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
     * @param rating
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