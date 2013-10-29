package hec.data.cwmsRating.io;

import hec.data.RatingException;
import hec.io.RatingContainer;

import org.jdom.Element;

/**
 * Data container class for TableRating objects
 *
 * @author Mike Perryman
 */
public class TableRatingContainer extends AbstractRatingContainer {
	/**
	 * The rating values
	 */
	public RatingValueContainer[] values = null;
	/**
	 * The extension values
	 */
	public RatingValueContainer[] extensionValues = null;
	/**
	 * The rating method for handling independent parameter values that lie between values in the rating table
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String inRangeMethod = null;
	/**
	 * The rating method for handling independent parameter values that are less than the least values in the rating table
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String outRangeLowMethod = null;
	/**
	 * The rating method for handling independent parameter values that are greater than the greatest values in the rating table
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String outRangeHighMethod = null;
	/**
	 * Fills another TableRatingContainer object with information from this one
	 * @param other The TableRatingContainer object to fill
	 */
	@Override
	public void clone(AbstractRatingContainer other) {
		if (!(other instanceof TableRatingContainer)) {
			throw new IllegalArgumentException("Clone-to object must be a TableRatingContainer.");
		}
		super.clone(other);
		TableRatingContainer trc = (TableRatingContainer)other;
		trc.inRangeMethod = inRangeMethod;
		trc.outRangeLowMethod = outRangeLowMethod;
		trc.outRangeHighMethod = outRangeHighMethod;
		if (values == null) {
			trc.values = null;
		}
		else {
			trc.values = new RatingValueContainer[values.length];
			for (int i = 0; i < values.length; ++i) {
				trc.values[i] = new RatingValueContainer();
				values[i].clone(trc.values[i]);
			}
		}
		if (extensionValues == null) {
			trc.extensionValues = null;
		}
		else {
			trc.extensionValues = new RatingValueContainer[extensionValues.length];
			for (int i = 0; i < extensionValues.length; ++i) {
				extensionValues[i].clone(trc.extensionValues[i]);
			}
		}
	}

	@Override
	public AbstractRatingContainer getInstance()
	{
		return new TableRatingContainer();
	}
	
	public static TableRatingContainer fromXml(Element ratingElement) throws RatingException {
		TableRatingContainer trc = new TableRatingContainer();
		AbstractRatingContainer.fromXml(ratingElement, trc);
		if (ratingElement.getChildren("rating-points").size() > 0) {
			trc.values = RatingValueContainer.makeContainers(ratingElement, "rating-points");
		}
		if (ratingElement.getChildren("extension-points").size() > 0) {
			trc.extensionValues = RatingValueContainer.makeContainers(ratingElement, "extension-points");
		}
		return trc;
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
		String pointPrefix = prefix + indent + indent;
		if (values != null) {
			sb.append(prefix).append(indent).append("<rating-points>\n");
			for (RatingValueContainer rvc : values) {
				rvc.toXml(pointPrefix, indent, sb);
			}
			sb.append(prefix).append(indent).append("</rating-points>\n");
		}
		if (extensionValues != null) {
			sb.append(prefix).append(indent).append("<extension-points>\n");
			for (RatingValueContainer rvc : extensionValues) {
				rvc.toXml(pointPrefix, indent, sb);
			}
			sb.append(prefix).append(indent).append("</extension-points>\n");
		}
		sb.append(prefix).append("</rating>\n");
		if (level == 0) {
			sb.append("</ratings>\n");
		}
		return sb.toString();
	}
}
