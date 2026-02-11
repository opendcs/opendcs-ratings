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

import hec.util.TextUtil;
import org.opendcs.ratings.*;
import org.opendcs.ratings.io.RatingSpecContainer;
import org.opendcs.ratings.io.RatingTemplateContainer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static hec.util.TextUtil.split;
import static org.opendcs.ratings.RatingConst.*;
import static org.opendcs.ratings.XmlUtil.*;

public final class RatingSpecXmlFactory {

    private RatingSpecXmlFactory() {
        throw new AssertionError("Utility class");
    }

    private static void logThrowable(Throwable t) {
        // still uses java.util.logging - should move to fluentLogger
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        AbstractRating.getLogger().severe(sw.toString());
    }
    /**
     * constructors a RatingSpecContainer from an XML snippet
     *
     * @param xmlText The XML snippet
     */
    public static RatingSpecContainer ratingSpecContainer(String xmlText) {
        RatingSpecContainer ratingSpecContainer = new RatingSpecContainer();
        String elementName = "rating-spec";
        RatingObjectDoesNotExistException noTemplateException = null;
        try {
            RatingTemplateContainer rtc = ratingTemplateContainer(xmlText);
            rtc.clone(ratingSpecContainer);
        } catch (RatingObjectDoesNotExistException e) {
            noTemplateException = e;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            BufferedReader br = new BufferedReader(new StringReader(xmlText));
            InputSource is = new InputSource(br);
            Document doc = db.parse(is);
            Element root = doc.getDocumentElement();
            if (root == null) {
                throw new RatingObjectDoesNotExistException("Cannot find root element in XML.");
            }
            String tagName = root.getTagName();
            if (tagName == null) {
                throw new RatingObjectDoesNotExistException("Cannot name of root element in XML.");
            }
            if (!tagName.equals(elementName)) {
                root = getChildElement(root, elementName);
            }
            if (!tagName.equals(elementName)) {
                throw new RatingObjectDoesNotExistException(String.format("No <%s> element in XML.", elementName));
            }
            Element elem;
            List<Element> elems;
            ratingSpecContainer.specOfficeId = getAttributeText(root, "office-id");
            elem = getChildElement(root, "rating-spec-id");
            if (elem != null) {
                ratingSpecContainer.specId = getElementText(elem);
            }
            elem = getChildElement(root, "template-id");
            if (elem != null) {
                ratingSpecContainer.templateId = getElementText(elem);
            }
            elem = getChildElement(root, "location-id");
            if (elem != null) {
                ratingSpecContainer.locationId = getElementText(elem);
            }
            elem = getChildElement(root, "version");
            if (elem != null) {
                ratingSpecContainer.specVersion = getElementText(elem);
            }
            elem = getChildElement(root, "source-agency");
            if (elem != null) {
                ratingSpecContainer.sourceAgencyId = getElementText(elem);
            }
            elem = getChildElement(root, "in-range-method");
            if (elem != null) {
                ratingSpecContainer.inRangeMethod = getElementText(elem);
            }
            elem = getChildElement(root, "out-range-low-method");
            if (elem != null) {
                ratingSpecContainer.outRangeLowMethod = getElementText(elem);
            }
            elem = getChildElement(root, "out-range-high-method");
            if (elem != null) {
                ratingSpecContainer.outRangeHighMethod = getElementText(elem);
            }
            elem = getChildElement(root, "active");
            if (elem != null) {
                ratingSpecContainer.active = Boolean.parseBoolean(getElementText(elem));
            }
            elem = getChildElement(root, "auto-update");
            if (elem != null) {
                ratingSpecContainer.autoUpdate = Boolean.parseBoolean(getElementText(elem));
            }
            elem = getChildElement(root, "active");
            if (elem != null) {
                ratingSpecContainer.active = Boolean.parseBoolean(getElementText(elem));
            }
            elem = getChildElement(root, "auto-update");
            if (elem != null) {
                ratingSpecContainer.autoUpdate = Boolean.parseBoolean(getElementText(elem));
            }
            elem = getChildElement(root, "auto-activate");
            if (elem != null) {
                ratingSpecContainer.autoActivate = Boolean.parseBoolean(getElementText(elem));
            }
            elem = getChildElement(root, "auto-migrate-extension");
            if (elem != null) {
                ratingSpecContainer.autoMigrateExtensions = Boolean.parseBoolean(getElementText(elem));
            }
            elem = getChildElement(root, "ind-rounding-specs");
            if (elem != null) {
                elems = getChildElements(elem,"ind-rounding-spec");
                int count = elems.size();
                ratingSpecContainer.indRoundingSpecs = new String[count];
                for (int i = 0; i < count; ++i) {
                    try {
                        elem = elems.get(i);
                        int j = Integer.parseInt(getAttributeText(elem,"position")) - 1;
                        if (j >= 0 && j < count) {
                            ratingSpecContainer.indRoundingSpecs[j] = getElementText(elem);
                        }
                    }
                    catch (Throwable t) {
                        logThrowable(t);
                    }
                }
            }
            elem = getChildElement(root, "dep-rounding-spec");
            if (elem != null) {
                ratingSpecContainer.depRoundingSpec = getElementText(elem);
            }
            elem = getChildElement(root, "description");
            if (elem != null) {
                ratingSpecContainer.specDescription = getElementText(elem);
            }
        }
        catch (Throwable t) {
            AbstractRating.getLogger().severe(t.getMessage());
            throw new RuntimeException(t);
        }
        if (noTemplateException != null) {
            AbstractRating.getLogger().finer(noTemplateException.getMessage());
        }
        return ratingSpecContainer;
    }

    /**
     * Generates an XML string (template and spec) from parameterized object
     *
     * @param ratingSpecContainer spec to transform into XML
     * @param indent              The amount to indent each level
     * @param level               The initial level of indentation
     * @return the generated XML
     */
    public static String toXml(RatingSpecContainer ratingSpecContainer, CharSequence indent, int level, boolean includeTemplate) {
        StringBuilder sb = new StringBuilder();
        int newLevel = level;
        if (level == 0) {
            newLevel = 1;
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            sb.append(
                "<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
        }
        if (includeTemplate) {
            sb.append(toXml(ratingSpecContainer, indent, newLevel));
        }
        sb.append(toSpecXml(ratingSpecContainer, indent, newLevel));
        if (level == 0) {
            sb.append("</ratings>\n");
        }
        return sb.toString();
    }

    /**
     * Generates a specification XML string from this object
     *
     * @param indent The amount to indent each level
     * @param level  The initial level of indentation
     * @return the generated XML
     */
    static String toSpecXml(RatingSpecContainer ratingSpecContainer, CharSequence indent, int level) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(indent).repeat(Math.max(0, level)));
        String prefix = sb.toString();
        sb.delete(0, sb.length());

        if (level == 0) {
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            sb.append(
                "<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
            prefix += indent;
        }
        String officeId = ratingSpecContainer.officeId;
        String specOfficeId = ratingSpecContainer.specOfficeId;
        String specId = ratingSpecContainer.specId;
        String templateId = ratingSpecContainer.templateId;
        String locationId = ratingSpecContainer.locationId;
        String specVersion = ratingSpecContainer.specVersion;
        String sourceAgencyId = ratingSpecContainer.sourceAgencyId;
        String inRangeMethod = ratingSpecContainer.inRangeMethod;
        String outRangeLowMethod = ratingSpecContainer.outRangeLowMethod;
        String outRangeHighMethod = ratingSpecContainer.outRangeHighMethod;
        boolean active = ratingSpecContainer.active;
        boolean autoUpdate = ratingSpecContainer.autoUpdate;
        boolean autoActivate = ratingSpecContainer.autoActivate;
        boolean autoMigrateExtensions = ratingSpecContainer.autoMigrateExtensions;
        String[] indRoundingSpecs = ratingSpecContainer.indRoundingSpecs;
        String depRoundingSpec = ratingSpecContainer.depRoundingSpec;
        String specDescription = ratingSpecContainer.specDescription;
        sb.append(prefix).append("<rating-spec office-id=\"").append(specOfficeId == null ? officeId == null ? "" : officeId : specOfficeId)
          .append("\">\n");
        sb.append(prefix).append(indent).append("<rating-spec-id>").append(specId == null ? "" : TextUtil.xmlEntityEncode(specId))
          .append("</rating-spec-id>\n");
        sb.append(prefix).append(indent).append("<template-id>").append(templateId == null ? "" : TextUtil.xmlEntityEncode(templateId))
          .append("</template-id>\n");
        sb.append(prefix).append(indent).append("<location-id>").append(locationId == null ? "" : TextUtil.xmlEntityEncode(locationId))
          .append("</location-id>\n");
        sb.append(prefix).append(indent).append("<version>").append(specVersion == null ? "" : TextUtil.xmlEntityEncode(specVersion))
          .append("</version>\n");
        sb.append(prefix).append(indent).append("<source-agency>").append(sourceAgencyId == null ? "" : TextUtil.xmlEntityEncode(sourceAgencyId))
          .append("</source-agency>\n");
        sb.append(prefix).append(indent).append("<in-range-method>").append(inRangeMethod == null ? "" : inRangeMethod)
          .append("</in-range-method>\n");
        sb.append(prefix).append(indent).append("<out-range-low-method>").append(outRangeLowMethod == null ? "" : outRangeLowMethod)
          .append("</out-range-low-method>\n");
        sb.append(prefix).append(indent).append("<out-range-high-method>").append(outRangeHighMethod == null ? "" : outRangeHighMethod)
          .append("</out-range-high-method>\n");
        sb.append(prefix).append(indent).append("<active>").append(active).append("</active>\n");
        sb.append(prefix).append(indent).append("<auto-update>").append(autoUpdate).append("</auto-update>\n");
        sb.append(prefix).append(indent).append("<auto-activate>").append(autoActivate).append("</auto-activate>\n");
        sb.append(prefix).append(indent).append("<auto-migrate-extension>").append(autoMigrateExtensions).append("</auto-migrate-extension>\n");
        sb.append(prefix).append(indent).append("<ind-rounding-specs>\n");
        if (indRoundingSpecs != null) {
            for (int i = 0; i < indRoundingSpecs.length; ++i) {
                sb.append(prefix).append(indent).append(indent).append("<ind-rounding-spec position=\"").append(i + 1).append("\">")
                  .append(indRoundingSpecs[i] == null ? "" : indRoundingSpecs[i]).append("</ind-rounding-spec>\n");
            }
        }
        sb.append(prefix).append(indent).append("</ind-rounding-specs>\n");
        sb.append(prefix).append(indent).append("<dep-rounding-spec>").append(depRoundingSpec == null ? "" : depRoundingSpec)
          .append("</dep-rounding-spec>\n");
        sb.append(prefix).append(indent).append("<description>").append(specDescription == null ? "" : TextUtil.xmlEntityEncode(specDescription))
          .append("</description>\n");
        sb.append(prefix).append("</rating-spec>\n");
        if (level == 0) {
            sb.append("</ratings>\n");
        }
        return sb.toString().replaceAll("<(.+?)></\\1>", "<$1/>");
    }

    /**
     * constructor from an XML snippet
     *
     * @param xmlStr The XML instance
     * @throws RatingObjectDoesNotExistException any errors processing the XML data.
     */
    public static RatingTemplateContainer ratingTemplateContainer(String xmlStr) throws RatingObjectDoesNotExistException {
        RatingTemplateContainer ratingTemplateContainer = new RatingTemplateContainer();
        String elementName = "rating-template";
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            BufferedReader br = new BufferedReader(new StringReader(xmlStr));
            InputSource is = new InputSource(br);
            Document doc = db.parse(is);
            Element root = doc.getDocumentElement();
            if (root == null) {
                throw new RatingObjectDoesNotExistException("Cannot find root element in XML.");
            }
            String tagName = root.getTagName();
            if (tagName == null) {
                throw new RatingObjectDoesNotExistException("Cannot name of root element in XML.");
            }
            if (!tagName.equals(elementName)) {
                root = getChildElement(root, elementName);
            }
            if (!tagName.equals(elementName)) {
                throw new RatingObjectDoesNotExistException(String.format("No <%s> element in XML.", elementName));
            }
            ratingTemplateContainer.officeId = getAttributeText(root, "office-id");
            Element elem;
            NodeList elems;
            elem = getChildElement(root, "version");
            if (elem != null) {
                ratingTemplateContainer.templateVersion = getElementText(elem);
            }
            elem = getChildElement(root, "ind-parameter-specs");
            if (elem != null) {
                elems = elem.getElementsByTagName("ind-parameter-spec");
                int count = elems.getLength();
                ratingTemplateContainer.indParams = new String[count];
                ratingTemplateContainer.inRangeMethods = new String[count];
                ratingTemplateContainer.outRangeLowMethods = new String[count];
                ratingTemplateContainer.outRangeHighMethods = new String[count];
                for (int i = 0; i < count; ++i) {
                    elem = (Element) elems.item(i);
                    try {
                        int j = Integer.parseInt(getAttributeText(elem, "position")) - 1;
                        if (j >= 0 && j < count) {
                            ratingTemplateContainer.indParams[i] = getChildElementText(elem, "parameter");
                            ratingTemplateContainer.inRangeMethods[i] = getChildElementText(elem, "in-range-method");
                            ratingTemplateContainer.outRangeLowMethods[i] = getChildElementText(elem, "out-range-low-method");
                            ratingTemplateContainer.outRangeHighMethods[i] = getChildElementText(elem, "out-range-high-method");
                        }
                    } catch (Throwable t) {
                        logThrowable(t);
                    }
                }
            }
            elem = getChildElement(root, "dep-parameter");
            if (elem != null) {
                ratingTemplateContainer.depParam = getElementText(elem);
            }
            elem = getChildElement(root, "description");
            if (elem != null) {
                ratingTemplateContainer.templateDescription = getElementText(elem);
            }
            if (ratingTemplateContainer.indParams != null && ratingTemplateContainer.indParams.length > 0 &&
                ratingTemplateContainer.depParam != null) {
                ratingTemplateContainer.parametersId =
                    String.format("%s%s%s", TextUtil.join(SEPARATOR3, ratingTemplateContainer.indParams), SEPARATOR2,
                        ratingTemplateContainer.depParam);
            }
            if (ratingTemplateContainer.parametersId != null && ratingTemplateContainer.templateVersion != null) {
                ratingTemplateContainer.templateId =
                    String.format("%s.%s", ratingTemplateContainer.parametersId, ratingTemplateContainer.templateVersion);
            }
        } catch (Throwable t) {
            AbstractRating.getLogger().severe(t.getMessage());
            throw new RuntimeException(t);
        }
        return ratingTemplateContainer;
    }

    /**
     * Generates a template XML string from parameterize container
     *
     * @param ratingTemplateContainer container to transform into XML
     * @param indent                  The amount to indent each level
     * @param level                   The initial level of indentation
     * @return xml representation of the template container
     */
    public static String toXml(RatingTemplateContainer ratingTemplateContainer, CharSequence indent, int level) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(indent).repeat(Math.max(0, level)));
        String prefix = sb.toString();
        sb.delete(0, sb.length());

        if (level == 0) {
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            sb.append(
                "<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
            prefix += indent;
        }
        String officeId = ratingTemplateContainer.officeId;
        String parametersId = ratingTemplateContainer.parametersId;
        String templateVersion = ratingTemplateContainer.templateVersion;
        String[] indParams = ratingTemplateContainer.indParams;
        String[] inRangeMethods = ratingTemplateContainer.inRangeMethods;
        String[] outRangeLowMethods = ratingTemplateContainer.outRangeLowMethods;
        String[] outRangeHighMethods = ratingTemplateContainer.outRangeHighMethods;
        String depParam = ratingTemplateContainer.depParam;
        String templateDescription = ratingTemplateContainer.templateDescription;
        sb.append(prefix).append("<rating-template office-id=\"").append(officeId == null ? "" : officeId).append("\">\n");
        sb.append(prefix).append(indent).append("<parameters-id>").append(parametersId == null ? "" : TextUtil.xmlEntityEncode(parametersId))
          .append("</parameters-id>\n");
        sb.append(prefix).append(indent).append("<version>").append(templateVersion == null ? "" : TextUtil.xmlEntityEncode(templateVersion))
          .append("</version>\n");
        sb.append(prefix).append(indent).append("<ind-parameter-specs>\n");
        if (indParams != null) {
            for (int i = 0; i < indParams.length; ++i) {
                String indParam = indParams[i] != null ? indParams[i] : "";
                String inRangeMethod = inRangeMethods != null && i < inRangeMethods.length && inRangeMethods[i] != null ? inRangeMethods[i] : "";
                String outRangeLowMethod =
                    outRangeLowMethods != null && i < outRangeLowMethods.length && outRangeLowMethods[i] != null ? outRangeLowMethods[i] : "";
                String outRangeHighMethod =
                    outRangeHighMethods != null && i < outRangeHighMethods.length && outRangeHighMethods[i] != null ? outRangeHighMethods[i] : "";
                sb.append(prefix).append(indent).append(indent).append("<ind-parameter-spec position=\"").append(i + 1).append("\">\n");
                sb.append(prefix).append(indent).append(indent).append(indent).append("<parameter>").append(TextUtil.xmlEntityEncode(indParam))
                  .append("</parameter>\n");
                sb.append(prefix).append(indent).append(indent).append(indent).append("<in-range-method>").append(inRangeMethod)
                  .append("</in-range-method>\n");
                sb.append(prefix).append(indent).append(indent).append(indent).append("<out-range-low-method>").append(outRangeLowMethod)
                  .append("</out-range-low-method>\n");
                sb.append(prefix).append(indent).append(indent).append(indent).append("<out-range-high-method>").append(outRangeHighMethod)
                  .append("</out-range-high-method>\n");
                sb.append(prefix).append(indent).append(indent).append("</ind-parameter-spec>\n");
            }
        }
        sb.append(prefix).append(indent).append("</ind-parameter-specs>\n");
        sb.append(prefix).append(indent).append("<dep-parameter>").append(depParam == null ? "" : TextUtil.xmlEntityEncode(depParam))
          .append("</dep-parameter>\n");
        sb.append(prefix).append(indent).append("<description>")
          .append(templateDescription == null ? "" : TextUtil.xmlEntityEncode(templateDescription)).append("</description>\n");
        sb.append(prefix).append("</rating-template>\n");
        if (level == 0) {
            sb.append("</ratings>\n");
        }
        return sb.toString();
    }

    /**
     * Generates an XML document fragment from this rating specification.
     *
     * @param ratingSpec rating template to serialize
     * @param indent     The character(s) for each level of indentation
     * @param level      The base indentation level for the document fragment
     * @return The XML document fragment
     */
    public static String toXml(RatingTemplate ratingSpec, CharSequence indent, int level) {
        return toXml(ratingSpec.getData(), indent, level);
    }

    /**
     * Generates an XML document fragment from this rating specification.
     *
     * @param ratingSpec rating specification to serialize
     * @param indent     The character(s) for each level of indentation
     * @param level      The base indentation level for the document fragment
     * @return The XML document fragment
     */
    public static String toXml(RatingSpec ratingSpec, CharSequence indent, int level) {
        return toXml(ratingSpec.getData(), indent, level);
    }

    /**
     * Creates the data from this object from an XML instance
     *
     * @param xmlText The XML instance
     * @throws RatingException any errors processing the XML data.
     */
    public static RatingTemplate ratingTemplate(String xmlText) throws RatingException {
        RatingTemplateContainer container = ratingTemplateContainer(xmlText);
        return new RatingTemplate(container);
    }

    /**
     * constructs a RatingSpec from XML strings
     *
     * @param templateXml The template XML text
     * @param specXml     The specification XML text
     * @throws RatingException On error
     */
    public static RatingSpec ratingSpec(String templateXml, String specXml) throws RatingException {
        if (templateXml == null) {
            throw new RatingException("The rating template XML string is null");
        }
        if (specXml == null) {
            throw new RatingException("The rating specification XML string is null");
        }
        try {
            RatingXmlUtil.initXmlParsing();
            Document templateDoc = RatingXmlUtil.readXmlAsDocument(templateXml);
            Document specDoc = RatingXmlUtil.readXmlAsDocument(specXml);
            return ratingSpec(templateDoc.getDocumentElement().getElementsByTagName("rating-template").item(0),
                specDoc.getDocumentElement().getElementsByTagName("rating-spec").item(0));
        } catch (Exception e) {
            throw new RatingException(e);
        }
    }

    static RatingSpec ratingSpec(Node templateNode, Node specNode) throws RatingException {
        try {
            if (templateNode == null) {
                throw new RatingException("The rating template node string is null");
            }
            if (specNode == null) {
                throw new RatingException("The rating specification node string is null");
            }
            //-------------------------//
            // parse the template node //
            //-------------------------//
            RatingXmlUtil.initXmlParsing();
            String parametersId = (String) RatingXmlUtil.parametersIdXpath.evaluate(templateNode, XPathConstants.STRING);
            String officeId = (String) RatingXmlUtil.officeIdXpath.evaluate(templateNode, XPathConstants.STRING);
            String[] paramIds = split(parametersId, SEPARATOR2, "L");
            if (paramIds.length != 2) {
                throw new RatingException(String.format("Rating template has invalid parameters identifier: %s", parametersId));
            }
            String depParam = paramIds[1];
            paramIds = split(paramIds[0], SEPARATOR3, "L");
            String templateVersion = (String) RatingXmlUtil.versionXpath.evaluate(templateNode, XPathConstants.STRING);
            Node indParamsNode = (Node) RatingXmlUtil.indParamsNodeXpath.evaluate(templateNode, XPathConstants.NODE);
            NodeList indParamNodes = (NodeList) RatingXmlUtil.indParamNodesXpath.evaluate(indParamsNode, XPathConstants.NODESET);
            int indParamCount = indParamNodes.getLength();
            if (indParamCount != paramIds.length) {
                throw new RatingException("Rating template has inconsistent numbers of independent parameters.");
            }
            RatingConst.RatingMethod[] inRangeMethods = new RatingConst.RatingMethod[indParamCount];
            RatingConst.RatingMethod[] outRangeLowMethods = new RatingConst.RatingMethod[indParamCount];
            RatingConst.RatingMethod[] outRangeHighMethods = new RatingConst.RatingMethod[indParamCount];
            for (int i = 0; i < indParamCount; ++i) {
                Node indParamNode = indParamNodes.item(i);
                double pos = (Double) RatingXmlUtil.indParamPosXpath.evaluate(indParamNode, XPathConstants.NUMBER);
                if (pos != i + 1) {
                    throw new RatingException("Parameters out of order in rating template.");
                }
                if (!paramIds[i].equals(RatingXmlUtil.parameterXpath.evaluate(indParamNode, XPathConstants.STRING))) {
                    throw new RatingException(String.format("Rating template has inconsistent independent parameter %d.", (i + 1)));
                }
                inRangeMethods[i] =
                    RatingConst.RatingMethod.fromString((String) RatingXmlUtil.inRangeMethodXpath.evaluate(indParamNode, XPathConstants.STRING));
                outRangeLowMethods[i] =
                    RatingConst.RatingMethod.fromString((String) RatingXmlUtil.outRangeLowMethodXpath.evaluate(indParamNode, XPathConstants.STRING));
                outRangeHighMethods[i] =
                    RatingConst.RatingMethod.fromString((String) RatingXmlUtil.outRangeHighMethodXpath.evaluate(indParamNode, XPathConstants.STRING));
            }
            if (!depParam.equals(RatingXmlUtil.depParamXpath.evaluate(templateNode, XPathConstants.STRING))) {
                throw new RatingException("Rating template has inconsistent dependent parameter.");
            }
            String templateDescription = (String) RatingXmlUtil.descriptionXpath.evaluate(templateNode, XPathConstants.STRING);
            //----------------------------//
            // parse the rating spec node //
            //----------------------------//
            if (!officeId.equals(RatingXmlUtil.officeIdXpath.evaluate(specNode, XPathConstants.STRING))) {
                throw new RatingException("Rating template and specification have different office identifiers.");
            }
            String ratingSpecId = (String) RatingXmlUtil.ratingSpecIdXpath.evaluate(specNode, XPathConstants.STRING);
            String templateId = (String) RatingXmlUtil.templateIdXpath.evaluate(specNode, XPathConstants.STRING);
            String locationId = (String) RatingXmlUtil.locationIdXpath.evaluate(specNode, XPathConstants.STRING);
            String[] parts = split(ratingSpecId, SEPARATOR1, "L");
            if (parts.length != 4) {
                throw new RatingException("Rating specification has invalid identifier");
            }
            if (!parts[0].equals(locationId)) {
                throw new RatingException("Rating template and specification have different locations.");
            }
            if (!templateId.equals(String.format("%s%s%s", parametersId, SEPARATOR1, templateVersion))) {
                throw new RatingException("Rating template and specification have inconsistent identifiers.");
            }
            String sourceAgencyId = (String) RatingXmlUtil.sourceAgencyXpath.evaluate(specNode, XPathConstants.STRING);
            String inRangeMethod = (String) RatingXmlUtil.inRangeMethodXpath.evaluate(specNode, XPathConstants.STRING);
            String outRangeLowMethod = (String) RatingXmlUtil.outRangeLowMethodXpath.evaluate(specNode, XPathConstants.STRING);
            String outRangeHighMethod = (String) RatingXmlUtil.outRangeHighMethodXpath.evaluate(specNode, XPathConstants.STRING);
            String activeStr = (String) RatingXmlUtil.activeXpath.evaluate(specNode, XPathConstants.STRING);
            String autoUpdateStr = (String) RatingXmlUtil.autoUpdateXpath.evaluate(specNode, XPathConstants.STRING);
            String autoActivateStr = (String) RatingXmlUtil.autoActivateXpath.evaluate(specNode, XPathConstants.STRING);
            String autoMigrateExtensionsStr = (String) RatingXmlUtil.autoMigrateExtXpath.evaluate(specNode, XPathConstants.STRING);
            boolean active = activeStr.equals("true");
            boolean autoUpdate = autoUpdateStr.equals("true");
            boolean autoActivate = autoActivateStr.equals("true");
            boolean autoMigrateExtensions = autoMigrateExtensionsStr.equals("true");
            indParamsNode = (Node) RatingXmlUtil.indRoundingNodeXpath.evaluate(specNode, XPathConstants.NODE);
            indParamNodes = (NodeList) RatingXmlUtil.indRoundingNodesXpath.evaluate(indParamsNode, XPathConstants.NODESET);
            indParamCount = indParamNodes.getLength();
            if (indParamCount != paramIds.length) {
                throw new RatingException("Rating specification has different numbers of independent parameters than rating template.");
            }
            String[] indRoundingSpecs = new String[indParamCount];
            for (int i = 0; i < indParamCount; ++i) {
                Node indParamNode = indParamNodes.item(i);
                double pos = (Double) RatingXmlUtil.indParamPosXpath.evaluate(indParamNode, XPathConstants.NUMBER);
                if (pos != i + 1) {
                    throw new RatingException("Parameters out of order in rating specification.");
                }
                indRoundingSpecs[i] = indParamNode.getFirstChild().getNodeValue().trim();
            }
            String depRoundingSpec = (String) RatingXmlUtil.depRoundingXpath.evaluate(specNode, XPathConstants.STRING);
            String specDescription = (String) RatingXmlUtil.descriptionXpath.evaluate(specNode, XPathConstants.STRING);
            RatingSpec ratingSpec = new RatingSpec();
            ratingSpec.setOfficeId(officeId);
            parts = split(ratingSpecId, SEPARATOR1, "L");
            if (parts.length != 4) {
                throw new RatingException("Invalid rating specification: " + ratingSpecId);
            }
            ratingSpec.setLocationId(parts[0]);
            ratingSpec.setParametersId(parts[1]);
            ratingSpec.setVersion(parts[2]);
            ratingSpec.setVersion(parts[3]);
            ratingSpec.setSourceAgencyId(sourceAgencyId);
            ratingSpec.setDescription(specDescription);
            ratingSpec.setInRangeMethod(RatingConst.RatingMethod.fromString(inRangeMethod));
            ratingSpec.setOutRangeLowMethod(RatingConst.RatingMethod.fromString(outRangeLowMethod));
            ratingSpec.setOutRangeHighMethod(RatingConst.RatingMethod.fromString(outRangeHighMethod));
            ratingSpec.setActive(active);
            ratingSpec.setAutoUpdate(autoUpdate);
            ratingSpec.setAutoActivate(autoActivate);
            ratingSpec.setAutoMigrateExtensions(autoMigrateExtensions);
            ratingSpec.setIndRoundingSpecs(indRoundingSpecs);
            ratingSpec.setDepRoundingSpec(depRoundingSpec);
            ratingSpec.setInRangeMethods(inRangeMethods);
            ratingSpec.setOutRangeLowMethods(outRangeLowMethods);
            ratingSpec.setOutRangeHighMethods(outRangeHighMethods);
            ratingSpec.setTemplateId(templateId);
            ratingSpec.setTemplateDescription(templateDescription);
            return ratingSpec;
        } catch (Throwable t) {
            logThrowable(t);
            if (t instanceof RatingException) {
                throw (RatingException) t;
            }
            throw new RatingException(t);
        }
    }

    /**
     * Generates an XML document fragment from this rating specification.
     *
     * @param ratingSpec      object to serialize
     * @param indent          The character(s) for each level of indentation
     * @param level           The base indentation level for the document fragment
     * @param includeTemplate include the serialization of the rating template in the resulting xml
     * @return The XML document fragment
     */
    public static String toXml(RatingSpec ratingSpec, CharSequence indent, int level, boolean includeTemplate) {
        return toXml(ratingSpec.getData(), indent, level, includeTemplate);
    }
}
