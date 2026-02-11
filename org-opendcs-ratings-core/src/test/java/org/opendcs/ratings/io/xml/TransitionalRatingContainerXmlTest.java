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

package org.opendcs.ratings.io.xml;

import mil.army.usace.hec.metadata.constants.NumericalConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.io.TransitionalRatingContainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TransitionalRatingContainerXmlTest {

    private TransitionalRatingContainer transitional;

    @BeforeEach
    public void setup() throws IOException, RatingException {
        try (InputStream inputStream = getClass().getResourceAsStream("transitional_rating.xml")) {
            Assertions.assertNotNull(inputStream);
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                 Stream<String> stream = bufferedReader.lines()) {
                String text = stream.collect(Collectors.joining("\n"));
                transitional = new TransitionalRatingContainer(text);
            }
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
