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

import static hec.lang.Const.UNDEFINED_TIME;


import org.opendcs.ratings.io.AbstractRatingContainer;
import org.opendcs.ratings.io.VirtualRatingContainer;
import org.opendcs.ratings.RatingException;
import hec.heclib.util.HecTime;
import hec.util.TextUtil;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import mil.army.usace.hec.metadata.VerticalDatumContainer;
import mil.army.usace.hec.metadata.VerticalDatumException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;

final class RatingXmlUtil {

    static boolean xmlParsingInitialized                    = false;
    private static DocumentBuilder builder					= null;
    static XPath xpath                                      = null;
    static XPathExpression officeIdXpath                    = null;
    static XPathExpression parametersIdXpath                = null;
    static XPathExpression versionXpath                     = null;
    static XPathExpression indParamsNodeXpath               = null;
    static XPathExpression indParamNodesXpath               = null;
    static XPathExpression indParamPosXpath                 = null;
    static XPathExpression parameterXpath                   = null;
    static XPathExpression inRangeMethodXpath               = null;
    static XPathExpression outRangeLowMethodXpath           = null;
    static XPathExpression outRangeHighMethodXpath          = null;
    static XPathExpression depParamXpath                    = null;
    static XPathExpression descriptionXpath                 = null;
    static XPathExpression ratingNodesXpath                 = null;
    static XPathExpression ratingSpecIdXpath                = null;
    static XPathExpression templateIdXpath                  = null;
    static XPathExpression locationIdXpath                  = null;
    static XPathExpression sourceAgencyXpath                = null;
    static XPathExpression autoUpdateXpath                  = null;
    static XPathExpression autoActivateXpath                = null;
    static XPathExpression autoMigrateExtXpath              = null;
    static XPathExpression indRoundingNodeXpath             = null;
    static XPathExpression indRoundingNodesXpath            = null;
    static XPathExpression depRoundingXpath                 = null;
    static XPathExpression unitsIdXpath                     = null;
    static XPathExpression effectiveDateXpath               = null;
    static XPathExpression createDateXpath                  = null;
    static XPathExpression activeXpath                      = null;
    static XPathExpression formulaXpath                     = null;
    static XPathExpression ratingPointGroupNodesXpath       = null;
    static XPathExpression extensionPointGroupNodesXpath    = null;
    static XPathExpression otherIndParamNodesXpath          = null;
    static XPathExpression otherIndValXPath                 = null;
    static XPathExpression pointNodesXpath                  = null;
    static XPathExpression indValXpath                      = null;
    static XPathExpression depValXpath                      = null;
    static XPathExpression noteXpath                        = null;
    static XPathExpression shiftNodesXpath                  = null;
    static XPathExpression offsetNodeXpath                  = null;

    private RatingXmlUtil() {
        throw new AssertionError("Utility class");
    }

    public static String getChildElementText(Element element, String tagName) {
        Element child = getChildElement(element, tagName);
        if (child == null) {
            return null;
        }
        return getElementText(child);
    }

    public static Element getChildElement(Element element, String tagName) {
        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) n;
                if (elem.getTagName().equals(tagName)) {
                    return elem;
                }
            }
        }
        return null;
    }

    public static List<Element> getChildElements(Element element, String tagName) {
        List<Element> elements = new ArrayList<>();
        boolean matchAll = tagName.equals("*");
        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) n;
                if (matchAll || elem.getTagName().equals(tagName)) {
                    elements.add(elem);
                }
            }
        }
        return elements;
    }

    public static String getElementText(Element element) {
        StringBuilder sb = new StringBuilder();

        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            short type = n.getNodeType();
            if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
                sb.append(n.getNodeValue());
            }
        }
        String text = sb.toString().trim();
        return text.isEmpty() ? null : text;
    }

    public static String getAttributeText(Element element, String name) {
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr a = (Attr) attrs.item(i);
            String qname = a.getName();
            String lname = a.getLocalName();
            if (qname.equals(name) || lname != null && lname.equals(name)) {
                return a.getValue();
            }
        }
        return "";
    }

    public static String elementToText(Element element) {
        Transformer t;
        try {
            t = TransformerFactory.newInstance().newTransformer();
            StringWriter sw = new StringWriter();
            t.transform(new DOMSource(element), new StreamResult(sw));
            return sw.toString();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

    }

    public static Element textToElement(String text) throws RatingException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);              // usually what you want
        Document doc;
        try {
            doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(text)));
        } catch (Exception e) {
            throw new RatingException(e);
        }
        return doc.getDocumentElement();
    }


    /**
     * Common code called from subclasses
     * @throws VerticalDatumException on error
     */
    static void populateCommonDataFromXml(Element ratingElement, AbstractRatingContainer arc) throws VerticalDatumException {
        HecTime hectime = new HecTime();
        String data;
        arc.officeId = getAttributeText(ratingElement, "office-id");
        arc.ratingSpecId = getChildElementText(ratingElement, "rating-spec-id");
        arc.unitsId = getChildElementText(ratingElement, "units-id");
        Element verticalDatumElement = getChildElement(ratingElement, "vertical-datum-info");
        if (verticalDatumElement != null) {
            arc.setVerticalDatumContainer(new VerticalDatumContainer(RatingXmlUtil.elementToText(verticalDatumElement)));
        }
        data = getChildElementText(ratingElement, "effective-date");
        if (data != null) {
            hectime.set(data);
            arc.effectiveDateMillis = hectime.getTimeInMillis();
        }
        data = getChildElementText(ratingElement, "create-date");
        if (data != null) {
            hectime.set(data);
            arc.createDateMillis = hectime.getTimeInMillis();
        }
        data = getChildElementText(ratingElement, "transition-start-date");
        if (data != null) {
            hectime.set(data);
            arc.transitionStartDateMillis = hectime.getTimeInMillis();
        }
        arc.active = Boolean.parseBoolean(getChildElementText(ratingElement, "active"));
        arc.description = getChildElementText(ratingElement, "description");
    }

    /**
     * Common code called from subclasses
     */
    static String toXml(AbstractRatingContainer ratingContainer, CharSequence prefix, CharSequence indent, String elementName) {
        HecTime hectime = new HecTime();
        StringBuilder sb = new StringBuilder();
        String officeId = ratingContainer.officeId;
        String ratingSpecId = ratingContainer.ratingSpecId;
        String unitsId = ratingContainer.unitsId;
        String description = ratingContainer.description;
        VerticalDatumContainer vdc = ratingContainer.getVerticalDatumContainer();
        long effectiveDateMillis = ratingContainer.effectiveDateMillis;
        long transitionStartDateMillis = ratingContainer.transitionStartDateMillis;
        long createDateMillis = ratingContainer.createDateMillis;
        boolean active = ratingContainer.active;
        sb.append(prefix).append("<").append(elementName).append(" office-id=\"").append(officeId == null ? "" : TextUtil.xmlEntityEncode(officeId)).append("\">\n");
        sb.append(prefix).append(indent).append("<rating-spec-id>").append(ratingSpecId == null ? "" : TextUtil.xmlEntityEncode(ratingSpecId)).append("</rating-spec-id>\n");
        if (vdc != null) {
            int level = indent.length() == 0 ? 0 : prefix.length() / indent.length();
            sb.append(vdc.toXml(indent, level+1));
        }
        if (!(ratingContainer instanceof VirtualRatingContainer)) {
            sb.append(prefix).append(indent).append("<units-id>").append(unitsId == null ? "" : TextUtil.xmlEntityEncode(unitsId)).append("</units-id>\n");
        }
        if (effectiveDateMillis == UNDEFINED_TIME) {
            sb.append(prefix).append(indent).append("<effective-date/>\n");
        }
        else {
            hectime.setTimeInMillis(effectiveDateMillis);
            sb.append(prefix).append(indent).append("<effective-date>").append(hectime.getXMLDateTime(0)).append("</effective-date>\n");
        }
        if (transitionStartDateMillis == UNDEFINED_TIME) {
            sb.append(prefix).append(indent).append("<transition-start-date/>\n");
        }
        else {
            hectime.setTimeInMillis(transitionStartDateMillis);
            sb.append(prefix).append(indent).append("<transition-start-date>").append(hectime.getXMLDateTime(0)).append("</transition-start-date>\n");
        }
        if (createDateMillis == UNDEFINED_TIME) {
            sb.append(prefix).append(indent).append("<create-date/>\n");
        }
        else {
            hectime.setTimeInMillis(createDateMillis);
            sb.append(prefix).append(indent).append("<create-date>").append(hectime.getXMLDateTime(0)).append("</create-date>\n");
        }
        sb.append(prefix).append(indent).append("<active>").append(active).append("</active>\n");
        if (description == null || description.isEmpty()) {
            sb.append(prefix).append(indent).append("<description/>\n");
        }
        else{
            sb.append(prefix).append(indent).append("<description>").append(TextUtil.xmlEntityEncode(description)).append("</description>\n");
        }
        return sb.toString();
    }

    /**
     * Initializes static variables for parsing CWMS-style ratings XML instances
     * @throws ParserConfigurationException on error
     * @throws XPathExpressionException on error
     */
    static void initXmlParsing() throws ParserConfigurationException, XPathExpressionException {
        if (!xmlParsingInitialized) {
            builder                       = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            xpath                         = XPathFactory.newInstance().newXPath();
            officeIdXpath                 = xpath.compile("./@office-id");
            parametersIdXpath             = xpath.compile("./parameters-id");
            versionXpath                  = xpath.compile("./version");
            indParamsNodeXpath            = xpath.compile("./ind-parameter-specs");
            indParamNodesXpath            = xpath.compile("./ind-parameter-spec");
            indParamPosXpath              = xpath.compile("./@position");
            parameterXpath                = xpath.compile("./parameter");
            inRangeMethodXpath            = xpath.compile("./in-range-method");
            outRangeLowMethodXpath        = xpath.compile("./out-range-low-method");
            outRangeHighMethodXpath       = xpath.compile("./out-range-high-method");
            depParamXpath                 = xpath.compile("./dep-parameter");
            descriptionXpath              = xpath.compile("./description");
            ratingNodesXpath              = xpath.compile("/ratings/rating|/ratings/usgs-stream-rating");
            ratingSpecIdXpath             = xpath.compile("./rating-spec-id");
            templateIdXpath               = xpath.compile("./template-id");
            locationIdXpath               = xpath.compile("./location-id");
            sourceAgencyXpath             = xpath.compile("./source-agency");
            autoUpdateXpath               = xpath.compile("./auto-update");
            autoActivateXpath             = xpath.compile("./auto-activate");
            autoMigrateExtXpath           = xpath.compile("./auto-migrate-extension");
            indRoundingNodeXpath          = xpath.compile("./ind-rounding-specs");
            indRoundingNodesXpath         = xpath.compile("./ind-rounding-spec");
            depRoundingXpath              = xpath.compile("./dep-rounding-spec");
            unitsIdXpath                  = xpath.compile("./units-id");
            effectiveDateXpath            = xpath.compile("./effective-date");
            createDateXpath               = xpath.compile("./create-date");
            activeXpath                   = xpath.compile("./active");
            formulaXpath                  = xpath.compile("./formula");
            ratingPointGroupNodesXpath    = xpath.compile("./rating-points");
            extensionPointGroupNodesXpath = xpath.compile("./extension-points");
            otherIndParamNodesXpath       = xpath.compile("./other-ind");
            otherIndValXPath              = xpath.compile("./@value");
            pointNodesXpath               = xpath.compile("./point");
            indValXpath                   = xpath.compile("./ind");
            depValXpath                   = xpath.compile("./dep");
            noteXpath                     = xpath.compile("./note");
            shiftNodesXpath               = xpath.compile("./height-shifts");
            offsetNodeXpath               = xpath.compile("./height-offsets");
            xmlParsingInitialized         = true;
        }
    }

    static synchronized Document readXmlAsDocument(String xml) throws IOException, SAXException
    {
        return builder.parse(new InputSource(new StringReader(xml)));
    }
}
