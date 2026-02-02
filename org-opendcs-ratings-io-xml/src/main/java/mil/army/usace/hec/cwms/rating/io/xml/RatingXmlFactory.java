/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package org.opendcs.ratings.io.xml;


import org.opendcs.ratings.AbstractRating;
import org.opendcs.ratings.AbstractRatingSet;
import org.opendcs.ratings.ExpressionRating;
import org.opendcs.ratings.RatingSet;
import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.RatingSetFactory;
import org.opendcs.ratings.TableRating;
import org.opendcs.ratings.TransitionalRating;
import org.opendcs.ratings.UsgsStreamTableRating;
import org.opendcs.ratings.VirtualRating;
import org.opendcs.ratings.io.AbstractRatingContainer;
import org.opendcs.ratings.io.ExpressionRatingContainer;
import org.opendcs.ratings.io.RatingSetContainer;
import org.opendcs.ratings.io.TableRatingContainer;
import org.opendcs.ratings.io.TransitionalRatingContainer;
import org.opendcs.ratings.io.UsgsStreamTableRatingContainer;
import org.opendcs.ratings.io.VirtualRatingContainer;
import hec.hecmath.HecMathException;
import hec.hecmath.TextMath;
import hec.io.TextContainer;
import hec.util.TextUtil;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RatingXmlFactory {

    private static final Logger LOGGER = Logger.getLogger(RatingXmlFactory.class.getName());

    private RatingXmlFactory() {
        throw new AssertionError("Utility class");
    }

    /**
     * Generates a new AbstractRating implementation object from XML text
     *
     * @param xmlText The XML text to generate the rating object from
     * @return The generated rating object.
     * @throws RatingException
     */
    public static AbstractRating abstractRating(String xmlText) throws RatingException {

        AbstractRatingContainer arc = RatingContainerXmlFactory.abstractRatingContainer(xmlText);
        if (arc instanceof UsgsStreamTableRatingContainer) {
            return new UsgsStreamTableRating((UsgsStreamTableRatingContainer) arc);
        }
        if (arc instanceof TableRatingContainer) {
            return new TableRating((TableRatingContainer) arc);
        }
        if (arc instanceof ExpressionRatingContainer) {
            return new ExpressionRating((ExpressionRatingContainer) arc);
        }
        if (arc instanceof TransitionalRatingContainer) {
            return new TransitionalRating((TransitionalRatingContainer) arc);
        }
        if (arc instanceof VirtualRatingContainer) {
            return new VirtualRating((VirtualRatingContainer) arc);
        }
        return null;
    }

    /**
     * serializes this rating as an XML string.
     *
     * @param abstractRating rating to serialize
     * @param indent         The character(s) used for indentation in the XML string
     * @param indentLevel    The beginning indentation level for the XML string
     * @return This rating as an XML string
     */
    public static String toXml(AbstractRating abstractRating, CharSequence indent, int indentLevel) throws RatingException {

        if (abstractRating instanceof UsgsStreamTableRating) {
            return toXml((UsgsStreamTableRating) abstractRating, indent, indentLevel);
        }
        if (abstractRating instanceof TableRating) {
            return toXml((TableRating) abstractRating, indent, indentLevel);
        }
        if (abstractRating instanceof ExpressionRating) {
            return toXml((ExpressionRating) abstractRating, indent, indentLevel);
        }
        if (abstractRating instanceof TransitionalRating) {
            return toXml((TransitionalRating) abstractRating, indent, indentLevel);
        }
        if (abstractRating instanceof VirtualRating) {
            return toXml((VirtualRating) abstractRating, indent, indentLevel);
        }
        return null;
    }

    /**
     * constructor from XML text
     *
     * @param xmlText The XML text to initialize from
     * @throws RatingException
     */
    public static ExpressionRating expressionRating(String xmlText) throws RatingException {
        ExpressionRatingContainer container = RatingContainerXmlFactory.expressionRatingContainer(xmlText);
        return new ExpressionRating(container);
    }


    /**
     * serializes this rating as an XML string.
     *
     * @param rating      rating to serialize
     * @param indent      The character(s) used for indentation in the XML string
     * @param indentLevel The beginning indentation level for the XML string
     * @return This rating as an XML string
     */
    public static String toXml(ExpressionRating rating, CharSequence indent, int indentLevel) {
        return RatingContainerXmlFactory.toXml(rating.getData(), indent, indentLevel);
    }

    /**
     * constructs from XML text
     *
     * @param xmlText The XML text to initialize from
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static TableRating tableRating(String xmlText) throws RatingException {
        TableRatingContainer container = RatingContainerXmlFactory.tableRatingContainer(xmlText);
        return new TableRating(container);
    }

    /**
     * serializes this rating as an XML string.
     *
     * @param rating      rating to serialize
     * @param indent      The character(s) used for indentation in the XML string
     * @param indentLevel The beginning indentation level for the XML string
     * @return This rating as an XML string
     */
    public static String toXml(TableRating rating, CharSequence indent, int indentLevel) {
        return RatingContainerXmlFactory.toXml(rating.getData(), indent, indentLevel);
    }

    /**
     * constructs from XML text
     *
     * @param xmlText The XML text to initialize from
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static UsgsStreamTableRating usgsStreamTableRating(String xmlText) throws RatingException {
        UsgsStreamTableRatingContainer container = RatingContainerXmlFactory.usgsStreamTableRatingContainer(xmlText);
        return new UsgsStreamTableRating(container);
    }


    /**
     * serializes this rating as an XML string.
     *
     * @param rating      rating to serialize
     * @param indent      The character(s) used for indentation in the XML string
     * @param indentLevel The beginning indentation level for the XML string
     * @return This rating as an XML string
     */
    public static String toXml(UsgsStreamTableRating rating, CharSequence indent, int indentLevel) throws RatingException {
        UsgsStreamTableRating clone = new UsgsStreamTableRating(rating.getData());
        if (clone.getShifts() != null) {
            try {
                clone.getShifts().removeRating(rating.getEffectiveDate());
            } catch (RatingException e) {
            }
        }
        return RatingContainerXmlFactory.toXml(clone.getData(), indent, indentLevel);
    }

    /**
     * constructs from XML text
     *
     * @param xmlText The XML text to initialize from
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static TransitionalRating transitionalRatingRating(String xmlText) throws RatingException {
        TransitionalRatingContainer container = RatingContainerXmlFactory.transitionalRatingContainer(xmlText);
        return new TransitionalRating(container);
    }


    /**
     * serializes this rating as an XML string.
     *
     * @param rating      rating to serialize
     * @param indent      The character(s) used for indentation in the XML string
     * @param indentLevel The beginning indentation level for the XML string
     * @return This rating as an XML string
     */
    public static String toXml(TransitionalRating rating, CharSequence indent, int indentLevel) {
        return RatingContainerXmlFactory.toXml(rating.getData(), indent, indentLevel);
    }

    /**
     * constructs from XML text
     *
     * @param xmlText The XML text to initialize from
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static VirtualRating virtualRating(String xmlText) throws RatingException {
        VirtualRatingContainer container = RatingContainerXmlFactory.virtualRatingContainer(xmlText);
        return new VirtualRating(container);
    }


    /**
     * serializes this rating as an XML string.
     *
     * @param rating      rating to serialize
     * @param indent      The character(s) used for indentation in the XML string
     * @param indentLevel The beginning indentation level for the XML string
     * @return This rating as an XML string
     */
    public static String toXml(VirtualRating rating, CharSequence indent, int indentLevel) {
        return RatingContainerXmlFactory.toXml(rating.getData(), indent, indentLevel);
    }

    /**
     * Outputs the rating set in an uncompressed XML representation
     *
     * @param ratingSet rating set to serialize
     * @return The compressed XML text
     */
    public static String toXml(RatingSet ratingSet, CharSequence indent) {
        return RatingContainerXmlFactory.toXml(ratingSet.getData(), indent, 0, true, true);
    }

    /**
     * Outputs the rating set in a compress XML instance suitable for storing in DSS
     *
     * @param ratingSet rating set to serialize
     * @return The compressed XML text
     * @throws RatingException any error shrinking the data
     */
    public static String toCompressedXml(RatingSet ratingSet) throws RatingException {
        try {
            return TextUtil.compress(toXml(ratingSet, "  "), "base64");
        } catch (RuntimeException | IOException t) {
            throw new RatingException(t);
        }
    }

    /**
     * Factory Constructor from TextContainer (as read from DSS)
     *
     * @param tc The TextContainer object to initialize from
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static AbstractRatingSet ratingSet(TextContainer tc) throws RatingException {
        String[] lines = tc.text.split("\\n");
        String className = RatingSet.class.getName();
        for (int i = 0; i < lines.length - 1; ++i) {
            if (lines[i].equals(className)) {
                int extra = lines[i + 1].length() % 4;
                int last = lines[i + 1].length() - extra;
                String compressedXml = lines[i + 1].substring(0, last);
                try {
                    String uncompressed = TextUtil.uncompress(compressedXml, "base64");
                    RatingSetContainer container = RatingSetContainerXmlFactory.ratingSetContainerFromXml(uncompressed);
                    return RatingSetFactory.ratingSet(container);
                } catch (Exception e) {
                    throw new RatingException(e);
                }
            }
        }
        throw new RatingException("Invalid text for RatingSet");
    }

    /**
     * Factory Constructor from an uncompressed XML instance
     *
     * @param xmlText The XML instance to initialize from
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static AbstractRatingSet ratingSet(String xmlText) throws RatingException {
        // clone can transform into a ReferenceRatingContainer
        try {
            RatingSetContainer container = RatingSetContainerXmlFactory.ratingSetContainerFromXml(xmlText).clone();
            return RatingSetFactory.ratingSet(container);
        } catch (RatingException e1) {
            try {
                String uncompressedXmlText = TextUtil.uncompress(xmlText, "base64");
                RatingSetContainer container = RatingSetContainerXmlFactory.ratingSetContainerFromXml(uncompressedXmlText).clone();
                return RatingSetFactory.ratingSet(container);
            } catch (RatingException | IOException | RuntimeException e2) {
                LOGGER.log(Level.FINE, "Invalid compressed ratings xml: " + xmlText, e2);
                RatingException ex = new RatingException("Text is not a valid compressed or uncompressed CWMS Ratings XML instance.", e1);
                ex.addSuppressed(e2);
                throw ex;
            }
        }
    }

    /**
     * Public Constructor from an XML instance
     *
     * @param xmlText      The XML instance to initialize from
     * @param isCompressed Flag specifying whether the string is a compressed XML string
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static AbstractRatingSet ratingSet(String xmlText, boolean isCompressed) throws RatingException {
        if (isCompressed) {
            try {
                xmlText = TextUtil.uncompress(xmlText, "base64");
            } catch (IOException | RuntimeException e) {
                throw new RatingException(e);
            }
        }
        // clone can transform into a ReferenceRatingContainer
        RatingSetContainer container = RatingSetContainerXmlFactory.ratingSetContainerFromXml(xmlText).clone();
        return RatingSetFactory.ratingSet(container);
    }

    /**
     * Factory Constructor from TextMath (as read from DSS)
     *
     * @param tm The TextMath object to initialize from
     * @throws RatingException any errors transforming serialized rating into data object
     */
    public static AbstractRatingSet ratingSet(TextMath tm) throws RatingException {
        try {
            return ratingSet((TextContainer) tm.getData());
        } catch (HecMathException | RuntimeException e) {
            throw new RatingException(e);
        }
    }

    /**
     * Retrieves a TextContainer containing the data of this object, suitable for storing to DSS.
     *
     * @param ratingSet rating set to serialize into text container
     * @return The TextContainer
     * @throws RatingException any errors reading from dss or processing the data
     */
    public static TextContainer textContainer(AbstractRatingSet ratingSet) throws RatingException {
        try {
            TextContainer tc = new TextContainer();
            tc.fullName = ratingSet.getDssPathname();
            tc.text = String.format("%s\n%s", RatingSet.class.getName(), toCompressedXml(ratingSet));
            return tc;
        } catch (RuntimeException t) {
            throw new RatingException(t);
        }
    }
}
