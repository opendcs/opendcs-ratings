/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.cda;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.AbstractRatingSet;
import hec.data.cwmsRating.ConcreteRatingSet;
import hec.data.cwmsRating.RatingConst;
import hec.data.cwmsRating.RatingSpec;
import mil.army.usace.hec.cwms.http.client.ApiConnectionInfo;
import mil.army.usace.hec.cwms.http.client.ApiConnectionInfoBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
        assertTrue(ratingSet instanceof ConcreteRatingSet);
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
