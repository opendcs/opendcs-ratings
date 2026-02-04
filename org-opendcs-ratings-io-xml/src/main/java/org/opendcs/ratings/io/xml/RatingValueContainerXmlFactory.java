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


import org.opendcs.ratings.io.RatingValueContainer;
import org.opendcs.ratings.io.TableRatingContainer;
import org.opendcs.ratings.RatingException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import static org.opendcs.ratings.io.xml.RatingXmlUtil.attributeText;
import static org.opendcs.ratings.io.xml.RatingXmlUtil.elementText;

final class RatingValueContainerXmlFactory {

    private RatingValueContainerXmlFactory() {
        throw new AssertionError("Utility class");
    }

    /**
     * Generates RatingValueContainer array from an XML node
     * @param ratingElement The XML rating node as an Element object
     * @return The generated RatingValueContainer array
     * @throws RatingException on error
     */
    static RatingValueContainer[] makeContainers(Element ratingElement, String pointsElementsName) throws RatingException {

        double[][] points;
        String[] notes = null;
        int width = -1;
        int depth = 0;
        //---------------------------------//
        // determine the shape of the data //
        //---------------------------------//
        NodeList pointsElems = ratingElement.getElementsByTagName(pointsElementsName);
        int count = pointsElems.getLength();
        if (count == 0) return null;
        for (int i = 0; i < count; ++i) {
            Element pointsElem = (Element) pointsElems.item(i);
            if (width == -1) {
                width = pointsElem.getElementsByTagName("other-ind").getLength(); // number of ind params - 1
            }
            if (pointsElem.getElementsByTagName("other-ind").getLength() != width) {
                throw new RatingException("Inconsistent number of independent parameters");
            }
            depth += pointsElem.getElementsByTagName("point").getLength();
        }
        width += 2; // now number of all params
        //------------------------------------//
        // parse the XML data into the arrays //
        //------------------------------------//
        points = new double[depth][];
        for (int i = 0; i < depth; ++i) points[i] = new double[width];
        double[] otherInds = new double[width-2];
        int row = 0;
        String note;
        for (int i = 0; i < count; ++i) {
            Element pointsElem = (Element) pointsElems.item(i);
            int col = 0;
            NodeList otherIndElems = pointsElem.getElementsByTagName("other-ind");
            count = otherIndElems.getLength();
            for (int j = 0; j < count; ++j) {
                Element otherIndElem = (Element) otherIndElems.item(j);
                int pos = Integer.parseInt(attributeText(otherIndElem, "position"));
                if (pos != col+1) {
                    throw new RatingException(String.format("Expected position %d, got %d on %s", col+1, pos, otherIndElem));
                }
                otherInds[col++] = Double.parseDouble(attributeText(otherIndElem, "value"));
            }
            NodeList pointElems = pointsElem.getElementsByTagName("point");
            count = pointElems.getLength();
            for (int j = 0; j < count; ++j) {
                Element pointElem = (Element) pointElems.item(j);
                for (col = 0; col < width-2; ++col) points[row][col] = otherInds[col];
                points[row][width-2] = Double.parseDouble(elementText((Element) pointElem.getElementsByTagName("ind").item(0)));
                points[row][width-1] = Double.parseDouble(elementText((Element) pointElem.getElementsByTagName("dep").item(0)));
                note = elementText((Element) pointElem.getElementsByTagName("note").item(0));
                if (!note.isEmpty()) {
                    if (notes == null) notes = new String[depth];
                    notes[row] = note;
                }
                ++row;
            }
        }
        return RatingValueContainer.makeContainers(points, notes, null, null, null);
    }
    static String toXml(RatingValueContainer ratingValueContainer, CharSequence prefix, CharSequence indent, StringBuilder sb) {
        return toXml(ratingValueContainer, prefix, indent, null, sb);
    }

    static String toXml(RatingValueContainer ratingValueContainer, CharSequence prefix, CharSequence indent, List<Double> otherIndParams, StringBuilder sb) {
        TableRatingContainer depTable = ratingValueContainer.depTable;
        double indValue = ratingValueContainer.indValue;
        double depValue = ratingValueContainer.depValue;
        String note = ratingValueContainer.note;
        if (depTable == null) {
            sb.append(prefix).append(indent).append("<point>\n");
            sb.append(prefix).append(indent).append(indent).append("<ind>").append(indValue).append("</ind>\n");
            sb.append(prefix).append(indent).append(indent).append("<dep>").append(depValue).append("</dep>\n");
            if (note != null) sb.append(prefix).append(indent).append(indent).append("<note>").append(note).append("</note>\n");
            sb.append(prefix).append(indent).append("</point>\n");
        }
        else {
            if (otherIndParams == null) {
                otherIndParams = new ArrayList<>();
            }
            otherIndParams.add(indValue);
            if (depTable.values[0].depTable == null) {
                sb.append(prefix).append("<rating-points>\n");
                for (int i = 0; i < otherIndParams.size(); ++i) {
                    sb.append(prefix).append(indent).append("<other-ind position=\"").append(i+1).append("\" value=\"").append(otherIndParams.get(i)).append("\"/>\n");
                }
            }
            for (RatingValueContainer rvc : depTable.values) {
                toXml(rvc, prefix, indent, otherIndParams, sb);
            }
            otherIndParams.remove(otherIndParams.size()-1);
            if (depTable.values[0].depTable == null) {
                sb.append(prefix).append("</rating-points>\n");
            }
        }
        return sb.toString();
    }
}
