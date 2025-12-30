package hec.data.cwmsRating;

import org.junit.jupiter.api.Test;
import mil.army.usace.hec.metadata.VerticalDatumContainer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RatingSetTest
{
    @Test
    void testAddRatingsPreservesCurrentVerticalDatum() throws Exception
    {
        ConcreteRatingSet ratingSet = new ConcreteRatingSet();
        TableRating rating = new TableRating();
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
}
