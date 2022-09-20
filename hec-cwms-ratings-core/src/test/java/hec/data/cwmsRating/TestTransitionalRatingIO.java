/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package hec.data.cwmsRating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import mil.army.usace.hec.metadata.constants.NumericalConstants;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class TestTransitionalRatingIO
{

    private String readFile(String jsonPath) throws IOException {
        URL resource = getClass().getClassLoader().getResource(jsonPath);
        if (resource == null) {
            throw new IOException("Resource not found: " + jsonPath);
        }
        Path path = new File(resource.getFile()).toPath();
        return String.join("\n", Files.readAllLines(path));
    }

    @Test
    public void testTransitionalRatingXml() throws Exception {
        String xml = readFile("hec/data/cwmsRating/transitional-rating.xml");
        try {
            RatingSet ratingSet = RatingSet.fromXml(xml);
            assertNotNull(ratingSet);
            //Was previously stack overflowing
            AbstractRating abstractRating = new TransitionalRating(xml);
            assertNotNull(abstractRating);
        } catch(Error error) {
            throw new Exception(error);
        }
    }

    @Test
    public void testVirtualRatingXml() throws Exception {
        String xml = readFile("hec/data/cwmsRating/virtual-rating.xml");
        try {
            RatingSet ratingSet = RatingSet.fromXml(xml);
            assertNotNull(ratingSet);
            //Was previously stack overflowing
            AbstractRating abstractRating = new VirtualRating(xml);
            assertNotNull(abstractRating);
        } catch(Error error) {
            throw new Exception(error);
        }
    }
}
