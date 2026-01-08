/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package hec.data.cwmsRating.io;


import hec.data.RatingRuntimeException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.AbstractRatingSet;
import hec.data.cwmsRating.RatingSet;
import hec.data.cwmsRating.RatingSpec;
import hec.hecmath.TextMath;
import hec.io.TextContainer;
import org.w3c.dom.Node;
import rma.util.lookup.Lookup;

public interface RatingXmlCompatUtil {

    static RatingXmlCompatUtil getInstance() throws RatingRuntimeException {
        RatingXmlCompatUtil service = Lookup.getDefault().lookup(RatingXmlCompatUtil.class);
        if (service == null) {
            throw new RatingRuntimeException("Backwards compatibility module is not on the classpath for deprecated method support");
        }
        return service;
    }

    AbstractRating fromXml(String xmlText) throws RatingException;

    String toXml(AbstractRating abstractRating, CharSequence indent, int indentLevel) throws RatingException;

    String toXml(RatingSet ratingSet, CharSequence indent);

    String toCompressedXml(RatingSet ratingSet) throws RatingException;

    RatingSet createRatingSet(String xmlText) throws RatingException;

    AbstractRatingSet createRatingSet(TextContainer tc) throws RatingException;

    AbstractRatingSet createRatingSet(String xmlStr, boolean isCompressed) throws RatingException;

    RatingSpec createRatingSpec(String templateXml, String specXml) throws RatingException;

    RatingSpec createRatingSpec(Node templateNode, Node specNode) throws RatingException;

    String toXml(RatingSpec ratingSpec, CharSequence indent, int level, boolean includeTemplate);

    TextContainer getDssData(AbstractRatingSet composedRatingSet) throws RatingException;

    AbstractRatingSet createRatingSet(TextMath tm) throws RatingException;
}
