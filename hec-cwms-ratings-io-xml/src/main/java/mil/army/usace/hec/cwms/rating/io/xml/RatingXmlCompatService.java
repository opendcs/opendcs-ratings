/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.xml;


import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.AbstractRatingSet;
import hec.data.cwmsRating.RatingSet;
import hec.data.cwmsRating.RatingSpec;
import hec.data.cwmsRating.io.RatingXmlCompatUtil;
import hec.hecmath.TextMath;
import hec.io.TextContainer;
import org.w3c.dom.Node;
import rma.services.annotations.ServiceProvider;

@ServiceProvider(service = RatingXmlCompatUtil.class)
public class RatingXmlCompatService implements RatingXmlCompatUtil {
    @Override
    public AbstractRating fromXml(String xmlText) throws RatingException {
        return RatingXmlFactory.abstractRating(xmlText);
    }

    @Override
    public String toXml(AbstractRating rating, CharSequence indent, int indentLevel) throws RatingException {
        return RatingXmlFactory.toXml(rating, indent, indentLevel);
    }

    @Override
    public String toXml(RatingSet ratingSet, CharSequence indent) {
        return RatingXmlFactory.toXml(ratingSet, indent);
    }

    @Override
    public String toCompressedXml(RatingSet ratingSet) throws RatingException {
        return RatingXmlFactory.toCompressedXml(ratingSet);
    }

    @Override
    public RatingSet createRatingSet(String xmlText) throws RatingException {
        return RatingXmlFactory.ratingSet(xmlText);
    }

    @Override
    public AbstractRatingSet createRatingSet(TextContainer tc) throws RatingException {
        return RatingXmlFactory.ratingSet(tc);
    }

    @Override
    public AbstractRatingSet createRatingSet(String xmlStr, boolean isCompressed) throws RatingException {
        return RatingXmlFactory.ratingSet(xmlStr, isCompressed);
    }

    @Override
    public RatingSpec createRatingSpec(String templateXml, String specXml) throws RatingException {
        return RatingSpecXmlFactory.ratingSpec(templateXml, specXml);
    }

    @Override
    public RatingSpec createRatingSpec(Node templateNode, Node specNode) throws RatingException {
        return RatingSpecXmlFactory.ratingSpec(templateNode, specNode);
    }

    @Override
    public String toXml(RatingSpec ratingSpec, CharSequence indent, int level, boolean includeTemplate) {
        return RatingSpecXmlFactory.toXml(ratingSpec, indent, level, includeTemplate);
    }

    @Override
    public TextContainer getDssData(AbstractRatingSet ratingSet) throws RatingException {
        return RatingXmlFactory.textContainer(ratingSet);
    }

    @Override
    public AbstractRatingSet createRatingSet(TextMath tm) throws RatingException {
        return RatingXmlFactory.ratingSet(tm);
    }
}
