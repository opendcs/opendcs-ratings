/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.xml;

import hec.data.RatingException;
import hec.data.RatingObjectDoesNotExistException;
import hec.data.cwmsRating.io.RatingContainerXmlCompatUtil;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.ExpressionRatingContainer;
import hec.data.cwmsRating.io.RatingSetContainer;
import hec.data.cwmsRating.io.RatingSpecContainer;
import hec.data.cwmsRating.io.RatingTemplateContainer;
import hec.data.cwmsRating.io.TableRatingContainer;
import hec.data.cwmsRating.io.TransitionalRatingContainer;
import hec.data.cwmsRating.io.UsgsStreamTableRatingContainer;
import hec.data.cwmsRating.io.VirtualRatingContainer;
import java.util.List;
import java.util.Set;
import org.jdom.Element;
import rma.services.annotations.ServiceProvider;

@ServiceProvider(service = RatingContainerXmlCompatUtil.class)
public final class RatingContainerXmlCompatService implements RatingContainerXmlCompatUtil {

    @Override
    public AbstractRatingContainer createAbstractRatingContainer(String xmlStr) throws RatingException {
        return RatingContainerXmlFactory.abstractRatingContainer(xmlStr);
    }

    @Override
    public VirtualRatingContainer createVirtualRatingContainer(String xmlText) throws RatingException {
        return RatingContainerXmlFactory.virtualRatingContainer(xmlText);
    }

    @Override
    public VirtualRatingContainer createVirtualRatingContainer(Element ratingElement) throws RatingException {
        return RatingContainerXmlFactory.virtualRatingContainer(ratingElement);
    }

    @Override
    public String toXml(VirtualRatingContainer virtualRatingContainer, CharSequence indent, int level) {
        return RatingContainerXmlFactory.toXml(virtualRatingContainer, indent, level);
    }

    @Override
    public void getSourceRatingsXml(VirtualRatingContainer virtualRatingContainer, CharSequence indent, int level, Set<String> templateStrings,
                                    Set<String> specStrings, List<String> ratingStrings) {
        RatingContainerXmlFactory.getSourceRatingsXml(virtualRatingContainer, indent, level, templateStrings, specStrings, ratingStrings);
    }

    @Override
    public TransitionalRatingContainer createTransitionalRatingContainer(String xmlText) throws RatingException {
        return RatingContainerXmlFactory.transitionalRatingContainer(xmlText);
    }

    @Override
    public TransitionalRatingContainer createTransitionalRatingContainer(Element ratingElement) throws RatingException {
        return RatingContainerXmlFactory.transitionalRatingContainer(ratingElement);
    }

    @Override
    public String toXml(TransitionalRatingContainer transitionalRatingContainer, CharSequence indent, int level) {
        return RatingContainerXmlFactory.toXml(transitionalRatingContainer, indent, level);
    }

    @Override
    public void getSourceRatingsXml(TransitionalRatingContainer transitionalRatingContainer, CharSequence indent, int level,
                                    Set<String> templateStrings, Set<String> specStrings, List<String> ratingStrings) {
        RatingContainerXmlFactory.getSourceRatingsXml(transitionalRatingContainer, indent, level, templateStrings, specStrings, ratingStrings);
    }

    @Override
    public ExpressionRatingContainer createExpressionRatingContainer(String xmlText) throws RatingException {
        return RatingContainerXmlFactory.expressionRatingContainer(xmlText);
    }

    @Override
    public ExpressionRatingContainer createExpressionRatingContainer(Element ratingElement) throws RatingException {
        return RatingContainerXmlFactory.expressionRatingContainer(ratingElement);
    }

    @Override
    public TableRatingContainer createTableRatingContainer(String xmlText) throws RatingException {
        return RatingContainerXmlFactory.tableRatingContainer(xmlText);
    }

    @Override
    public TableRatingContainer createTableRatingContainer(Element ratingElement) throws RatingException {
        return RatingContainerXmlFactory.tableRatingContainer(ratingElement);
    }

    @Override
    public String toXml(TableRatingContainer tableRatingContainer, CharSequence indent, int level) {
        return RatingContainerXmlFactory.toXml(tableRatingContainer, indent, level);
    }

    @Override
    public UsgsStreamTableRatingContainer createUsgsStreamTableRatingContainer(String xmlText) throws RatingException {
        return RatingContainerXmlFactory.usgsStreamTableRatingContainer(xmlText);
    }

    @Override
    public UsgsStreamTableRatingContainer createUsgsStreamTableRatingContainer(Element ratingElement) throws RatingException {
        return RatingContainerXmlFactory.usgsStreamTableRatingContainer(ratingElement);
    }

    @Override
    public String toXml(UsgsStreamTableRatingContainer usgsStreamTableRatingContainer, CharSequence indent, int level) {
        return RatingContainerXmlFactory.toXml(usgsStreamTableRatingContainer, indent, level);
    }

    @Override
    public String toXml(AbstractRatingContainer abstractRatingContainer, CharSequence indent, int level) {
        return RatingContainerXmlFactory.toXml(abstractRatingContainer, indent, level);
    }

    @Override
    public RatingSetContainer createRatingSetContainer(String xmlText) throws RatingException {
        return RatingSetContainerXmlFactory.ratingSetContainerFromXml(xmlText);
    }

    @Override
    public RatingSetContainer createRatingSetContainer(Element ratingElement) throws RatingException {
        return createRatingSetContainer(RatingXmlUtil.jdomElementToText(ratingElement));
    }

    @Override
    public String toXml(RatingSetContainer ratingSetContainer, CharSequence indent, int level, boolean includeTemplate,
                        boolean includeEmptyTableRatings) {
        return RatingContainerXmlFactory.toXml(ratingSetContainer, indent, level, includeTemplate, includeEmptyTableRatings);
    }

    @Override
    public RatingSpecContainer createRatingSpecContainer(String xmlText) throws RatingObjectDoesNotExistException {
        return RatingSpecXmlFactory.ratingSpecContainer(xmlText);
    }

    @Override
    public String toXml(RatingSpecContainer ratingSpecContainer, CharSequence indent, int level, boolean includeTemplate) {
        return RatingSpecXmlFactory.toXml(ratingSpecContainer, indent, level, includeTemplate);
    }

    @Override
    public String toSpecXml(RatingSpecContainer ratingSpecContainer, CharSequence indent, int level) {
        return RatingSpecXmlFactory.toSpecXml(ratingSpecContainer, indent, level);
    }

    @Override
    public RatingTemplateContainer createRatingTemplateContainer(String xmlStr) throws RatingObjectDoesNotExistException {
        return RatingSpecXmlFactory.ratingTemplateContainer(xmlStr);
    }

    @Override
    public String toXml(RatingTemplateContainer ratingTemplateContainer, CharSequence indent, int level) {
        return RatingSpecXmlFactory.toXml(ratingTemplateContainer, indent, level);
    }
}
