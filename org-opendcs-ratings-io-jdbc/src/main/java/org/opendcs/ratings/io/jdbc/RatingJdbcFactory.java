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

import static org.opendcs.ratings.RatingConst.SEPARATOR1;


import org.opendcs.ratings.AbstractRating;
import org.opendcs.ratings.AbstractRatingSet;
import org.opendcs.ratings.RatingSet;
import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.RatingSpec;
import org.opendcs.ratings.RatingTemplate;
import org.opendcs.ratings.io.RatingSetContainer;
import org.opendcs.ratings.io.ReferenceRatingContainer;
import hec.util.TextUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import org.opendcs.ratings.io.xml.RatingContainerXmlFactory;
import org.opendcs.ratings.io.xml.RatingSetContainerXmlFactory;
import org.opendcs.ratings.io.xml.RatingSpecXmlFactory;
import org.opendcs.ratings.io.xml.RatingXmlFactory;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import usace.cwms.db.jooq.codegen.packages.CWMS_RATING_PACKAGE;
import usace.cwms.db.jooq.codegen.packages.CWMS_UTIL_PACKAGE;

public final class RatingJdbcFactory {

    private RatingJdbcFactory() {
        throw new AssertionError("Utility class");
    }

    static RatingSet.DatabaseLoadMethod getDatabaseLoadMethod(RatingSet.DatabaseLoadMethod loadMethod) {
        String specifiedLoadMethod =
            (loadMethod == null ? System.getProperty("hec.data.cwmsRating.RatingSet.databaseLoadMethod", "lazy") : loadMethod.name()).toUpperCase();
        RatingSet.DatabaseLoadMethod databaseLoadMethod;
        try {
            databaseLoadMethod = RatingSet.DatabaseLoadMethod.valueOf(specifiedLoadMethod);
        } catch (RuntimeException ex) {
            RatingSet.getLogger().log(Level.WARNING,
                "Invalid value for property hec.data.cwmsRating.RatingSet.databaseLoadMethod: " + specifiedLoadMethod +
                    "\nMust be one of \"Eager\", \"Lazy\", or \"Reference\".\nUsing \"Lazy\"");
            databaseLoadMethod = RatingSet.DatabaseLoadMethod.LAZY;
        }
        return databaseLoadMethod;
    }

    /**
     * Generates a new AbstractRating object from a CWMS database connection
     *
     * @param conn          The connection to a CWMS database
     * @param officeId      The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId  The rating specification identifier
     * @param effectiveDate Specifies (in milliseconds) a time to be an upper bound on the effective date.
     *                      The rating with the latest effective date on or before this time is retrieved. If null, the latest rating is retrieved.
     * @return The new AbstractRating object
     * @throws RatingException any issues retrieving the data or processing what's returned
     */
    public static AbstractRating retrieveRating(Connection conn, String officeId, String ratingSpecId, Long effectiveDate) throws RatingException {
        try {
            Configuration configuration = getConfiguration(conn);
            Timestamp timestamp = null;
            if (effectiveDate != null) {
                timestamp = new Timestamp(effectiveDate);
            }
            String ratingsXml = CWMS_RATING_PACKAGE.call_RETRIEVE_RATINGS_XML2(configuration, ratingSpecId, null, timestamp, "UTC", officeId);
            return RatingXmlFactory.abstractRating(ratingsXml);
        } catch (RuntimeException ex) {
            throw new RatingException(ex);
        }
    }

    private static Configuration getConfiguration(Connection conn) {
        DSLContext dsl = DSL.using(conn);
        return dsl.configuration();
    }

    /**
     * Stores the rating  to a CWMS database
     *
     * @param abstractRating    data object to store
     * @param conn              The connection to the CWMS database
     * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
     * @throws RatingException on error
     */
    public static void store(AbstractRating abstractRating, Connection conn, boolean overwriteExisting) throws RatingException {
        String ratingXml = RatingContainerXmlFactory.toXml(abstractRating.getData(), "", 0);
        storeXml(conn, ratingXml, overwriteExisting, true);
    }

    /**
     * Stores the rating set to a CWMS database
     *
     * @param conn              The connection to the CWMS database
     * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
     * @param replaceBase       Flag specifying whether to replace the base curves of USGS-style stream ratings (false = only store shifts)
     * @throws RatingException on error
     */
    private static void storeXml(Connection conn, String xml, boolean overwriteExisting, boolean replaceBase) throws RatingException {
        try {
            Configuration configuration = getConfiguration(conn);
            String errors = CWMS_RATING_PACKAGE.call_STORE_RATINGS_XML__5(configuration, xml, overwriteExisting ? "F" : "T", replaceBase ? "T" : "F");
            if (errors != null && !errors.isEmpty()) {
                throw new RatingException(errors);
            }
        } catch (RuntimeException t) {
            throw new RatingException(t);
        }
    }

    /**
     * Stores the rating set to a CWMS database
     *
     * @param ratingSet         data object to store
     * @param conn              The connection to the CWMS database
     * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
     * @param includeTemplate   Flag specifying whether to include the rating template in the XML
     * @throws RatingException any errors storing to the database
     */
    public static void store(RatingSet ratingSet, Connection conn, boolean overwriteExisting, boolean includeTemplate) throws RatingException {
        //Can't store reference ratings. Silently succeeding for backwards compatibility.
        //Should probably consider throwing an exception in the future
        if (!(ratingSet instanceof ReferenceJdbcRatingSet)) {
            String ratingXml = RatingContainerXmlFactory.toXml(ratingSet.getData(), "", 0, includeTemplate, false);
            storeXml(conn, ratingXml, overwriteExisting, includeTemplate);
        }
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
     * @throws RatingException any issues retrieving the data or processing what's returned
     */
    public static String getXmlFromDatabase(RatingSet.DatabaseLoadMethod loadMethod, ConnectionProvider conn, String officeId, String ratingSpecId,
                                            Long startTime, Long endTime, boolean dataTimes) throws RatingException {
        try {

            RatingSet.DatabaseLoadMethod databaseLoadMethod = getDatabaseLoadMethod(loadMethod);
            String xmlText = null;
            Timestamp startDate = null;
            if (startTime != null) {
                startDate = new Timestamp(startTime);
            }
            Timestamp endDate = null;
            if (endTime != null) {
                endDate = new Timestamp(endTime);
            }
            Connection connection = conn.getConnection();
            try {
                switch (databaseLoadMethod) {
                    case EAGER:
                        xmlText = retrieveRatingSetXml(connection, officeId, ratingSpecId, startDate, endDate, dataTimes);
                        break;
                    case LAZY:
                        xmlText = retrieveLazyRatingSetXml(connection, officeId, ratingSpecId, startDate, endDate, dataTimes);
                        break;
                    case REFERENCE:
                        xmlText = retrieveReferenceRatingSetXml(connection, officeId, ratingSpecId);
                        break;
                }
            } finally {
                conn.closeConnection(connection);
            }
            RatingSet.getLogger().log(Level.FINE, "Retrieved XML:\n" + xmlText);
            return xmlText;
        } catch (RuntimeException t) {
            throw new RatingException(t);
        }
    }

    private static String retrieveRatingSetXml(Connection conn, String officeId, String ratingSpecId, Timestamp startTime, Timestamp endTime,
                                               boolean dataTimes) {
        //------------------------------------//
        // Load all rating data from database //
        //------------------------------------//
        String ratingXml;
        if (dataTimes) {
            ratingXml = CWMS_RATING_PACKAGE.call_RETRIEVE_EFF_RATINGS_XML3(getConfiguration(conn), ratingSpecId, startTime, endTime, "UTC", officeId);
        } else {
            ratingXml = CWMS_RATING_PACKAGE.call_RETRIEVE_RATINGS_XML3(getConfiguration(conn), ratingSpecId, startTime, endTime, "UTC", officeId);
        }
        return ratingXml;
    }

    private static String retrieveLazyRatingSetXml(Connection conn, String officeId, String ratingSpecId, Timestamp startTime, Timestamp endTime,
                                                   boolean dataTimes) {
        //-----------------------------------------------//
        // load only spec and rating times from database //
        //-----------------------------------------------//
        RatingSet.getLogger().log(Level.FINE, "Retrieving XML from database");
        String xmlText =
            CWMS_RATING_PACKAGE.call_RETRIEVE_RATINGS_XML_DATA(getConfiguration(conn), dataTimes ? "T" : "F", ratingSpecId, startTime, endTime, "UTC",
                true, true, true, true, "F", officeId);
        RatingSet.getLogger().log(Level.FINE, "XML retrieved from database");
        RatingSet.getLogger().log(Level.FINE, "XML length = " + xmlText);
        return xmlText;
    }

    static String retrieveReferenceRatingSetXml(Connection conn, String officeId, String ratingSpecId) throws RatingException {
        String xmlText;
        //----------------------------------------//
        // Load only /template+spec from database //
        //----------------------------------------//
        String[] parts = TextUtil.split(ratingSpecId, SEPARATOR1);
        String ratingTemplateId = TextUtil.join(SEPARATOR1, parts[1], parts[2]);
        String templateXmlText = getRatingTemplateXmlFromDatabase(conn, officeId, ratingTemplateId);
        StringBuilder sb = new StringBuilder();
        sb.append(templateXmlText, 0, templateXmlText.indexOf("</ratings>"));
        sb.append("  ");
        String specXmlText = getRatingSpecXmlFromDatabase(conn, officeId, ratingSpecId);
        sb.append(specXmlText.substring(specXmlText.indexOf("<rating-spec")));
        xmlText = sb.toString();
        return xmlText;
    }

    /**
     * Public constructor a CWMS database connection. The system property default for database load method is used for choosing an implementation.
     *
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
     * @throws RatingException any issues retrieving the data or processing what's returned
     */
    public static AbstractRatingSet ratingSet(ConnectionProvider conn, String officeId, String ratingSpecId, Long startTime, Long endTime,
                                              boolean dataTimes)
        throws RatingException {
        return ratingSet(null, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
    }

    /**
     * Public constructor a CWMS database connection
     *
     * @param databaseLoadMethod The method used to load the object from the database. If null, the value of the property
     *                           "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
     *                           are null (or if an invalid value is specified) the Lazy method will be used.
     *                               <table border="1">
     *                           <caption>Loading Methods</caption>
     *                                 <tr>
     *                                   <th>Value (case insensitive)</th>
     *                                   <th>Interpretation</th>
     *                                 </tr>
     *                                 <tr>
     *                                   <td>Eager</td>
     *                                   <td>Ratings for all effective times are loaded initially</td>
     *                                   <td>Lazy</td>
     *                                   <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
     *                                   <td>Reference</td>
     *                                   <td>No ratings are loaded ever - values are passed to database to be rated</td>
     *                                 </tr>
     *                               </table>
     * @param conn               The connection to a CWMS database
     * @param officeId           The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId       The rating specification identifier
     * @param startTime          The earliest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no earliest limit is set.
     * @param endTime            The latest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no latest limit is set.
     * @param dataTimes          Determines how startTime and endTime are interpreted.
     *                               <table border="1">
     *                           <caption>Start/End Time values</caption>
     *                                 <tr>
     *                                   <th>Value</th>
     *                                   <th>Interpretation</th>
     *                                 </tr>
     *                                 <tr>
     *                                   <td>false</td>
     *                                   <td>The time window specifies the extent of when the ratings became effective</td>
     *                                   <td>true</td>
     *                                   <td>Time time window specifies the time extent of data rate</td>
     *                                 </tr>
     *                               </table>
     * @throws RatingException any issues retrieving the data or processing what's returned
     */
    public static AbstractRatingSet ratingSet(RatingSet.DatabaseLoadMethod databaseLoadMethod, ConnectionProvider conn, String officeId,
                                              String ratingSpecId,
                                              Long startTime, Long endTime, boolean dataTimes) throws RatingException {
        RatingSet.DatabaseLoadMethod loadMethod = getDatabaseLoadMethod(databaseLoadMethod);
        AbstractRatingSet retval;
        switch (loadMethod) {
            case EAGER:
                retval = eagerRatingSet(conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
                break;
            case LAZY:
                retval = lazyRatingSet(conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
                break;
            case REFERENCE:
                retval = referenceRatingSet(conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
                break;
            default:
                throw new RatingException("Unsupported database load method: " + loadMethod);
        }
        return retval;
    }

    /**
     * Generates a new JdbcRatingSet object from a CWMS database connection. No ratings are loaded ever - values are passed to database to be rated
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
     * @throws RatingException any issues retrieving the data or processing what's returned
     */
    public static JdbcRatingSet referenceRatingSet(ConnectionProvider conn, String officeId, String ratingSpecId, Long startTime, Long endTime,
                                                   boolean dataTimes) throws RatingException {
        String ratingXml = getXmlFromDatabase(RatingSet.DatabaseLoadMethod.REFERENCE, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
        RatingSetContainer ratingSetContainer = RatingSetContainerXmlFactory.ratingSetContainerFromXml(ratingXml, false).clone();
        if (ratingSetContainer instanceof ReferenceRatingContainer) {
            JdbcRatingSet.DbInfo dbInfo = getDbInfo(conn);
            ReferenceRating dbrating = new ReferenceRating((ReferenceRatingContainer) ratingSetContainer);
            ReferenceJdbcRatingSet referenceJdbcRatingSet = new ReferenceJdbcRatingSet(conn, dbInfo, dbrating);
            dbrating.parent = referenceJdbcRatingSet;
            referenceJdbcRatingSet.setRatingSpec(new RatingSpec(ratingSetContainer.ratingSpecContainer));
            return referenceJdbcRatingSet;
        } else {
            throw new RatingException("Cannot create a ReferenceRating without a ReferenceRatingContainer");
        }
    }

    /**
     * Generates a new JdbcRatingSet object from a CWMS database connection. No ratings are loaded initially - each rating is only loaded when it is needed
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
     * @throws RatingException any issues retrieving the data or processing what's returned
     */
    public static JdbcRatingSet lazyRatingSet(ConnectionProvider conn, String officeId, String ratingSpecId, Long startTime, Long endTime,
                                              boolean dataTimes)
        throws RatingException {
        String ratingXml = getXmlFromDatabase(RatingSet.DatabaseLoadMethod.LAZY, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
        RatingSetContainer ratingSetContainer = RatingSetContainerXmlFactory.ratingSetContainerFromXml(ratingXml, false).clone();
        JdbcRatingSet.DbInfo dbInfo = getDbInfo(conn);
        LazyJdbcRatingSet lazyJdbcRatingSet = new LazyJdbcRatingSet(conn, dbInfo);
        lazyJdbcRatingSet.setData(ratingSetContainer);
        if (ratingSetContainer.state != null) {
            lazyJdbcRatingSet.setState(ratingSetContainer.state);
        }
        return lazyJdbcRatingSet;
    }

    /**
     * Generates a new JdbcRatingSet object from a CWMS database connection. Ratings for all effective times are loaded initially
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
     * @throws RatingException any issues retrieving the data or processing what's returned
     */
    public static AbstractRatingSet eagerRatingSet(ConnectionProvider conn, String officeId, String ratingSpecId, Long startTime, Long endTime,
                                                   boolean dataTimes) throws RatingException {
        String ratingXml = getXmlFromDatabase(RatingSet.DatabaseLoadMethod.EAGER, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
        return RatingXmlFactory.ratingSet(ratingXml);
    }

    private static JdbcRatingSet.DbInfo getDbInfo(ConnectionProvider conn) throws RatingException {
        JdbcRatingSet.DbInfo dbInfo;
        try {
            Connection connection = conn.getConnection();
            try {
                DSLContext dsl = DSL.using(connection);
                Configuration configuration = dsl.configuration();
                String dbUserId = CWMS_UTIL_PACKAGE.call_GET_USER_ID(configuration);
                String dbOfficeId = CWMS_UTIL_PACKAGE.call_USER_OFFICE_ID(configuration);
                dbInfo = new JdbcRatingSet.DbInfo(connection.getMetaData().getURL(), dbUserId, dbOfficeId);
            } finally {
                conn.closeConnection(connection);
            }
        } catch (RuntimeException | SQLException e) {
            throw new RatingException(e);
        }
        return dbInfo;
    }

    /**
     * Stores the rating template to a CWMS database
     *
     * @param ratingTemplate    data object to store
     * @param conn              The connection to the CWMS database
     * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
     * @throws RatingException any issues storing this to the database
     */
    public static void store(RatingTemplate ratingTemplate, Connection conn, boolean overwriteExisting) throws RatingException {
        String templateXml = RatingSpecXmlFactory.toXml(ratingTemplate, "", 0);
        storeXml(conn, templateXml, overwriteExisting, true);
    }

    /**
     * Stores the rating specification (without template) to a CWMS database
     *
     * @param ratingSpec        data object to store
     * @param conn              The connection to the CWMS database
     * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
     * @throws RatingException any issues storing this to the database
     */
    public static void store(RatingSpec ratingSpec, Connection conn, boolean overwriteExisting, boolean storeTemplate) throws RatingException {
        String xml = RatingSpecXmlFactory.toXml(ratingSpec.getData(), "", 0, storeTemplate);
        storeXml(conn, xml, overwriteExisting, true);
    }

    /**
     * Factory constructor from the CWMS database
     *
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @throws RatingException any issues retrieving the data or processing what's returned
     */
    public static RatingSpec ratingSpec(Connection conn, String officeId, String ratingSpecId) throws RatingException {
        try {
            String specXml = getRatingSpecXmlFromDatabase(conn, officeId, ratingSpecId);
            String[] parts = TextUtil.split(ratingSpecId, SEPARATOR1);
            String templateId = TextUtil.join(SEPARATOR1, parts[1], parts[2]);
            String templateXml = getRatingTemplateXmlFromDatabase(conn, officeId, templateId);
            return RatingSpecXmlFactory.ratingSpec(templateXml, specXml);
        } catch (RuntimeException t) {
            throw new RatingException(t);
        }
    }

    /**
     * Retrieves a RatingTemplate XML instance from a CWMS database connection
     *
     * @param conn         The connection to a CWMS database
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connected user is used.
     * @param ratingSpecId The rating specification identifier
     * @return XML string from the database
     * @throws RatingException any issues retrieving the data or processing what's returned
     */
    public static String getRatingSpecXmlFromDatabase(Connection conn, String officeId, String ratingSpecId) throws RatingException {
        try {
            Configuration configuration = getConfiguration(conn);
            return CWMS_RATING_PACKAGE.call_RETRIEVE_SPECS_XML(configuration, ratingSpecId, officeId);
        } catch (RuntimeException t) {
            throw new RatingException(t);
        }
    }

    /**
     * Factory constructor from a CWMS database connection
     *
     * @param conn       The connection to a CWMS database
     * @param officeId   The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param templateId The rating template identifier
     * @throws RatingException any issues with retrieving the data.
     */
    public static RatingTemplate ratingTemplate(Connection conn, String officeId, String templateId) throws RatingException {
        try {
            String templateXml = getRatingTemplateXmlFromDatabase(conn, officeId, templateId);
            return RatingSpecXmlFactory.ratingTemplate(templateXml);
        } catch (RuntimeException t) {
            throw new RatingException(t);
        }
    }

    /**
     * Generates a new RatingTemplate object from a CWMS database connection
     *
     * @param conn             The connection to a CWMS database
     * @param officeId         The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingTemplateId The rating template identifier
     * @return the rating template in XML form
     * @throws RatingException any issues retrieving the data or processing what's returned
     */
    public static String getRatingTemplateXmlFromDatabase(Connection conn, String officeId, String ratingTemplateId) throws RatingException {
        try {
            Configuration configuration = getConfiguration(conn);
            return CWMS_RATING_PACKAGE.call_RETRIEVE_TEMPLATES_XML(configuration, ratingTemplateId, officeId);
        } catch (RuntimeException t) {
            throw new RatingException(t);
        }
    }
}
