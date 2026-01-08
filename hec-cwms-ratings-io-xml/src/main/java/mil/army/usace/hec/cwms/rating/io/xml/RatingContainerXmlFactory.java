/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.xml;

import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.USGS_OFFSETS_SPEC_VERSION;
import static hec.data.cwmsRating.RatingConst.USGS_OFFSETS_SUBPARAM;
import static hec.data.cwmsRating.RatingConst.USGS_OFFSETS_TEMPLATE_VERSION;
import static hec.data.cwmsRating.RatingConst.USGS_SHIFTS_SPEC_VERSION;
import static hec.data.cwmsRating.RatingConst.USGS_SHIFTS_SUBPARAM;
import static hec.data.cwmsRating.RatingConst.USGS_SHIFTS_TEMPLATE_VERSION;
import static hec.lang.Const.UNDEFINED_TIME;


import hec.data.RatingObjectDoesNotExistException;
import hec.data.RatingRuntimeException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.RatingMethodId;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.ExpressionRatingContainer;
import hec.data.cwmsRating.io.RatingSetContainer;
import hec.data.cwmsRating.io.RatingSpecContainer;
import hec.data.cwmsRating.io.RatingValueContainer;
import hec.data.cwmsRating.io.SourceRatingContainer;
import hec.data.cwmsRating.io.TableRatingContainer;
import hec.data.cwmsRating.io.TransitionalRatingContainer;
import hec.data.cwmsRating.io.UsgsStreamTableRatingContainer;
import hec.data.cwmsRating.io.VirtualRatingContainer;
import hec.heclib.util.HecTime;
import hec.util.TextUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import mil.army.usace.hec.metadata.VerticalDatumContainer;
import mil.army.usace.hec.metadata.VerticalDatumException;
import org.jdom.Element;

public final class RatingContainerXmlFactory {

    private RatingContainerXmlFactory() {
        throw new AssertionError("Factory utility class");
    }

    static AbstractRatingContainer abstractRatingContainer(String xmlStr) throws RatingException {

        AbstractRatingContainer arc = null;
        //----------------------------//
        // first try the root element //
        //----------------------------//
        Element elem = RatingXmlUtil.textToJdomElement(xmlStr);
        if (elem.getName().equals("simple-rating") || elem.getName().equals("rating")) {
            if (elem.getChild("formula") != null) {
                arc = expressionRatingContainer(elem);
            } else {
                arc = tableRatingContainer(elem);
            }
        } else if (elem.getName().equals("usgs-stream-rating")) {
            arc = usgsStreamTableRatingContainer(elem);
        } else if (elem.getName().equals("virtual-rating")) {
            arc = virtualRatingContainer(elem);
        } else if (elem.getName().equals("transitional-rating")) {
            arc = transitionalRatingContainer(elem);
        } else {
            //------------------------------------------//
            // next try immediate descendants from root //
            //------------------------------------------//
            for (Object obj : elem.getChildren()) {
                elem = (Element) obj;
                if (elem.getName().equals("simple-rating") || elem.getName().equals("rating")) {
                    if (elem.getChild("formula") != null) {
                        arc = expressionRatingContainer(elem);
                        break;
                    } else {
                        arc = tableRatingContainer(elem);
                        break;
                    }
                } else if (elem.getName().equals("usgs-stream-rating")) {
                    arc = usgsStreamTableRatingContainer(elem);
                    break;
                } else if (elem.getName().equals("virtual-rating")) {
                    arc = virtualRatingContainer(elem);
                    break;
                } else if (elem.getName().equals("transitional-rating")) {
                    arc = transitionalRatingContainer(elem);
                    break;
                }
            }
            if (arc == null) {
                throw new RatingObjectDoesNotExistException(
                    "No <rating>, <simple-rating>, <usgs-stream-rating>, <virtual-rating>, or <transitional-rating> element in XML.");
            }
        }
        return arc;
    }

    /**
     * Generates an XML string from this object. The subclass overrides should normally be called instead of this.
     *
     * @param abstractRatingContainer container to serialize
     * @param indent                  The amount to indent each level
     * @param level                   The initial level of indentation
     * @return the generated XML
     */
    public static String toXml(AbstractRatingContainer abstractRatingContainer, CharSequence indent, int level) {
        if (abstractRatingContainer instanceof UsgsStreamTableRatingContainer) {
            return toXml((UsgsStreamTableRatingContainer) abstractRatingContainer, indent, level);
        } else if (abstractRatingContainer instanceof TableRatingContainer) {
            return toXml((TableRatingContainer) abstractRatingContainer, indent, level);
        } else if (abstractRatingContainer instanceof ExpressionRatingContainer) {
            return toXml((ExpressionRatingContainer) abstractRatingContainer, indent, level);
        } else if (abstractRatingContainer instanceof VirtualRatingContainer) {
            return toXml((VirtualRatingContainer) abstractRatingContainer, indent, level);
        } else if (abstractRatingContainer instanceof TransitionalRatingContainer) {
            return toXml((TransitionalRatingContainer) abstractRatingContainer, indent, level);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < level; ++i) {
                sb.append(indent);
            }
            String prefix = sb.toString();
            sb.delete(0, sb.length());
            if (level == 0) {
                sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                sb.append(
                    "<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
                prefix += indent;
            }
            sb.append(RatingXmlUtil.toXml(abstractRatingContainer, prefix, indent, "rating")).append(prefix).append("</rating>\n");
            if (level == 0) {
                sb.append("</ratings>\n");
            }
            return sb.toString();
        }
    }

    /**
     * Factory constructor from a jdom element
     *
     * @param ratingElement the jdom element
     * @return container object represented by jdom input
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static ExpressionRatingContainer expressionRatingContainer(Element ratingElement) throws RatingException {
        try {
            ExpressionRatingContainer container = new ExpressionRatingContainer();
            RatingXmlUtil.populateCommonDataFromXml(ratingElement, container);
            container.expression = ratingElement.getChildTextTrim("formula");
            return container;
        } catch (VerticalDatumException e) {
            throw new RatingException(e);
        }
    }

    /**
     * Factory constructor from an XML snippet
     *
     * @param xmlText the XML snippet
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static ExpressionRatingContainer expressionRatingContainer(String xmlText) throws RatingException {
        AbstractRatingContainer arc = abstractRatingContainer(xmlText);
        if (arc instanceof ExpressionRatingContainer) {
            return (ExpressionRatingContainer) arc;
        } else {
            throw new RatingException("XML text does not specify an ExpressionRating object.");
        }
    }

    /**
     * Generates an XML string from this object. The subclass overrides should normally be called instead of this.
     *
     * @param expressionRatingContainer container to serialize
     * @param indent                    The amount to indent each level
     * @param level                     The initial level of indentation
     * @return the generated XML
     */
    public static String toXml(ExpressionRatingContainer expressionRatingContainer, CharSequence indent, int level) {
        VerticalDatumContainer vdc = expressionRatingContainer.getVerticalDatumContainer();
        try {
            if (vdc != null && vdc.getCurrentOffset() != 0.) {
                ExpressionRatingContainer clone = new ExpressionRatingContainer();
                expressionRatingContainer.clone(clone);
                clone.toNativeVerticalDatum();
                return toXml(clone, indent, level);
            }
        } catch (VerticalDatumException e) {
            throw new RatingRuntimeException(e);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; ++i) {
            sb.append(indent);
        }
        String prefix = sb.toString();
        sb.delete(0, sb.length());
        if (level == 0) {
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            sb.append(
                "<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
            prefix += indent;
        }
        sb.append(RatingXmlUtil.toXml(expressionRatingContainer, prefix, indent, "simple-rating"));
        String expression = expressionRatingContainer.expression.replaceAll("([Aa][Rr][Gg]|\\$)(\\d)", "I$2");
        sb.append(prefix).append(indent).append("<formula>").append(TextUtil.xmlEntityEncode(expression)).append("</formula>\n");
        sb.append(prefix).append("</simple-rating>\n");
        if (level == 0) {
            sb.append("</ratings>\n");
        }
        return sb.toString();
    }

    /**
     * Factory constructor from a jdom element
     *
     * @param ratingElement the jdom element
     * @return container object represented by jdom input
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static VirtualRatingContainer virtualRatingContainer(Element ratingElement) throws RatingException {
        try {
            VirtualRatingContainer virtualRatingContainer = new VirtualRatingContainer();
            RatingXmlUtil.populateCommonDataFromXml(ratingElement, virtualRatingContainer);
            return virtualRatingContainer;
        } catch (VerticalDatumException e) {
            throw new RatingException(e);
        }
    }

    /**
     * Generates an XML string from this object. The subclass overrides should normally be called instead of this.
     *
     * @param virtualRatingContainer container to serialize
     * @param indent                 The amount to indent each level
     * @param level                  The initial level of indentation
     * @return the generated XML
     */
    public static String toXml(VirtualRatingContainer virtualRatingContainer, CharSequence indent, int level) {
        VerticalDatumContainer vdc = virtualRatingContainer.getVerticalDatumContainer();
        try {
            if (vdc != null && vdc.getCurrentOffset() != 0.) {
                VirtualRatingContainer clone = (VirtualRatingContainer) virtualRatingContainer.clone();
                clone.toNativeVerticalDatum();
                return toXml(clone, indent, level);
            }
        } catch (VerticalDatumException e) {
            throw new RatingRuntimeException(e);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; ++i) {
            sb.append(indent);
        }
        String prefix = sb.toString();
        sb.delete(0, sb.length());
        List<String> ratingXmlStrings = new ArrayList<>();
        if (level == 0) {
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            sb.append(
                "<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
            SortedSet<String> templateXmlStrings = new TreeSet<>();
            SortedSet<String> specXmlStrings = new TreeSet<>();
            getSourceRatingsXml(virtualRatingContainer, indent, level + 1, templateXmlStrings, specXmlStrings, ratingXmlStrings);
            for (String templateXml : templateXmlStrings) {
                sb.append(templateXml);
            }
            for (String specXml : specXmlStrings) {
                sb.append(specXml);
            }
            prefix += indent;
        }
        sb.append(RatingXmlUtil.toXml(virtualRatingContainer, prefix, indent, "virtual-rating"));
        String connections = virtualRatingContainer.connections;
        SourceRatingContainer[] sourceRatings = virtualRatingContainer.sourceRatings;
        if (connections == null || connections.length() == 0) {
            sb.append(prefix).append(indent).append("<connections/>\n");
        } else {
            sb.append(prefix).append(indent).append("<connections>").append(connections).append("</connections>\n");
        }
        if (sourceRatings == null || sourceRatings.length == 0) {
            sb.append(prefix).append(indent).append("<source-ratings/>\n");
        } else {
            sb.append(prefix).append(indent).append("<source-ratings>\n");
            StringBuilder sourceRatingUnits = new StringBuilder();
            for (int i = 0; i < sourceRatings.length; ++i) {
                sourceRatingUnits.setLength(0);
                sourceRatingUnits.append(" {");
                for (int j = 0; j < sourceRatings[i].units.length; ++j) {
                    sourceRatingUnits.append(j == 0 ? "" : j == sourceRatings[i].units.length - 1 ? ";" : ",")
                                     .append(TextUtil.xmlEntityEncode(sourceRatings[i].units[j]));
                }
                sourceRatingUnits.append("}");
                sb.append(prefix).append(indent).append(indent).append("<source-rating position=\"" + (i + 1) + "\">\n");
                if (sourceRatings[i].rsc == null) {
                    sb.append(prefix).append(indent).append(indent).append(indent).append("<rating-expression>")
                      .append(TextUtil.xmlEntityEncode(sourceRatings[i].mathExpression)).append(sourceRatingUnits).append("</rating-expression>\n");
                } else {
                    sb.append(prefix).append(indent).append(indent).append(indent).append("<rating-spec-id>")
                      .append(TextUtil.xmlEntityEncode(sourceRatings[i].rsc.ratingSpecContainer.specId)).append(sourceRatingUnits)
                      .append("</rating-spec-id>\n");
                }
                sb.append(prefix).append(indent).append(indent).append("</source-rating>\n");
            }
            sb.append(prefix).append(indent).append("</source-ratings>\n");
        }
        sb.append(prefix).append("</virtual-rating>\n");
        if (!ratingXmlStrings.isEmpty()) {
            for (String ratingXml : ratingXmlStrings) {
                sb.append(ratingXml);
            }
        }
        if (level == 0) {
            sb.append("</ratings>\n");
        }
        return sb.toString();
    }

    /**
     * Factory constructor from an XML snippet
     *
     * @param xmlText the XML snippet
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static VirtualRatingContainer virtualRatingContainer(String xmlText) throws RatingException {
        RatingSetContainer rsc = RatingSetContainerXmlFactory.ratingSetContainerFromXml(xmlText);
        if (rsc.abstractRatingContainers.length == 1 && rsc.abstractRatingContainers[0] instanceof VirtualRatingContainer) {
            return (VirtualRatingContainer) rsc.abstractRatingContainers[0];
        } else {
            throw new RatingException("XML text does not specify an VirtualRating object.");
        }
    }

    /**
     * Factory constructor from a jdom element
     *
     * @param ratingElement the jdom element
     * @return container object represented by jdom input
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static UsgsStreamTableRatingContainer usgsStreamTableRatingContainer(Element ratingElement) throws RatingException {
        UsgsStreamTableRatingContainer usgsStreamTableRatingContainer = new UsgsStreamTableRatingContainer();
        try {
            RatingXmlUtil.populateCommonDataFromXml(ratingElement, usgsStreamTableRatingContainer);
        } catch (VerticalDatumException e1) {
            throw new RatingException(e1);
        }
        String unitsId = usgsStreamTableRatingContainer.unitsId;
        String ratingSpecId = usgsStreamTableRatingContainer.ratingSpecId;
        Element elem = null;
        String[] parts;
        @SuppressWarnings("rawtypes") List elems = ratingElement.getChildren("height-shifts");
        String heightUnit = unitsId.split(";", 'L')[0];
        if (elems.size() > 0) {
            HecTime hectime = new HecTime();
            usgsStreamTableRatingContainer.shifts = new RatingSetContainer();
            usgsStreamTableRatingContainer.shifts.ratingSpecContainer = new RatingSpecContainer();
            RatingSpecContainer rsc = usgsStreamTableRatingContainer.shifts.ratingSpecContainer;
            rsc.inRangeMethod = RatingMethodId.Linear.name();
            rsc.outRangeLowMethod = RatingMethodId.Nearest.name();
            rsc.outRangeHighMethod = RatingMethodId.Nearest.name();
            rsc.inRangeMethods = new String[] {RatingMethodId.Linear.name()};
            rsc.outRangeLowMethods = new String[] {RatingMethodId.Nearest.name()};
            rsc.outRangeHighMethods = new String[] {RatingMethodId.Nearest.name()};
            parts = TextUtil.split(ratingSpecId, SEPARATOR1);
            rsc.locationId = parts[0];
            rsc.templateVersion = USGS_SHIFTS_TEMPLATE_VERSION;
            rsc.specVersion = USGS_SHIFTS_SPEC_VERSION;
            parts = TextUtil.split(parts[1], SEPARATOR2);
            rsc.indParams = new String[] {parts[0]};
            rsc.depParam = String.format("%s-%s", parts[0], USGS_SHIFTS_SUBPARAM);
            rsc.parametersId = TextUtil.join(SEPARATOR2, rsc.indParams[0], rsc.depParam);
            rsc.templateId = TextUtil.join(SEPARATOR1, rsc.parametersId, rsc.templateVersion);
            rsc.specId = TextUtil.join(SEPARATOR1, rsc.locationId, rsc.templateId, rsc.specVersion);
            rsc.indRoundingSpecs = new String[] {"4444444449"};
            rsc.depRoundingSpec = "4444444449";

            usgsStreamTableRatingContainer.shifts.abstractRatingContainers = new TableRatingContainer[elems.size()];
            for (int i = 0; i < elems.size(); ++i) {
                usgsStreamTableRatingContainer.shifts.abstractRatingContainers[i] = new TableRatingContainer();
                TableRatingContainer trc = (TableRatingContainer) usgsStreamTableRatingContainer.shifts.abstractRatingContainers[i];
                trc.ratingSpecId = rsc.specId;
                elem = (Element) elems.get(i);
                String data = elem.getChildTextTrim("effective-date");
                if (data != null && !data.isEmpty()) {
                    hectime.set(data);
                    trc.effectiveDateMillis = hectime.getTimeInMillis();
                }
                data = elem.getChildTextTrim("create-date");
                if (data != null && !data.isEmpty()) {
                    hectime.set(data);
                    trc.createDateMillis = hectime.getTimeInMillis();
                }
                data = elem.getChildTextTrim("transition-start-date");
                if (data != null && !data.isEmpty()) {
                    hectime.set(data);
                    trc.transitionStartDateMillis = hectime.getTimeInMillis();
                }
                trc.active = Boolean.parseBoolean(elem.getChildTextTrim("active"));
                trc.description = elem.getChildTextTrim("description");
                trc.unitsId = String.format("%s;%s", heightUnit, heightUnit);
                trc.inRangeMethod = RatingMethodId.Linear.name();
                trc.outRangeLowMethod = RatingMethodId.Nearest.name();
                trc.outRangeHighMethod = RatingMethodId.Nearest.name();
                @SuppressWarnings("rawtypes") List pointElems = elem.getChildren("point");
                if (pointElems.size() > 0) {
                    trc.values = new RatingValueContainer[pointElems.size()];
                    for (int j = 0; j < pointElems.size(); ++j) {
                        trc.values[j] = new RatingValueContainer();
                        elem = (Element) pointElems.get(j);
                        trc.values[j].indValue = Double.parseDouble(elem.getChildTextTrim("ind"));
                        trc.values[j].depValue = Double.parseDouble(elem.getChildTextTrim("dep"));
                        trc.values[j].note = elem.getChildTextTrim("note");
                    }
                }
            }
        }
        elem = ratingElement.getChild("height-offsets");
        if (elem != null) {
            usgsStreamTableRatingContainer.offsets = new TableRatingContainer();
            TableRatingContainer trc = usgsStreamTableRatingContainer.offsets;
            parts = TextUtil.split(ratingSpecId, SEPARATOR1);
            String indParamId = TextUtil.split(parts[1], SEPARATOR2)[0];
            trc.ratingSpecId = TextUtil.join(SEPARATOR1, parts[0], TextUtil.join(SEPARATOR2, indParamId, indParamId + "-" + USGS_OFFSETS_SUBPARAM),
                USGS_OFFSETS_TEMPLATE_VERSION, USGS_OFFSETS_SPEC_VERSION);
            trc.unitsId = String.format("%s;%s", heightUnit, heightUnit);
            trc.inRangeMethod = RatingMethodId.Previous.name();
            trc.outRangeLowMethod = RatingMethodId.Next.name();
            trc.outRangeHighMethod = RatingMethodId.Previous.name();
            @SuppressWarnings("rawtypes") List pointElems = elem.getChildren("point");
            if (pointElems.size() > 0) {
                trc.values = new RatingValueContainer[pointElems.size()];
                for (int i = 0; i < pointElems.size(); ++i) {
                    trc.values[i] = new RatingValueContainer();
                    elem = (Element) pointElems.get(i);
                    trc.values[i].indValue = Double.parseDouble(elem.getChildTextTrim("ind"));
                    trc.values[i].depValue = Double.parseDouble(elem.getChildTextTrim("dep"));
                    trc.values[i].note = elem.getChildTextTrim("note");
                }
            }
        }
        elem = ratingElement.getChild("rating-points");
        if (elem != null) {
            @SuppressWarnings("rawtypes") List pointElems = elem.getChildren("point");
            if (pointElems.size() > 0) {
                usgsStreamTableRatingContainer.values = new RatingValueContainer[pointElems.size()];
                for (int i = 0; i < pointElems.size(); ++i) {
                    usgsStreamTableRatingContainer.values[i] = new RatingValueContainer();
                    elem = (Element) pointElems.get(i);
                    usgsStreamTableRatingContainer.values[i].indValue = Double.parseDouble(elem.getChildTextTrim("ind"));
                    usgsStreamTableRatingContainer.values[i].depValue = Double.parseDouble(elem.getChildTextTrim("dep"));
                    usgsStreamTableRatingContainer.values[i].note = elem.getChildTextTrim("note");
                }
            }
        }
        elem = ratingElement.getChild("extension-points");
        if (elem != null) {
            @SuppressWarnings("rawtypes") List pointElems = elem.getChildren("point");
            if (pointElems.size() > 0) {
                usgsStreamTableRatingContainer.extensionValues = new RatingValueContainer[pointElems.size()];
                for (int i = 0; i < pointElems.size(); ++i) {
                    usgsStreamTableRatingContainer.extensionValues[i] = new RatingValueContainer();
                    elem = (Element) pointElems.get(i);
                    usgsStreamTableRatingContainer.extensionValues[i].indValue = Double.parseDouble(elem.getChildTextTrim("ind"));
                    usgsStreamTableRatingContainer.extensionValues[i].depValue = Double.parseDouble(elem.getChildTextTrim("dep"));
                    usgsStreamTableRatingContainer.extensionValues[i].note = elem.getChildTextTrim("note");
                }
            }
        }
        Element verticalDatumElement = ratingElement.getChild("vertical-datum-info");
        if (verticalDatumElement != null) {
            try {
                usgsStreamTableRatingContainer.setVerticalDatumContainer(new VerticalDatumContainer(verticalDatumElement.toString()));
            } catch (VerticalDatumException e) {
                throw new RatingException(e);
            }
        }
        return usgsStreamTableRatingContainer;
    }

    /**
     * Factory constructor from an XML snippet
     *
     * @param xmlText the XML snippet
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static UsgsStreamTableRatingContainer usgsStreamTableRatingContainer(String xmlText) throws RatingException {
        AbstractRatingContainer arc = abstractRatingContainer(xmlText);
        if (arc instanceof UsgsStreamTableRatingContainer) {
            return (UsgsStreamTableRatingContainer) arc;
        } else {
            throw new RatingException("XML text does not specify an UsgsStreamTableRating object.");
        }
    }

    /**
     * Generates an XML string from this object. The subclass overrides should normally be called instead of this.
     *
     * @param usgsStreamTableRatingContainer container to serialize
     * @param indent                         The amount to indent each level
     * @param level                          The initial level of indentation
     * @return the generated XML
     */
    public static String toXml(UsgsStreamTableRatingContainer usgsStreamTableRatingContainer, CharSequence indent, int level) {
        RatingValueContainer[] values = usgsStreamTableRatingContainer.values;
        RatingSetContainer shifts = usgsStreamTableRatingContainer.shifts;
        TableRatingContainer offsets = usgsStreamTableRatingContainer.offsets;
        boolean hasValues = values != null;
        boolean hasShifts = shifts != null && shifts.abstractRatingContainers != null;
        boolean hasOffsets = offsets != null && offsets.values != null;
        if (hasValues && !hasShifts && !hasOffsets) {
            //-----------------------------//
            // serialize as a table rating //
            //-----------------------------//
            return toXml((TableRatingContainer) usgsStreamTableRatingContainer, indent, level);
        }
        VerticalDatumContainer vdc = usgsStreamTableRatingContainer.getVerticalDatumContainer();
        try {
            if (vdc != null && vdc.getCurrentOffset() != 0.) {
                UsgsStreamTableRatingContainer clone = new UsgsStreamTableRatingContainer();
                usgsStreamTableRatingContainer.clone(clone);
                clone.toNativeVerticalDatum();
                return toXml(clone, indent, level);
            }
        } catch (VerticalDatumException e) {
            throw new RatingRuntimeException(e);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; ++i) {
            sb.append(indent);
        }
        String prefix = sb.toString();
        sb.delete(0, sb.length());
        if (level == 0) {
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            sb.append(
                "<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
            prefix += indent;
        }
        sb.append(RatingXmlUtil.toXml(usgsStreamTableRatingContainer, prefix, indent, "usgs-stream-rating"));
        String pointPrefix = prefix + indent + indent;
        if (hasShifts) {
            HecTime hectime = new HecTime();
            for (AbstractRatingContainer arc : shifts.abstractRatingContainers) {
                sb.append(prefix).append(indent).append("<height-shifts>\n");
                if (arc.effectiveDateMillis == UNDEFINED_TIME) {
                    sb.append(prefix).append(indent).append(indent).append("<effective-date/>\n");
                } else {
                    hectime.setTimeInMillis(arc.effectiveDateMillis);
                    sb.append(prefix).append(indent).append(indent).append("<effective-date>").append(hectime.getXMLDateTime(0))
                      .append("</effective-date>\n");
                }
                if (arc.transitionStartDateMillis == UNDEFINED_TIME) {
                    sb.append(prefix).append(indent).append(indent).append("<transition-start-date/>\n");
                } else {
                    hectime.setTimeInMillis(arc.transitionStartDateMillis);
                    sb.append(prefix).append(indent).append(indent).append("<transition-start-date>").append(hectime.getXMLDateTime(0))
                      .append("</transition-start-date>\n");
                }
                if (arc.createDateMillis == UNDEFINED_TIME) {
                    sb.append(prefix).append(indent).append(indent).append("<create-date/>\n");
                } else {
                    hectime.setTimeInMillis(arc.createDateMillis);
                    sb.append(prefix).append(indent).append(indent).append("<create-date>").append(hectime.getXMLDateTime(0))
                      .append("</create-date>\n");
                }
                sb.append(prefix).append(indent).append(indent).append("<active>").append(arc.active).append("</active>\n");
                if (arc.description != null) {
                    sb.append(prefix).append(indent).append(indent).append("<description>").append(TextUtil.xmlEntityEncode(arc.description))
                      .append("</description>\n");
                }
                TableRatingContainer trc = (TableRatingContainer) arc;
                if (trc.values != null) {
                    for (RatingValueContainer rvc : trc.values) {
                        RatingValueContainerXmlFactory.toXml(rvc, pointPrefix, indent, sb);
                    }
                }
                sb.append(prefix).append(indent).append("</height-shifts>\n");
            }
        }
        if (hasOffsets) {
            sb.append(prefix).append(indent).append("<height-offsets>\n");
            for (RatingValueContainer rvc : offsets.values) {
                RatingValueContainerXmlFactory.toXml(rvc, pointPrefix, indent, sb);
            }
            sb.append(prefix).append(indent).append("</height-offsets>\n");
        }
        if (values == null) {
            sb.append(prefix).append(indent).append("<rating-points/>\n");
        } else {
            sb.append(prefix).append(indent).append("<rating-points>\n");
            for (RatingValueContainer rvc : values) {
                RatingValueContainerXmlFactory.toXml(rvc, pointPrefix, indent, sb);
            }
            sb.append(prefix).append(indent).append("</rating-points>\n");
        }
        RatingValueContainer[] extensionValues = usgsStreamTableRatingContainer.extensionValues;
        if (extensionValues != null) {
            sb.append(prefix).append(indent).append("<extension-points>\n");
            for (RatingValueContainer rvc : extensionValues) {
                RatingValueContainerXmlFactory.toXml(rvc, pointPrefix, indent, sb);
            }
            sb.append(prefix).append(indent).append("</extension-points>\n");
        }
        sb.append(prefix).append("</usgs-stream-rating>\n");
        if (level == 0) {
            sb.append("</ratings>\n");
        }
        return sb.toString();
    }

    /**
     * Factory constructor from a jdom element
     *
     * @param ratingElement the jdom element
     * @return container object represented by jdom input
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static TableRatingContainer tableRatingContainer(Element ratingElement) throws RatingException {
        try {
            TableRatingContainer tableRatingContainer = new TableRatingContainer();
            RatingXmlUtil.populateCommonDataFromXml(ratingElement, tableRatingContainer);
            if (!ratingElement.getChildren("rating-points").isEmpty()) {
                tableRatingContainer.values = RatingValueContainerXmlFactory.makeContainers(ratingElement, "rating-points");
            }
            if (!ratingElement.getChildren("extension-points").isEmpty()) {
                tableRatingContainer.extensionValues = RatingValueContainerXmlFactory.makeContainers(ratingElement, "extension-points");
            }
            return tableRatingContainer;
        } catch (VerticalDatumException e) {
            throw new RatingException(e);
        }
    }

    /**
     * Factory constructor from an XML snippet
     *
     * @param xmlText the XML snippet
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static TableRatingContainer tableRatingContainer(String xmlText) throws RatingException {
        AbstractRatingContainer arc = abstractRatingContainer(xmlText);
        if (arc instanceof TableRatingContainer) {
            return (TableRatingContainer) arc;
        } else {
            throw new RatingException("XML text does not specify an TableRating object.");
        }
    }

    /**
     * Generates an XML string from this object. The subclass overrides should normally be called instead of this.
     *
     * @param tableRatingContainer container to serialize
     * @param indent               The amount to indent each level
     * @param level                The initial level of indentation
     * @return the generated XML
     */
    public static String toXml(TableRatingContainer tableRatingContainer, CharSequence indent, int level) {
        VerticalDatumContainer vdc = tableRatingContainer.getVerticalDatumContainer();
        try {
            if (vdc != null && vdc.getCurrentOffset() != 0.) {
                TableRatingContainer clone = new TableRatingContainer();
                tableRatingContainer.clone(clone);
                clone.toNativeVerticalDatum();
                return toXml(clone, indent, level);
            }
        } catch (VerticalDatumException e) {
            throw new RatingRuntimeException(e);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; ++i) {
            sb.append(indent);
        }
        String prefix = sb.toString();
        sb.delete(0, sb.length());
        if (level == 0) {
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            sb.append(
                "<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
            prefix += indent;
        }

        sb.append(RatingXmlUtil.toXml(tableRatingContainer, prefix, indent, "simple-rating"));
        String pointsPrefix = prefix + indent;
        if (tableRatingContainer.values != null) {
            boolean multiParam = tableRatingContainer.values[0].depTable != null;
            if (multiParam) {
                for (int i = 0; i < tableRatingContainer.values.length; ++i) {
                    RatingValueContainerXmlFactory.toXml(tableRatingContainer.values[i], pointsPrefix, indent, sb);
                }
            } else {
                if (tableRatingContainer.values == null) {
                    sb.append(prefix).append(indent).append("<rating-points/>\n");
                } else {
                    sb.append(prefix).append(indent).append("<rating-points>\n");
                    for (int i = 0; i < tableRatingContainer.values.length; ++i) {
                        RatingValueContainerXmlFactory.toXml(tableRatingContainer.values[i], pointsPrefix, indent, sb);
                    }
                    sb.append(prefix).append(indent).append("</rating-points>\n");
                }
            }
        }
        if (tableRatingContainer.extensionValues != null) {
            boolean multiParam = tableRatingContainer.extensionValues[0].depTable != null;
            if (multiParam) {
                AbstractRating.getLogger().severe("Multiple independent parameter ratings cannot use extension values, ignoring");
            } else {
                if (tableRatingContainer.extensionValues != null) {
                    sb.append(prefix).append(indent).append("<extension-points>\n");
                    for (int i = 0; i < tableRatingContainer.extensionValues.length; ++i) {
                        RatingValueContainerXmlFactory.toXml(tableRatingContainer.extensionValues[i], pointsPrefix, indent, sb);
                    }
                    sb.append(prefix).append(indent).append("</extension-points>\n");
                }
            }
        }
        sb.append(prefix).append("</simple-rating>\n");
        if (level == 0) {
            sb.append("</ratings>\n");
        }
        return sb.toString();
    }

    /**
     * Factory constructor from a jdom element
     *
     * @param ratingElement the jdom element
     * @return container object represented by jdom input
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static TransitionalRatingContainer transitionalRatingContainer(Element ratingElement) throws RatingException {
        try {
            TransitionalRatingContainer transitionalRatingContainer = new TransitionalRatingContainer();
            RatingXmlUtil.populateCommonDataFromXml(ratingElement, transitionalRatingContainer);
            return transitionalRatingContainer;
        } catch (VerticalDatumException e) {
            throw new RatingException(e);
        }
    }

    /**
     * Factory constructor from an XML snippet
     *
     * @param xmlText the XML snippet
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static TransitionalRatingContainer transitionalRatingContainer(String xmlText) throws RatingException {
        RatingSetContainer rsc = RatingSetContainerXmlFactory.ratingSetContainerFromXml(xmlText);
        if (rsc.abstractRatingContainers.length == 1 && rsc.abstractRatingContainers[0] instanceof TransitionalRatingContainer) {
            return (TransitionalRatingContainer) rsc.abstractRatingContainers[0];
        } else {
            throw new RatingException("XML text does not specify an TransitionalRatingContainer object.");
        }
    }

    /**
     * Generates an XML string from this object. The subclass overrides should normally be called instead of this.
     *
     * @param transitionalRatingContainer container to serialize
     * @param indent                      The amount to indent each level
     * @param level                       The initial level of indentation
     * @return the generated XML
     */
    public static String toXml(TransitionalRatingContainer transitionalRatingContainer, CharSequence indent, int level) {
        VerticalDatumContainer vdc = transitionalRatingContainer.getVerticalDatumContainer();
        try {
            if (vdc != null && vdc.getCurrentOffset() != 0.) {
                TransitionalRatingContainer clone = (TransitionalRatingContainer) transitionalRatingContainer.clone();
                clone.toNativeVerticalDatum();
                return toXml(clone, indent, level);
            }
        } catch (VerticalDatumException e) {
            throw new RatingRuntimeException(e);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; ++i) {
            sb.append(indent);
        }
        String prefix = sb.toString();
        sb.delete(0, sb.length());
        List<String> ratingXmlStrings = new ArrayList<>();
        if (level == 0) {
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            sb.append(
                "<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
            SortedSet<String> templateXmlStrings = new TreeSet<>();
            SortedSet<String> specXmlStrings = new TreeSet<>();
            getSourceRatingsXml(transitionalRatingContainer, indent, level + 1, templateXmlStrings, specXmlStrings, ratingXmlStrings);
            for (String templateXml : templateXmlStrings) {
                sb.append(templateXml);
            }
            for (String specXml : specXmlStrings) {
                sb.append(specXml);
            }
            prefix += indent;
        }
        sb.append(RatingXmlUtil.toXml(transitionalRatingContainer, prefix, indent, "transitional-rating"));
        String[] conditions = transitionalRatingContainer.conditions;
        String[] evaluations = transitionalRatingContainer.evaluations;
        SourceRatingContainer[] sourceRatings = transitionalRatingContainer.sourceRatings;
        if (conditions == null || conditions.length == 0) {
            sb.append(prefix).append(indent).append("<select/>\n");
        } else {
            sb.append(prefix).append(indent).append("<select>\n");
            for (int i = 0; i < conditions.length; ++i) {
                sb.append(prefix).append(indent).append(indent).append("<case position=\"" + (i + 1) + "\">\n");
                sb.append(prefix).append(indent).append(indent).append(indent)
                  .append(String.format("<when>%s</when>\n", TextUtil.xmlEntityEncode(conditions[i])));
                sb.append(prefix).append(indent).append(indent).append(indent)
                  .append(String.format("<then>%s</then>\n", TextUtil.xmlEntityEncode(evaluations[i])));
                sb.append(prefix).append(indent).append(indent).append("</case>\n");
            }
            sb.append(prefix).append(indent).append(indent)
              .append(String.format("<default>%s</default>\n", TextUtil.xmlEntityEncode(evaluations[evaluations.length - 1])));
            sb.append(prefix).append(indent).append("</select>\n");
        }
        if (sourceRatings == null) {
            sb.append(prefix).append(indent).append("<source-ratings/>\n");
        } else {
            sb.append(prefix).append(indent).append("<source-ratings>\n");
            StringBuilder sourceRatingUnits = new StringBuilder();
            for (int i = 0; i < sourceRatings.length; ++i) {
                sourceRatingUnits.setLength(0);
                sourceRatingUnits.append(" {");
                for (int j = 0; j < sourceRatings[i].units.length; ++j) {
                    sourceRatingUnits.append(j == 0 ? "" : j == sourceRatings[i].units.length - 1 ? ";" : ",")
                                     .append(TextUtil.xmlEntityEncode(sourceRatings[i].units[j]));
                }
                sourceRatingUnits.append("}");
                sb.append(prefix).append(indent).append(indent).append("<rating-spec-id position=\"" + (i + 1) + "\">\n");
                sb.append(prefix).append(indent).append(indent).append(indent)
                  .append(TextUtil.xmlEntityEncode(sourceRatings[i].rsc.ratingSpecContainer.specId)).append("\n");
                sb.append(prefix).append(indent).append(indent).append("</rating-spec-id>\n");
            }
            sb.append(prefix).append(indent).append("</source-ratings>\n");
        }
        sb.append(prefix).append("</transitional-rating>\n");
        if(!ratingXmlStrings.isEmpty()) {
            for (String ratingsXml : ratingXmlStrings) {
                sb.append(ratingsXml);
            }
        }
        if (level == 0) {
            sb.append("</ratings>\n");
        }
        return sb.toString();
    }

    static void getSourceRatingsXml(TransitionalRatingContainer transitionalRatingContainer, CharSequence indent, int level,
                                    Set<String> templateStrings, Set<String> specStrings, List<String> ratingStrings) {
        SourceRatingContainer[] sourceRatings = transitionalRatingContainer.sourceRatings;
        if (sourceRatings != null) {
            for (SourceRatingContainer src : sourceRatings) {
                if (src.rsc != null) {
                    RatingSpecXmlFactory.toXml(src.rsc.ratingSpecContainer, indent, level);
                    templateStrings.add(RatingSpecXmlFactory.toXml(src.rsc.ratingSpecContainer, indent, level));
                    specStrings.add(RatingSpecXmlFactory.toSpecXml(src.rsc.ratingSpecContainer, indent, level));
                    for (AbstractRatingContainer arc : src.rsc.abstractRatingContainers) {
                        ratingStrings.add(toXml(arc, indent, level));
                    }
                    if (src.rsc.abstractRatingContainers[0] instanceof VirtualRatingContainer) {
                        VirtualRatingContainer vrc = (VirtualRatingContainer) src.rsc.abstractRatingContainers[0];
                        getSourceRatingsXml(vrc, indent, level, templateStrings, specStrings, ratingStrings);
                    } else if (src.rsc.abstractRatingContainers[0] instanceof TransitionalRatingContainer) {
                        TransitionalRatingContainer trc = (TransitionalRatingContainer) src.rsc.abstractRatingContainers[0];
                        getSourceRatingsXml(trc, indent, level, templateStrings, specStrings, ratingStrings);
                    }
                }
            }
        }
    }

    /**
     * Generates an XML string from this object. The subclass overrides should normally be called instead of this.
     *
     * @param ratingSetContainer container to serialize
     * @param indent             The amount to indent each level
     * @param level              The initial level of indentation
     * @param includeTemplate    include the rating template in the xml serialization
     * @return the generated XML
     */
    public static String toXml(RatingSetContainer ratingSetContainer, CharSequence indent, int level, boolean includeTemplate,
                               boolean includeEmptyTableRatings) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; ++i) {
            sb.append(indent);
        }
        String prefix = sb.toString();
        sb.delete(0, sb.length());
        List<String> ratingXmlStrings = new ArrayList<>();
        SortedSet<String> templateXmlStrings = new TreeSet<>();
        SortedSet<String> specXmlStrings = new TreeSet<>();
        String thisTemplateXml = RatingSpecXmlFactory.toXml(ratingSetContainer.ratingSpecContainer, indent, level + 1);
        if (level == 0) {
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            sb.append(
                "<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
            if (ratingSetContainer.abstractRatingContainers != null) {
                for (AbstractRatingContainer arc : ratingSetContainer.abstractRatingContainers) {
                    if (arc instanceof VirtualRatingContainer) {
                        VirtualRatingContainer vrc = (VirtualRatingContainer) arc;
                        getSourceRatingsXml(vrc, indent, level + 1, templateXmlStrings, specXmlStrings, ratingXmlStrings);
                    } else if (arc instanceof TransitionalRatingContainer) {
                        TransitionalRatingContainer trrc = (TransitionalRatingContainer) arc;
                        getSourceRatingsXml(trrc, indent, level + 1, templateXmlStrings, specXmlStrings, ratingXmlStrings);
                    }
                }
            }
        }
        for (String templateXml : templateXmlStrings) {
            if (!templateXml.equals(thisTemplateXml)) {
                sb.append(templateXml);
            }
        }
        if (ratingSetContainer.ratingSpecContainer != null) {
            sb.append(RatingSpecXmlFactory.toXml(ratingSetContainer.ratingSpecContainer, indent, level + 1, includeTemplate));
        }
        for (String specXml : specXmlStrings) {
            sb.append(specXml);
        }
        if (ratingSetContainer.abstractRatingContainers != null) {
            for (AbstractRatingContainer arc : ratingSetContainer.abstractRatingContainers) {
                if (includeEmptyTableRatings || !(arc instanceof TableRatingContainer) || ((TableRatingContainer) arc).values != null) {
                    sb.append(toXml(arc, indent, level + 1));
                }
            }
        }
        for (String ratingXml : ratingXmlStrings) {
            sb.append(ratingXml);
        }
        if (level == 0) {
            sb.append(prefix).append("</ratings>\n");
        }
        return sb.toString();
    }

    static void getSourceRatingsXml(VirtualRatingContainer virtualRatingContainer, CharSequence indent, int level, Set<String> templateStrings,
                                    Set<String> specStrings, List<String> ratingStrings) {
        SourceRatingContainer[] sourceRatings = virtualRatingContainer.sourceRatings;
        if (sourceRatings != null) {
            for (SourceRatingContainer src : sourceRatings) {
                if (src.rsc != null) {
                    templateStrings.add(RatingSpecXmlFactory.toXml(src.rsc.ratingSpecContainer, indent, level));
                    specStrings.add(RatingSpecXmlFactory.toSpecXml(src.rsc.ratingSpecContainer, indent, level));
                    for (AbstractRatingContainer arc : src.rsc.abstractRatingContainers) {
                        ratingStrings.add(toXml(arc, indent, level));
                    }
                    if (src.rsc.abstractRatingContainers[0] instanceof VirtualRatingContainer) {
                        VirtualRatingContainer vrc = (VirtualRatingContainer) src.rsc.abstractRatingContainers[0];
                        getSourceRatingsXml(vrc, indent, level, templateStrings, specStrings, ratingStrings);
                    } else if (src.rsc.abstractRatingContainers[0] instanceof TransitionalRatingContainer) {
                        TransitionalRatingContainer trc = (TransitionalRatingContainer) src.rsc.abstractRatingContainers[0];
                        getSourceRatingsXml(trc, indent, level, templateStrings, specStrings, ratingStrings);
                    }
                }
            }
        }
    }
}
