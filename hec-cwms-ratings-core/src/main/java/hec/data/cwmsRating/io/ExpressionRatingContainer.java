/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

/**
 *
 */
package hec.data.cwmsRating.io;

import hec.data.RatingException;
import hec.data.RatingRuntimeException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.ExpressionRating;
import hec.util.TextUtil;
import mil.army.usace.hec.metadata.VerticalDatumException;

import org.jdom.Element;

/**
 * Container for ExpressionRating data
 *
 * @author Mike Perryman
 */
public class ExpressionRatingContainer extends AbstractRatingContainer
{
	/**
	 * The mathematical expression for the rating
	 */
	public String expression = null;
	/**
	 * Public empty constructor;
	 */
	public ExpressionRatingContainer() {}
	/**
	 * Public constructor from a jdom element
	 * @param ratingElement the jdom element
	 * @throws RatingException
	 */
	public ExpressionRatingContainer(Element ratingElement) throws RatingException {
		populateFromXml(ratingElement);
	}
	/**
	 * public constructor from an XML snippet
	 * @param xmlText the XML snippet
	 * @throws RatingException
	 */
	public ExpressionRatingContainer(String xmlText) throws RatingException {
		populateFromXml(xmlText);
	}
	/**
	 * Populates data from an jdom element
	 * @param ratingElement
	 * @throws RatingException
	 */
	public void populateFromXml(Element ratingElement) throws RatingException {
		try {
			AbstractRatingContainer.populateCommonDataFromXml(ratingElement, this);
		}
		catch (VerticalDatumException e) {
			throw new RatingException(e);
		}
		expression = ratingElement.getChildTextTrim("formula");
	}
	/**
	 * Populates data from an XML snippet
	 * @param xmlText
	 * @throws RatingException
	 */
	public void populateFromXml(String xmlText) throws RatingException {
		AbstractRatingContainer arc = AbstractRatingContainer.buildFromXml(xmlText);
		if (arc instanceof ExpressionRatingContainer) {
			arc.clone(this);
		}
		else {
			throw new RatingException("XML text does not specify an ExpressionRating object.");
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = obj == this;
		if (!result) {
			do {
				if (obj == null || obj.getClass() != getClass()) break;
				ExpressionRatingContainer other = (ExpressionRatingContainer)obj;
				if (!super.equals(obj)) break;
				if ((other.expression == null) != (expression == null)) break;
				if (expression != null) {
					if (!other.expression.equals(expression)) break;
				}
				result = true;
			} while(false);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode() + super.hashCode() + (expression == null ? 0 : expression.hashCode());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * hec.data.cwmsRating.io.AbstractRatingContainer#clone(hec.data.cwmsRating
	 * .io.AbstractRatingContainer)
	 */
	@Override
	public void clone(AbstractRatingContainer other)
	{
		if (!(other instanceof ExpressionRatingContainer))
		{
			throw new IllegalArgumentException("Clone-to object must be a ExpressionRatingContainer.");
		}
		super.clone(other);
		ExpressionRatingContainer rec = (ExpressionRatingContainer) other;
		rec.expression = expression;
	}

	@Override
	public AbstractRatingContainer getInstance()
	{
		return new ExpressionRatingContainer();
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#newRating()
	 */
	@Override
	public AbstractRating newRating() throws RatingException {
		ExpressionRating rating = new ExpressionRating(this);
		return rating;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#toXml(java.lang.CharSequence)
	 */
	@Override
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#toXml(java.lang.CharSequence, int)
	 */
	@Override
	public String toXml(CharSequence indent, int level) {
		try {
			if (vdc != null && vdc.getCurrentOffset() != 0.) {
				ExpressionRatingContainer _clone = new ExpressionRatingContainer();
				clone(_clone);
				_clone.toNativeVerticalDatum();
				return _clone.toXml(indent, level);
			}
		}
		catch (VerticalDatumException e) {
			throw new RatingRuntimeException(e);
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level; ++i) sb.append(indent);
		String prefix = sb.toString();
		sb.delete(0, sb.length());
		if (level == 0) {
			sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			sb.append("<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
			prefix += indent;
		}
		sb.append(super.toXml(prefix, indent, "simple-rating"));
		String expression = this.expression.replaceAll("([Aa][Rr][Gg]|\\$)(\\d)", "I$2");
		sb.append(prefix).append(indent).append("<formula>").append(TextUtil.xmlEntityEncode(expression)).append("</formula>\n");
		sb.append(prefix).append("</simple-rating>\n");
		if (level == 0) {
			sb.append("</ratings>\n");
		}
		return sb.toString();
	}

}
