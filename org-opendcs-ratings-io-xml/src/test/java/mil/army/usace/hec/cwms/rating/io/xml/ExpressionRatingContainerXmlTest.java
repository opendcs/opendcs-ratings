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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.io.ExpressionRatingContainer;
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

class ExpressionRatingContainerXmlTest {

    private ExpressionRatingContainer expressionRatingContainer;

    @BeforeEach
    public void setup() throws IOException, RatingException {
        try (InputStream inputStream = getClass().getResourceAsStream("expression_rating.xml");
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
             Stream<String> stream = bufferedReader.lines()) {
            String text = stream.collect(Collectors.joining("\n"));
            expressionRatingContainer = new ExpressionRatingContainer(text);
        }
    }

    @Test
    void testXmlSerialization() throws RatingException {
        String xml = expressionRatingContainer.toXml("");
        ExpressionRatingContainer newContainer = new ExpressionRatingContainer(xml);
        assertEquals(expressionRatingContainer, newContainer, "Serialized object should equal original when deserialized");
    }

    @Test
    void testXmlJDomSerialization() throws RatingException {
        String xml = expressionRatingContainer.toXml("");
        Element element = RatingXmlUtil.textToJdomElement(xml).getChild("simple-rating");
        ExpressionRatingContainer newContainer = new ExpressionRatingContainer(element);
        assertEquals(expressionRatingContainer, newContainer, "Serialized object should equal original when deserialized");
    }

    @Test
    void testXmlDeserialization() {
        assertEquals("ACTT2.Elev;Elev.Standard.Production", expressionRatingContainer.ratingSpecId);
        assertEquals("NAB", expressionRatingContainer.officeId);
        assertEquals("ft;ft", expressionRatingContainer.unitsId);
        assertTrue(expressionRatingContainer.active);
        assertEquals(Instant.parse("2018-04-11T16:47:00Z"), Instant.ofEpochMilli(expressionRatingContainer.effectiveDateMillis));
        assertEquals(NumericalConstants.UNDEFINED_INSTANT, Instant.ofEpochMilli(expressionRatingContainer.transitionStartDateMillis));
        assertEquals(Instant.parse("1970-01-01T00:00:00Z"), Instant.ofEpochMilli(expressionRatingContainer.createDateMillis));
        assertEquals("", expressionRatingContainer.description);
        assertNull(expressionRatingContainer.getVerticalDatumContainer());


        assertEquals("I1 + 1", expressionRatingContainer.expression);
    }
}
