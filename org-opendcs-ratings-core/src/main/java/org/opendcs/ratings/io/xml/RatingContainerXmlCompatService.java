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


import org.opendcs.ratings.RatingObjectDoesNotExistException;
import org.opendcs.ratings.io.RatingContainerXmlCompatUtil;
import org.opendcs.ratings.io.AbstractRatingContainer;
import org.opendcs.ratings.io.ExpressionRatingContainer;
import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.io.RatingSetContainer;
import org.opendcs.ratings.io.RatingSpecContainer;
import org.opendcs.ratings.io.RatingTemplateContainer;
import org.opendcs.ratings.io.TableRatingContainer;
import org.opendcs.ratings.io.TransitionalRatingContainer;
import org.opendcs.ratings.io.UsgsStreamTableRatingContainer;
import org.opendcs.ratings.io.VirtualRatingContainer;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;
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
        return createRatingSetContainer(org.opendcs.ratings.XmlUtil.elementToText(ratingElement));
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
