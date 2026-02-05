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

package org.opendcs.ratings.io.cda;

import org.opendcs.ratings.AbstractRating;
import org.opendcs.ratings.AbstractRatingSet;
import org.opendcs.ratings.ConcreteRatingSet;
import org.opendcs.ratings.RatingConst;
import org.opendcs.ratings.RatingSpec;
import mil.army.usace.hec.cwms.http.client.ApiConnectionInfo;
import mil.army.usace.hec.cwms.http.client.ApiConnectionInfoBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RatingCdaFactoryTest
{

    @Disabled
    @Test
    public void testRatingSetRetrievalCda() throws Exception {
        String ratingId = "WARW.Stage;Flow.EXSA.PRODUCTION";
        ApiConnectionInfo apiConnectionInfo = new ApiConnectionInfoBuilder("http://localhost:11526/cwms-data/").build();
        AbstractRatingSet ratingSet = RatingCdaFactory.eagerRatingSet(apiConnectionInfo, "SWT", ratingId, null, null, true);
        assertNotNull(ratingSet);
        AbstractRating[] ratings = ratingSet.getRatings();
        assertEquals(15, ratings.length);
        assertInstanceOf(ConcreteRatingSet.class, ratingSet);
        RatingSpec ratingSpec = ratingSet.getRatingSpec();
        assertEquals(ratingId, ratingSpec.getRatingSpecId());
        assertEquals("Flow", ratingSpec.getDepParameter());
        assertEquals("2222233332", ratingSpec.getDepRoundingSpecString());
        assertEquals("Deep Fork at Warwick, OK Expanded, Shift-Adjusted PRODUCTION Stream Rating", ratingSpec.getDescription());
        assertEquals("/SWT/WARW/Stage;Flow//EXSA/PRODUCTION/", ratingSpec.getDssPathname());
        assertEquals(1, ratingSpec.getIndParamCount());
        assertArrayEquals(new String[]{"Stage"}, ratingSpec.getIndParameters());
        assertArrayEquals(new String[]{"2223456782"}, ratingSpec.getIndRoundingSpecStrings());
        assertEquals(RatingConst.RatingMethod.PREVIOUS, ratingSpec.getInRangeMethod());
        assertArrayEquals(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR}, ratingSpec.getInRangeMethods());
        assertEquals("WARW", ratingSpec.getLocationId());
        assertEquals("SWT", ratingSpec.getOfficeId());
        assertEquals(RatingConst.RatingMethod.PREVIOUS, ratingSpec.getOutRangeHighMethod());
        assertArrayEquals(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.NEAREST}, ratingSpec.getOutRangeHighMethods());
        assertEquals(RatingConst.RatingMethod.NEAREST, ratingSpec.getOutRangeLowMethod());
        assertArrayEquals(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.NEAREST}, ratingSpec.getOutRangeLowMethods());
        assertEquals("Stage;Flow", ratingSpec.getParametersId());
        assertEquals("Blah", ratingSpec.getSourceAgencyId());
        assertEquals("Stage;Flow.EXSA", ratingSpec.getTemplateId());
        assertEquals("PRODUCTION", ratingSpec.getVersion());
    }
}
