package org.opendcs.ratings.io.xml;

import com.google.common.flogger.FluentLogger;
import org.junit.jupiter.api.Test;
import org.opendcs.ratings.RatingSet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.ratings.io.xml.RatingXmlFactory.ratingSet;

public class HugeRatingSetXmlTest {

    static FluentLogger logger = FluentLogger.forEnclosingClass();

    @Test
    public void testLoadHugeRatingSetFromXml() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("huge_rating_set.xml")) {
            assertNotNull(inputStream);
            try (
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    Stream<String> stream = bufferedReader.lines()) {
                String text = stream.collect(Collectors.joining("\n"));
                Instant t1 = Instant.now();
                RatingSet rs = ratingSet(text);
                Instant t2 = Instant.now();
                Duration d = Duration.between(t1, t2);
                boolean isFastEnough = d.compareTo(Duration.ofMillis(1000)) < 0;
                logger.atInfo().log("Deserializing RatingSet from XML (%d bytes) took %s", text.length(), d.toString());
                assertTrue(isFastEnough);
                t1 = Instant.now();
                String xml = RatingXmlFactory.toXml(rs, "  ");
                t2 = Instant.now();
                d = Duration.between(t1, t2);
                logger.atInfo().log("Serializing RatingSet to XML (%d bytes) took %s", xml.length(), d.toString());
            }
        }
    }
}
