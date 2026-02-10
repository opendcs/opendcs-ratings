/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */
package org.opendcs.ratings.io;


import org.opendcs.ratings.AbstractRating;
import org.opendcs.ratings.ExpressionRating;
import org.opendcs.ratings.RatingException;
import org.w3c.dom.Element;

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
	 * Public constructor from a DOM element
	 * @param ratingElement the DOM element
	 * @throws RatingException on error
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#expressionRatingContainer(Element) instead
	 */
	@Deprecated
	public ExpressionRatingContainer(Element ratingElement) throws RatingException {
		populateFromXml(ratingElement);
	}
	/**
	 * public constructor from an XML snippet
	 * @param xmlText the XML snippet
	 * @throws RatingException on error
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#expressionRatingContainer(String) instead
	 */
	@Deprecated
	public ExpressionRatingContainer(String xmlText) throws RatingException {
		populateFromXml(xmlText);
	}

	/**
	 * Populates data from an DOM element
	 * @param ratingElement The rating element to populate from
	 * @throws RatingException on error
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#expressionRatingContainer(ExpressionRatingContainer, CharSequence, int) instead
	 */
	@Deprecated
	public void populateFromXml(Element ratingElement) throws RatingException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		ExpressionRatingContainer container = service.createExpressionRatingContainer(ratingElement);
		container.clone(this);

	}
	/**
	 * Populates data from an XML snippet
	 * @param xmlText The XML text to populate from
	 * @throws RatingException on error
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#expressionRatingContainer(String) instead
	 */
	@Deprecated
	public void populateFromXml(String xmlText) throws RatingException {
		ExpressionRatingContainer container = RatingContainerXmlCompatUtil.getInstance().createExpressionRatingContainer(xmlText);
		container.clone(this);
	}

	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#equals(java.lang.Object)
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
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode() + super.hashCode() + (expression == null ? 0 : expression.hashCode());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.opendcs.ratings.io.AbstractRatingContainer#clone(org.opendcs.ratings
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
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#newRating()
	 */
	@Override
	public AbstractRating newRating() throws RatingException {
        return new ExpressionRating(this);
	}

	/**
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#expressionRatingContainer(CharSequence, int) instead
	 */
	@Deprecated
	@Override
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}

	/**
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#expressionRatingContainer(CharSequence, int) instead
	 */
	@Deprecated
	@Override
	public String toXml(CharSequence indent, int level) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.toXml(this, indent, level);
	}

}
