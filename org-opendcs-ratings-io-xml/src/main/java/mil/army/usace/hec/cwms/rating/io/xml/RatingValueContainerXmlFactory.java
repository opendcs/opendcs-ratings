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
import java.util.ArrayList;
import java.util.List;
import org.jdom.Element;

final class RatingValueContainerXmlFactory {

    private RatingValueContainerXmlFactory() {
        throw new AssertionError("Utility class");
    }

    /**
     * Generates RatingValueContainer array from an XML node
     * @param ratingElement The XML rating node as an org.jdom.Element object
     * @return The generated RatingValueContainer array
     * @throws RatingException
     */
    static RatingValueContainer[] makeContainers(Element ratingElement, String pointsElementsName) throws RatingException {

        double[][] points = null;
        String[] notes = null;
        int width = -1;
        int depth = 0;
        //---------------------------------//
        // determine the shape of the data //
        //---------------------------------//
        List<?> pointsElems = ratingElement.getChildren(pointsElementsName);
        if (pointsElems.size() == 0) return null;
        for (Object pointsObj : pointsElems) {
            Element pointsElem = (Element)pointsObj;
            if (width == -1) {
                width = pointsElem.getChildren("other-ind").size(); // number of ind params - 1
            }
            if (pointsElem.getChildren("other-ind").size() != width) {
                throw new RatingException("Inconsistent number of independent parameters");
            }
            depth += pointsElem.getChildren("point").size();
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
        for (Object obj : pointsElems) {
            Element pointsElem = (Element)obj;
            int col = 0;
            for (Object otherIndObj : pointsElem.getChildren("other-ind")) {
                Element otherIndElem = (Element)otherIndObj;
                int pos = Integer.parseInt(otherIndElem.getAttributeValue("position"));
                if (pos != col+1) {
                    throw new RatingException(String.format("Expected position %d, got %d on %s", col+1, pos, otherIndElem.toString()));
                }
                otherInds[col++] = Double.parseDouble(otherIndElem.getAttributeValue("value"));
            }
            for (Object pointObj : pointsElem.getChildren("point")) {
                Element pointElem = (Element)pointObj;
                for (col = 0; col < width-2; ++col) points[row][col] = otherInds[col];
                points[row][width-2] = Double.parseDouble(pointElem.getChildTextTrim("ind"));
                points[row][width-1] = Double.parseDouble(pointElem.getChildTextTrim("dep"));
                note = pointElem.getChildTextTrim("note");
                if (note != null) {
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
