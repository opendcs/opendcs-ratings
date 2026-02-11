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

package org.opendcs.ratings.io;

import hec.hecmath.TextMath;
import hec.io.TextContainer;
import org.opendcs.ratings.*;
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
