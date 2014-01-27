package hec.data.cwmsRating.io;

import hec.data.IVerticalDatum;
import hec.data.RatingException;
import hec.data.VerticalDatumException;
import hec.data.cwmsRating.RatingSetXmlParser;
import hec.io.VerticalDatumContainer;

/**
 * Data container class for RatingSet
 * @author Mike Perryman
 */
public class RatingSetContainer implements IVerticalDatum {
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
		return toXml(indent, level, true);
	}

	public String toXml(CharSequence indent, int level, boolean includeTemplate) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level; ++i) sb.append(indent);
		String prefix = sb.toString();
		sb.delete(0, sb.length());
		if (level == 0) {
			sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			sb.append("<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
		}
		if (ratingSpecContainer != null) {
			sb.append(ratingSpecContainer.toXml(indent, level+1, includeTemplate));
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
	/**
	 * Add the specified offset to the values of the specified parameter.
	 * @param paramNum Specifies which parameter to add the offset to. 0, 1 = first, second independent, etc... -1 = dependent parameter
	 * @param offset The offset to add
	 * @throws RatingException if not implemented for a specific rating container type or if paramNum is invalid for this container
	 */
	public void addOffset(int paramNum, double offset) throws RatingException {
		for (int i = 0; i < abstractRatingContainers.length; ++i) {
			abstractRatingContainers[i].addOffset(paramNum, offset);
		}
	}
	/**
	 * Returns whether this object has any vertical datum info
	 * @return whether this object has any vertical datum info
	 */
	public boolean hasVerticalDatum() {
		boolean hasVerticalDatum = false;
		if (abstractRatingContainers != null) {
			for (int i = 0; i < abstractRatingContainers.length; ++i) {
				if (abstractRatingContainers[i].hasVerticalDatum()) {
					hasVerticalDatum = true;
					break;
				}
			}
		}
		return hasVerticalDatum;
	}
	/**
	 * Returns whether any included vertical datum info is consistent
	 * @return true if there are zero or one vertical datum containers, or if multiple vertical
	 * datum container specify the same information and false if there are multiple vertical datum
	 * containers with different information 
	 */
	public boolean isVerticalDatumInfoConsistent() {
		boolean isConsistent = true;
		if (abstractRatingContainers != null && abstractRatingContainers.length > 1) {
			VerticalDatumContainer vdc = null;
			for (int i = 0; i < abstractRatingContainers.length; ++i) {
				if (abstractRatingContainers[i].vdc != null) {
					if (vdc == null) {
						vdc = abstractRatingContainers[i].vdc;
					}
					else {
						if (!abstractRatingContainers[i].vdc.equals(vdc)) {
							isConsistent = false;
							break;
						}
					}
				}
			}
		}
		return isConsistent;
	}
	/**
	 * Ensures that every rating has the same vertical datum info (possibly none)
	 * @throws VerticalDatumException if multiple vertical datums are encountered
	 */
	public void normalizeVerticalDatumInfo() throws VerticalDatumException {
		if (abstractRatingContainers != null && abstractRatingContainers.length > 1) {
			VerticalDatumContainer vdc = null;
			for (int i = 0; i < abstractRatingContainers.length; ++i) {
				if (abstractRatingContainers[i].vdc != null) {
					if (vdc == null) {
						vdc = abstractRatingContainers[i].vdc;
						break;
					}
				}
			}
			for (int i = 0; i < abstractRatingContainers.length; ++i) {
				if (abstractRatingContainers[i].vdc == null) {
					vdc = abstractRatingContainers[i].vdc = vdc;
				}
				else if (!abstractRatingContainers[i].vdc.equals(vdc)) {
						throw new VerticalDatumException("Object contains multiple vertical datums");
				}
			}
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNativeVerticalDatum()
	 */
	@Override
	public String getNativeVerticalDatum() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getNativeVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getCurrentVerticalDatum()
	 */
	@Override
	public String getCurrentVerticalDatum() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getCurrentVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isCurrentVerticalDatumEstimated()
	 */
	@Override
	public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].isCurrentVerticalDatumEstimated();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toNativeVerticalDatum()
	 */
	@Override
	public boolean toNativeVerticalDatum() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		boolean change = false;
		for (int i = 0; i < abstractRatingContainers.length; ++i) {
			if (abstractRatingContainers[i].toNativeVerticalDatum()) {
				change = true;
			}
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toNGVD29()
	 */
	@Override
	public boolean toNGVD29() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		boolean change = false;
		for (int i = 0; i < abstractRatingContainers.length; ++i) {
			if (abstractRatingContainers[i].toNGVD29()) {
				change = true;
			}
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toNAVD88()
	 */
	@Override
	public boolean toNAVD88() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		boolean change = false;
		for (int i = 0; i < abstractRatingContainers.length; ++i) {
			if (abstractRatingContainers[i].toNAVD88()) {
				change = true;
			}
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toVerticalDatum(java.lang.String)
	 */
	@Override
	public boolean toVerticalDatum(String datum) throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		boolean change = false;
		for (int i = 0; i < abstractRatingContainers.length; ++i) {
			if (abstractRatingContainers[i].toVerticalDatum(datum)) {
				change = true;
			}
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getCurrentOffset()
	 */
	@Override
	public double getCurrentOffset() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getCurrentOffset();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getCurrentOffset(java.lang.String)
	 */
	@Override
	public double getCurrentOffset(String unit) throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getCurrentOffset(unit);
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNGVD29Offset()
	 */
	@Override
	public double getNGVD29Offset() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getNGVD29Offset();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNGVD29Offset(java.lang.String)
	 */
	@Override
	public double getNGVD29Offset(String unit) throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getNGVD29Offset(unit);
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNAVD88Offset()
	 */
	@Override
	public double getNAVD88Offset() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getNAVD88Offset();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNAVD88Offset(java.lang.String)
	 */
	@Override
	public double getNAVD88Offset(String unit) throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getNAVD88Offset(unit);
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isNGVD29OffsetEstimated()
	 */
	@Override
	public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].isNGVD29OffsetEstimated();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isNAVD88OffsetEstimated()
	 */
	@Override
	public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].isNAVD88OffsetEstimated();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getVerticalDatumInfo()
	 */
	@Override
	public String getVerticalDatumInfo() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getVerticalDatumInfo();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#setVerticalDatumInfo(java.lang.String)
	 */
	@Override
	public void setVerticalDatumInfo(String xmlStr) throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		for (int i = 0; i < abstractRatingContainers.length; ++i) {
			abstractRatingContainers[i].setVerticalDatumInfo(xmlStr);
		}
	}
}
