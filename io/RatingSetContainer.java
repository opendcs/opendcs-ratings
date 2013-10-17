package hec.data.cwmsRating.io;

import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;
import hec.data.cwmsRating.RatingSetXmlParser;

/**
 * Data container class for RatingSet
 * @author Mike Perryman
 */
public class RatingSetContainer {
	/**
	 * Contains the rating specification
	 */
	public RatingSpecContainer ratingSpecContainer = null;
	/**
	 * Contains the individual ratings
	 */
	public AbstractRatingContainer[] abstractRatingContainers = null;
	/**
	 * Fills another RatingSetContainer object with data from this one
	 * @param other The RatingSetContainer object to fill
	 */
	public void clone(RatingSetContainer other) {
		if (ratingSpecContainer == null) {
			other.ratingSpecContainer = null;
		}
		else {
			if (other.ratingSpecContainer == null) other.ratingSpecContainer = new RatingSpecContainer();
			ratingSpecContainer.clone(other.ratingSpecContainer);
		}
		if (abstractRatingContainers == null) {
			other.abstractRatingContainers = null;
		}
		else {
			other.abstractRatingContainers = new AbstractRatingContainer[abstractRatingContainers.length];
			for (int i = 0; i < abstractRatingContainers.length; ++i) {
				other.abstractRatingContainers[i] = new TableRatingContainer();
				abstractRatingContainers[i].clone(other.abstractRatingContainers[i]);
			}
		}
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		try {
			return String.format(
					"%s - %d ratings",
					ratingSpecContainer == null ? "<NULL>" : ratingSpecContainer.toString(),
					abstractRatingContainers == null ? 0 : abstractRatingContainers.length);	
		}
		catch (Throwable t) {
			return ((Object)this).toString();
		}
	}
	/**
	 * Generates a new RatingSetContainer object from an XML instance.
	 * @param xmlText The XML instance to construct the RatingSet object from. The document (root) node is expected to be
	 *        &lt;ratings&gt;, which is expected to have one or more &lt;rating&gt; or &lt;usgs-stream-rating&gt; child nodes, all of the same
	 *        rating specification.  Appropriate <rating-template> and &lt;rating-spec&gt; nodes are required for the rating set;
	 *        any other template and specification nodes are ignored.
	 * @return A new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSetContainer fromXml(String xmlText) throws RatingException {
		return RatingSetXmlParser.parseString(xmlText);
	}
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}

	public String toXml(CharSequence indent, int level) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level; ++i) sb.append(indent);
		String prefix = sb.toString();
		sb.delete(0, sb.length());
		if (level == 0) {
			sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			sb.append("<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
		}
		if (ratingSpecContainer != null) {
			sb.append(ratingSpecContainer.toXml(indent, level+1));
		}
		if (abstractRatingContainers != null) {
			for (AbstractRatingContainer arc : abstractRatingContainers) {
				sb.append(arc.toXml(indent, level+1));
			}
		}
		if (level == 0) {
			sb.append(prefix).append("</ratings>\n");
		}
		return sb.toString();
	}

}
