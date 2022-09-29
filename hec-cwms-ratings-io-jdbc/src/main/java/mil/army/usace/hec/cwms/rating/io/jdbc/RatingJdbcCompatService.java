/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.jdbc;

import hec.data.RatingException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.AbstractRatingSet;
import hec.data.cwmsRating.RatingSet;
import hec.data.cwmsRating.RatingSpec;
import hec.data.cwmsRating.RatingTemplate;
import hec.data.cwmsRating.io.RatingJdbcCompatUtil;
import java.sql.Connection;
import java.sql.SQLException;
import rma.services.annotations.ServiceProvider;

@ServiceProvider(service = RatingJdbcCompatUtil.class)
public final class RatingJdbcCompatService implements RatingJdbcCompatUtil {
    @Override
    public AbstractRating fromDatabase(Connection conn, String officeId, String ratingSpecId, Long effectiveDate) throws RatingException {
        return RatingJdbcFactory.retrieveRating(conn, officeId, ratingSpecId, effectiveDate);
    }

    @Override
    public void storeToDatabase(AbstractRating abstractRating, Connection conn, boolean overwriteExisting) throws RatingException {
        RatingJdbcFactory.store(abstractRating, conn, overwriteExisting);
    }

    @Override
    public void storeToDatabase(RatingSet ratingSet, Connection conn, boolean overwriteExisting, boolean includeTemplate) throws RatingException {
        RatingJdbcFactory.store(ratingSet, conn, overwriteExisting, includeTemplate);
    }

    @Override
    public String getXmlFromDatabase(RatingSet.DatabaseLoadMethod loadMethod, Connection conn, String officeId, String ratingSpecId, Long startTime,
                                     Long endTime, boolean dataTimes) throws RatingException {
        return RatingJdbcFactory.getXmlFromDatabase(loadMethod, new TransientConnectionProvider(conn), officeId, ratingSpecId, startTime, endTime,
            dataTimes);
    }

    @Override
    public AbstractRatingSet fromDatabase(RatingSet.DatabaseLoadMethod loadMethod, Connection conn, String office, String ratingSpecId,
                                          Long startTime, Long endTime, boolean dataTimes) throws RatingException {
        return RatingJdbcFactory.ratingSet(loadMethod, new TransientConnectionProvider(conn), office, ratingSpecId, startTime, endTime, dataTimes);
    }

    @Override
    public RatingSpec specFromDatabase(Connection conn, String officeId, String ratingSpecId) throws RatingException {
        return RatingJdbcFactory.ratingSpec(conn, officeId, ratingSpecId);
    }

    @Override
    public String getSpecXmlFromDatabase(Connection conn, String officeId, String ratingSpecId) throws RatingException {
        return RatingJdbcFactory.getRatingSpecXmlFromDatabase(conn, officeId, ratingSpecId);
    }

    @Override
    public RatingTemplate templateFromDatabase(Connection conn, String officeId, String ratingSpecId) throws RatingException {
        return RatingJdbcFactory.ratingTemplate(conn, officeId, ratingSpecId);
    }

    @Override
    public String getTemplateXmlFromDatabase(Connection conn, String officeId, String ratingTemplateId) throws RatingException {
        return RatingJdbcFactory.getRatingSpecXmlFromDatabase(conn, officeId, ratingTemplateId);
    }

    @Override
    public void storeToDatabase(RatingTemplate ratingTemplate, Connection conn, boolean overwriteExisting) throws RatingException {
        RatingJdbcFactory.store(ratingTemplate, conn, overwriteExisting);
    }

    @Override
    public void storeToDatabase(RatingSpec ratingSpec, Connection conn, boolean overwriteExisting, boolean storeTemplate) throws RatingException {
        RatingJdbcFactory.store(ratingSpec, conn, overwriteExisting, storeTemplate);
    }
}
