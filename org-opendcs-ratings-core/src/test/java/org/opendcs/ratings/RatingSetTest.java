package org.opendcs.ratings;

import hec.data.RoundingException;
import org.junit.jupiter.api.Test;
import mil.army.usace.hec.metadata.VerticalDatumContainer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RatingSetTest
{
    private static RatingSpec getRatingSpec() throws RatingException, RoundingException {
        RatingSpec ratingSpec = new RatingSpec();
        ratingSpec.setLocationId("LOCATION1");
        ratingSpec.setOfficeId("SPK");
        ratingSpec.setTemplateId("Elev;Area.Standard");
        ratingSpec.setVersion("Production");
        ratingSpec.setParametersId("Elev;Area.Standard.Production");
        ratingSpec.setActive(true);
        ratingSpec.setAutoUpdate(true);
        ratingSpec.setAutoActivate(true);
        ratingSpec.setIndRoundingSpecs(new String[]{"4444444444"});
        ratingSpec.setDepRoundingSpec("4444444444");
        ratingSpec.setDescription("");
        return ratingSpec;
    }

    @Test
    void testAddRatingsPreservesCurrentVerticalDatum() throws Exception
    {
        ConcreteRatingSet ratingSet = new ConcreteRatingSet();
        TableRating rating = new TableRating();
        RatingSpec ratingSpec = getRatingSpec();
        VerticalDatumContainer vdc = new VerticalDatumContainer();
        vdc.nativeDatum = "NGVD29";
        vdc.currentDatum = "NAVD88";
        rating.effectiveDate = Instant.now().toEpochMilli();
        rating.ratingSpec = ratingSpec;
        rating.setVerticalDatumContainer(vdc);
        //test addRating
        String expectedNative = rating.getNativeVerticalDatum();
        String expectedCurrent = rating.getCurrentVerticalDatum();
        ratingSet.addRating(rating);
        assertEquals(expectedNative, ratingSet.getNativeVerticalDatum());
        assertEquals(expectedCurrent, ratingSet.getCurrentVerticalDatum());
    }

    @Test
    void testAddRatingsRecognizesCurrentVerticalDatumWithDash() throws Exception
    {
        ConcreteRatingSet ratingSet = new ConcreteRatingSet();
        TableRating rating = new TableRating();
        RatingSpec ratingSpec = getRatingSpec();
        VerticalDatumContainer vdc = new VerticalDatumContainer();
        vdc.unit = "ft";
        vdc.nativeDatum = "NGVD29";
        vdc.currentDatum = "NAVD-88";
        vdc.addOffset("NAVD88", 1.0, false);
        rating.effectiveDate = Instant.now().toEpochMilli();
        rating.ratingSpec = ratingSpec;
        rating.setVerticalDatumContainer(vdc);
        //test addRating with dash in current datum
        ratingSet.addRating(rating);
        assertTrue(ratingSet.getData().getVerticalDatumContainer().toNativeVerticalDatum());
    }
}
