/**
 * 
 */
package hec.data.cwmsRating.io;

import hec.data.RatingException;

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
	
	public static ExpressionRatingContainer fromXml(Element ratingElement) throws RatingException {
		ExpressionRatingContainer erc = new ExpressionRatingContainer();
		AbstractRatingContainer.fromXml(ratingElement, erc);
		erc.expression = ratingElement.getChildTextTrim("formula");
		return erc;
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
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level; ++i) sb.append(indent);
		String prefix = sb.toString();
		sb.delete(0, sb.length());
		if (level == 0) {
			sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			sb.append("<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
			prefix += indent;
		}
		sb.append(super.toXml(prefix, indent, "rating"));
		sb.append(prefix).append(indent).append("<formula>").append(expression).append("</formula>\n");
		sb.append(prefix).append("</rating>\n");
		if (level == 0) {
			sb.append("</ratings>\n");
		}
		return sb.toString();
	}
	
}
