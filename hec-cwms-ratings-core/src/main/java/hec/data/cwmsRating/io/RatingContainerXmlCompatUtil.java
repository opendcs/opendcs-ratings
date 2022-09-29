/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package hec.data.cwmsRating.io;

import hec.data.RatingException;
import hec.data.RatingObjectDoesNotExistException;
import hec.data.RatingRuntimeException;
import java.util.List;
import java.util.Set;
import org.jdom.Element;
import rma.util.lookup.Lookup;

public interface RatingContainerXmlCompatUtil {

    static RatingContainerXmlCompatUtil getInstance() throws RatingRuntimeException {
        RatingContainerXmlCompatUtil service = Lookup.getDefault().lookup(RatingContainerXmlCompatUtil.class);
        if (service == null) {
            throw new RatingRuntimeException("Backwards compatibility module is not on the classpath for deprecated method support");
        }
        return service;
    }

    VirtualRatingContainer createVirtualRatingContainer(String xmlText) throws RatingException;

    VirtualRatingContainer createVirtualRatingContainer(Element ratingElement) throws RatingException;

    String toXml(VirtualRatingContainer virtualRatingContainer, CharSequence indent, int level);

    void getSourceRatingsXml(VirtualRatingContainer virtualRatingContainer, CharSequence indent, int level, Set<String> templateStrings, Set<String> specStrings, List<String> ratingStrings);

    TableRatingContainer createTableRatingContainer(String xmlText) throws RatingException;

    TableRatingContainer createTableRatingContainer(Element ratingElement) throws RatingException;

    String toXml(TableRatingContainer tableRatingContainer, CharSequence indent, int level);

    UsgsStreamTableRatingContainer createUsgsStreamTableRatingContainer(String xmlText) throws RatingException;

    UsgsStreamTableRatingContainer createUsgsStreamTableRatingContainer(Element ratingElement) throws RatingException;

    String toXml(UsgsStreamTableRatingContainer usgsStreamTableRatingContainer, CharSequence indent, int level);

    TransitionalRatingContainer createTransitionalRatingContainer(String xmlText) throws RatingException;

    TransitionalRatingContainer createTransitionalRatingContainer(Element ratingElement) throws RatingException;

    String toXml(TransitionalRatingContainer transitionalRatingContainer, CharSequence indent, int level);

    void getSourceRatingsXml(TransitionalRatingContainer transitionalRatingContainer, CharSequence indent, int level, Set<String> templateStrings,
                             Set<String> specStrings, List<String> ratingStrings);

    ExpressionRatingContainer createExpressionRatingContainer(String xmlText) throws RatingException;

    ExpressionRatingContainer createExpressionRatingContainer(Element ratingElement) throws RatingException;

    AbstractRatingContainer createAbstractRatingContainer(String xmlStr) throws RatingException;

    String toXml(AbstractRatingContainer abstractRatingContainer, CharSequence indent, int level);

    RatingSetContainer createRatingSetContainer(String xmlText) throws RatingException;

    RatingSetContainer createRatingSetContainer(Element ratingElement) throws RatingException;

    String toXml(RatingSetContainer ratingSetContainer, CharSequence indent, int level, boolean includeTemplate, boolean includeEmptyTableRatings);

    RatingSpecContainer createRatingSpecContainer(String xmlStr) throws RatingObjectDoesNotExistException;

    String toXml(RatingSpecContainer ratingSpecContainer, CharSequence indent, int level, boolean includeTemplate);

    String toSpecXml(RatingSpecContainer ratingSpecContainer, CharSequence indent, int level);

    RatingTemplateContainer createRatingTemplateContainer(String xmlStr) throws RatingObjectDoesNotExistException;

    String toXml(RatingTemplateContainer ratingTemplateContainer, CharSequence indent, int level);

}
