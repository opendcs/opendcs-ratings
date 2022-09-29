/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.xml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hec.data.RatingException;
import hec.data.cwmsRating.io.TransitionalRatingContainer;
import hec.data.cwmsRating.io.TransitionalRatingContainer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mil.army.usace.hec.metadata.constants.NumericalConstants;
import org.jdom.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransitionalRatingContainerXmlTest {

    private TransitionalRatingContainer transitional;

    @BeforeEach
    public void setup() throws IOException, RatingException {
        try (InputStream inputStream = getClass().getResourceAsStream("transitional_rating.xml");
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
             Stream<String> stream = bufferedReader.lines()) {
            String text = stream.collect(Collectors.joining("\n"));
            transitional = new TransitionalRatingContainer(text);
        }
    }

    @Test
    void testXmlSerialization() throws RatingException {
        String xml = transitional.toXml("");
        TransitionalRatingContainer newContainer = new TransitionalRatingContainer(xml);
        assertEquals(transitional, newContainer, "Serialized object should equal original when deserialized");
    }

    @Test
    void testXmlDeserialization() {
        assertEquals("ADAM6.Stage,Speed-Water Index;Flow.Transitional.Production", transitional.ratingSpecId);
        assertEquals("SWF", transitional.officeId);
        assertEquals("ft,mph;cfs", transitional.unitsId);
        assertTrue(transitional.active);
        assertEquals(Instant.parse("1900-01-01T06:00:00Z"), Instant.ofEpochMilli(transitional.effectiveDateMillis));
        assertEquals(NumericalConstants.UNDEFINED_INSTANT, Instant.ofEpochMilli(transitional.transitionStartDateMillis));
        assertEquals(Instant.parse("2017-04-06T18:32:00Z"), Instant.ofEpochMilli(transitional.createDateMillis));
        assertEquals("", transitional.description);
        assertNull(transitional.getVerticalDatumContainer());

        assertEquals(2, transitional.sourceRatings.length);
        assertArrayEquals(new String[]{"R1", "R2"}, transitional.evaluations);
        assertArrayEquals(new String[]{"SWF/ADAM6.Stage,Speed-Water Index;Flow.Linear.Dummy", "SWF/ADAM6.Stage,Speed-Water Index;Flow.Linear.Production"}, transitional.sourceRatingIds);
        assertArrayEquals(new String[]{"I1 GT 25"}, transitional.conditions);
    }
}
