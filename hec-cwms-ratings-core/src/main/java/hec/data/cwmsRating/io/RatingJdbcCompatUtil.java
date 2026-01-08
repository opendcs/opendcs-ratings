/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package hec.data.cwmsRating.io;

import hec.data.cwmsRating.RatingException;
import hec.data.RatingRuntimeException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.AbstractRatingSet;
import hec.data.cwmsRating.RatingSet;
import hec.data.cwmsRating.RatingSpec;
import hec.data.cwmsRating.RatingTemplate;
import java.sql.Connection;
import rma.util.lookup.Lookup;

public interface RatingJdbcCompatUtil {

    static RatingJdbcCompatUtil getInstance() throws RatingRuntimeException {
        RatingJdbcCompatUtil service = Lookup.getDefault().lookup(RatingJdbcCompatUtil.class);
        if (service == null) {
            throw new RatingRuntimeException("Backwards compatibility module is not on the classpath for deprecated method support");
        }
        return service;
    }

    AbstractRating fromDatabase(Connection conn, String officeId, String ratingSpecId, Long effectiveDate) throws RatingException;

    void storeToDatabase(AbstractRating abstractRating, Connection conn, boolean overwriteExisting) throws RatingException;

    void storeToDatabase(RatingSet ratingSet, Connection conn, boolean overwriteExisting, boolean includeTemplate) throws RatingException;

    String getXmlFromDatabase(RatingSet.DatabaseLoadMethod loadMethod, Connection conn, String officeId, String ratingSpecId, Long startTime, Long endTime, boolean dataTimes)
        throws RatingException;

    AbstractRatingSet fromDatabase(RatingSet.DatabaseLoadMethod loadMethod, Connection conn, String office, String ratingSpecId, Long startTime, Long endTime, boolean dataTimes)
        throws RatingException;

    RatingTemplate templateFromDatabase(Connection conn, String officeId, String ratingSpecId) throws RatingException;

    String getTemplateXmlFromDatabase(Connection conn, String officeId, String ratingTemplateId) throws RatingException;

    void storeToDatabase(RatingTemplate ratingTemplate, Connection conn, boolean overwriteExisting) throws RatingException;

    void storeToDatabase(RatingSpec ratingSpec, Connection conn, boolean overwriteExisting, boolean storeTemplate) throws RatingException;

    RatingSpec specFromDatabase(Connection conn, String officeId, String ratingSpecId) throws RatingException;

    String getSpecXmlFromDatabase(Connection conn, String officeId, String ratingSpecId) throws RatingException;

}
