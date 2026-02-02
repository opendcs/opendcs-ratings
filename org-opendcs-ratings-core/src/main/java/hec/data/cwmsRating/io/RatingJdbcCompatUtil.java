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

package org.opendcs.ratings.io;

import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.RatingRuntimeException;
import org.opendcs.ratings.AbstractRating;
import org.opendcs.ratings.AbstractRatingSet;
import org.opendcs.ratings.RatingSet;
import org.opendcs.ratings.RatingSpec;
import org.opendcs.ratings.RatingTemplate;
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
