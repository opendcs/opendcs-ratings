/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.radar;

import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import static hec.util.TextUtil.join;

import hec.data.RatingException;
import hec.data.cwmsRating.AbstractRatingSet;
import hec.data.cwmsRating.RatingSet;
import hec.data.cwmsRating.RatingSpec;
import hec.data.cwmsRating.RatingTemplate;
import hec.data.cwmsRating.io.RatingSpecContainer;
import hec.data.cwmsRating.io.RatingTemplateContainer;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.logging.Level;
import mil.army.usace.hec.cwms.http.client.ApiConnectionInfo;
import mil.army.usace.hec.cwms.radar.client.controllers.RatingController;
import mil.army.usace.hec.cwms.radar.client.controllers.RatingEndpointInput;
import mil.army.usace.hec.cwms.radar.client.controllers.RatingSpecController;
import mil.army.usace.hec.cwms.radar.client.controllers.RatingSpecEndpointInput;
import mil.army.usace.hec.cwms.radar.client.controllers.RatingTemplateController;
import mil.army.usace.hec.cwms.radar.client.controllers.RatingTemplateEndpointInput;
import mil.army.usace.hec.cwms.radar.client.model.IndependentRoundingSpec;
import mil.army.usace.hec.cwms.radar.client.model.ParameterSpec;
import mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory;
import mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory;

public final class RatingRadarFactory {

    private RatingRadarFactory() {
        throw new AssertionError("Utility class");
    }

    private static RatingSet.DatabaseLoadMethod getDatabaseLoadMethod(RatingSet.DatabaseLoadMethod loadMethod) {
        String specifiedLoadMethod =
            (loadMethod == null ? System.getProperty("hec.data.cwmsRating.RatingSet.databaseLoadMethod", "lazy") : loadMethod.name()).toUpperCase();
        RatingSet.DatabaseLoadMethod databaseLoadMethod;
        try {
            databaseLoadMethod = RatingSet.DatabaseLoadMethod.valueOf(specifiedLoadMethod);
        } catch (RuntimeException ex) {
            if (RatingSet.getLogger().isLoggable(Level.FINE)) {
                RatingSet.getLogger().log(Level.WARNING, ex,
                    () -> "Invalid value for property hec.data.cwmsRating.RatingSet.databaseLoadMethod: " + specifiedLoadMethod +
                        "\nMust be one of " + Arrays.toString(RatingSet.DatabaseLoadMethod.values()));
            } else {
                RatingSet.getLogger().log(Level.WARNING,
                    () -> "Invalid value for property hec.data.cwmsRating.RatingSet.databaseLoadMethod: " + specifiedLoadMethod +
                        "\nMust be one of " + Arrays.toString(RatingSet.DatabaseLoadMethod.values()));
            }
            databaseLoadMethod = RatingSet.DatabaseLoadMethod.LAZY;
        }
        return databaseLoadMethod;
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
     * @param conn         The connection information to CWMS RADAR
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
    public static String getXmlFromDatabase(RatingSet.DatabaseLoadMethod loadMethod, ApiConnectionInfo conn, String officeId, String ratingSpecId,
                                            Long startTime, Long endTime, boolean dataTimes) throws RatingException {
        try {
            RatingSet.DatabaseLoadMethod databaseLoadMethod = getDatabaseLoadMethod(loadMethod);
            String xmlText;
            Timestamp startDate = null;
            if (startTime != null) {
                startDate = new Timestamp(startTime);
            }
            Timestamp endDate = null;
            if (endTime != null) {
                endDate = new Timestamp(endTime);
            }
            switch (databaseLoadMethod) {
                case EAGER:
                    xmlText = retrieveRatingSetXml(conn, officeId, ratingSpecId, startDate, endDate, dataTimes);
                    break;
                case LAZY:
                case REFERENCE:
                default:
                    throw new RatingException("Database load method: " + databaseLoadMethod + " is currently unsupported for CWMS RADAR client");
            }
            RatingSet.getLogger().log(Level.FINE, () -> "Retrieved XML:\n" + xmlText);
            return xmlText;
        } catch (RuntimeException t) {
            throw new RatingException(t);
        }
    }

    private static String retrieveRatingSetXml(ApiConnectionInfo conn, String officeId, String ratingSpecId, Timestamp startTime, Timestamp endTime,
                                               boolean dataTimes) throws RatingException {
        //------------------------------------//
        // Load all rating data from RADAR //
        //------------------------------------//
        RatingController ratingController = new RatingController();
        try {
            return ratingController.retrieveRatingXml(conn, RatingEndpointInput.getOne(ratingSpecId, officeId));
        } catch (IOException e) {
            throw new RatingException(e);
        }
    }

    /**
     * Public constructor a CWMS database connection. The system property default for database load method is used for choosing an implementation.
     *
     * @param conn         The connection information to CWMS RADAR
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
    public static AbstractRatingSet ratingSet(ApiConnectionInfo conn, String officeId, String ratingSpecId, Long startTime, Long endTime,
                                              boolean dataTimes) throws RatingException {
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
     * @param conn               The connection information to CWMS RADAR
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
    public static AbstractRatingSet ratingSet(RatingSet.DatabaseLoadMethod databaseLoadMethod, ApiConnectionInfo conn, String officeId,
                                              String ratingSpecId, Long startTime, Long endTime, boolean dataTimes) throws RatingException {
        RatingSet.DatabaseLoadMethod loadMethod = getDatabaseLoadMethod(databaseLoadMethod);
        AbstractRatingSet retval;
        switch (loadMethod) {
            case EAGER:
                retval = eagerRatingSet(conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
                break;
            case LAZY:
            case REFERENCE:
            default:
                throw new RatingException("Database load method: " + databaseLoadMethod + " is currently unsupported for CWMS RADAR client");
        }
        return retval;
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
    public static AbstractRatingSet eagerRatingSet(ApiConnectionInfo conn, String officeId, String ratingSpecId, Long startTime, Long endTime,
                                                   boolean dataTimes) throws RatingException {
        String ratingXml = getXmlFromDatabase(RatingSet.DatabaseLoadMethod.EAGER, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
        return RatingXmlFactory.ratingSet(ratingXml);
    }

    /**
     * Factory constructor from the CWMS database
     *
     * @param conn         The connection information to CWMS RADAR
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingSpecId The rating specification identifier
     * @throws RatingException any issues retrieving the data or processing what's returned
     */
    public static RatingSpec ratingSpec(ApiConnectionInfo conn, String officeId, String ratingSpecId) throws RatingException {
        try {
            RatingSpecController ratingSpecController = new RatingSpecController();
            RatingSpecEndpointInput input = new RatingSpecEndpointInput().ratingId(ratingSpecId).officeId(officeId);
            mil.army.usace.hec.cwms.radar.client.model.RatingSpec ratingSpec = ratingSpecController.retrieveRatingSpec(conn, input);

            RatingSpecContainer ratingSpecContainer = new RatingSpecContainer();
            ratingTemplate(conn, officeId, ratingSpec.getTemplateId()).getData().clone(ratingSpecContainer);
            ratingSpecContainer.active = ratingSpec.isActive();
            ratingSpecContainer.autoActivate = ratingSpec.isAutoActivate();
            ratingSpecContainer.autoMigrateExtensions = ratingSpec.isAutoMigrateExtension();
            ratingSpecContainer.autoUpdate = ratingSpec.isAutoUpdate();
            ratingSpecContainer.depRoundingSpec = ratingSpec.getDependentRoundingSpec();
            ratingSpecContainer.indRoundingSpecs =
                ratingSpec.getIndependentRoundingSpecs().stream().map(IndependentRoundingSpec::getValue).toArray(String[]::new);
            ratingSpecContainer.inRangeMethod = ratingSpec.getInRangeMethod();
            ratingSpecContainer.locationId = ratingSpec.getLocationId();
            ratingSpecContainer.outRangeHighMethod = ratingSpec.getOutRangeHighMethod();
            ratingSpecContainer.outRangeLowMethod = ratingSpec.getOutRangeLowMethod();
            ratingSpecContainer.sourceAgencyId = ratingSpec.getSourceAgency();
            ratingSpecContainer.specDescription = ratingSpec.getDescription();
            ratingSpecContainer.specId = ratingSpec.getRatingId();
            ratingSpecContainer.specOfficeId = ratingSpec.getOfficeId();
            ratingSpecContainer.specVersion = ratingSpec.getVersion();
            return new RatingSpec(ratingSpecContainer);
        } catch (RuntimeException | IOException t) {
            throw new RatingException(t);
        }
    }

    /**
     * Retrieves a RatingTemplate XML instance from a CWMS database connection
     *
     * @param conn         The connection information to CWMS RADAR
     * @param officeId     The identifier of the office owning the rating. If null, the office associated with the connected user is used.
     * @param ratingSpecId The rating specification identifier
     * @return XML string from the database
     * @throws RatingException any issues retrieving the data or processing what's returned
     */
    public static String getRatingSpecXmlFromDatabase(ApiConnectionInfo conn, String officeId, String ratingSpecId) throws RatingException {
        try {
            return RatingSpecXmlFactory.toXml(ratingSpec(conn, officeId, ratingSpecId), "", 0, false);
        } catch (RuntimeException t) {
            throw new RatingException(t);
        }
    }

    /**
     * Factory constructor from a CWMS database connection
     *
     * @param conn       The connection information to CWMS RADAR
     * @param officeId   The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param templateId The rating template identifier
     * @throws RatingException any issues with retrieving the data.
     */
    public static RatingTemplate ratingTemplate(ApiConnectionInfo conn, String officeId, String templateId) throws RatingException {
        try {
            RatingTemplateController ratingTemplateController = new RatingTemplateController();
            mil.army.usace.hec.cwms.radar.client.model.RatingTemplate ratingTemplate =
                ratingTemplateController.retrieveRatingTemplate(conn, new RatingTemplateEndpointInput().templateId(templateId).officeId(officeId));
            RatingTemplateContainer ratingTemplateContainer = new RatingTemplateContainer();
            ratingTemplateContainer.depParam = ratingTemplate.getDependentParameter();
            ratingTemplateContainer.indParams =
                ratingTemplate.getIndependentParameterSpecs().stream().map(ParameterSpec::getParameter).toArray(String[]::new);
            ratingTemplateContainer.inRangeMethods =
                ratingTemplate.getIndependentParameterSpecs().stream().map(ParameterSpec::getInRangeMethod).toArray(String[]::new);
            ratingTemplateContainer.officeId = ratingTemplate.getOfficeId();
            ratingTemplateContainer.outRangeHighMethods =
                ratingTemplate.getIndependentParameterSpecs().stream().map(ParameterSpec::getOutRangeHighMethod).toArray(String[]::new);
            ratingTemplateContainer.outRangeLowMethods =
                ratingTemplate.getIndependentParameterSpecs().stream().map(ParameterSpec::getOutRangeLowMethod).toArray(String[]::new);
            ratingTemplateContainer.parametersId =
                String.format("%s;%s", join(SEPARATOR3, ratingTemplateContainer.indParams), ratingTemplateContainer.depParam);
            ratingTemplateContainer.templateDescription = ratingTemplate.getDescription();
            ratingTemplateContainer.templateId = ratingTemplate.getId();
            ratingTemplateContainer.templateVersion = ratingTemplate.getVersion();
            return new RatingTemplate(ratingTemplateContainer);
        } catch (RuntimeException | IOException t) {
            throw new RatingException(t);
        }
    }

    /**
     * Generates a new RatingTemplate object from a CWMS database connection
     *
     * @param conn             The connection information to CWMS RADAR
     * @param officeId         The identifier of the office owning the rating. If null, the office associated with the connect user is used.
     * @param ratingTemplateId The rating template identifier
     * @return the rating template in XML form
     * @throws RatingException any issues retrieving the data or processing what's returned
     */
    public static String getRatingTemplateXmlFromDatabase(ApiConnectionInfo conn, String officeId, String ratingTemplateId) throws RatingException {
        try {
            return RatingSpecXmlFactory.toXml(ratingTemplate(conn, officeId, ratingTemplateId), "", 0);
        } catch (RuntimeException t) {
            throw new RatingException(t);
        }
    }
}
