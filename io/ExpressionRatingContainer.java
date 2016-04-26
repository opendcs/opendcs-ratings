/**
 * 
 */
package hec.data.cwmsRating.io;

import org.jdom.Element;

import hec.data.RatingException;
import hec.data.VerticalDatumException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.ExpressionRating;
import hec.util.TextUtil;

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

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		do {
			if (!(obj instanceof ExpressionRatingContainer)) break;
			ExpressionRatingContainer other = (ExpressionRatingContainer)obj;
			if (!super.equals(obj)) break;
			if ((other.expression == null) != (expression == null)) break;
			if (expression != null) {
				if (!other.expression.equals(expression)) break;
			}
			result = true;
		} while(false);
		return result;
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

	public static ExpressionRatingContainer fromXml(Element ratingElement) throws RatingException {
		ExpressionRatingContainer erc = new ExpressionRatingContainer();
		try {
			AbstractRatingContainer.fromXml(ratingElement, erc);
		}
		catch (VerticalDatumException e) {
			throw new RatingException(e);
		}
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
		try {
			if (vdc != null && vdc.getCurrentOffset() != 0.) {
				ExpressionRatingContainer _clone = new ExpressionRatingContainer();
				clone(_clone);
				_clone.toNativeVerticalDatum();
				return _clone.toXml(indent, level);
			}
		}
		catch (VerticalDatumException e) {
			throw new RuntimeException(e);
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
		sb.append(prefix).append(indent).append("<formula>").append(TextUtil.xmlEntityEncode(expression)).append("</formula>\n");
		sb.append(prefix).append("</simple-rating>\n");
		if (level == 0) {
			sb.append("</ratings>\n");
		}
		return sb.toString();
	}
	
}
