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


import org.opendcs.ratings.AbstractRating;
import org.opendcs.ratings.AbstractRatingSet;
import org.opendcs.ratings.RatingSet;
import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.RatingSpec;
import org.opendcs.ratings.io.RatingXmlCompatUtil;
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
