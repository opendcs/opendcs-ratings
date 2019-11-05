package hec.data.cwmsRating.io;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jdom.Element;

import hec.data.IVerticalDatum;
import hec.data.RatingException;
import hec.data.VerticalDatumException;
import hec.data.cwmsRating.RatingSetXmlParser;
import hec.data.cwmsRating.RatingUtil;
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
	
	public RatingSetStateContainer state = null;
	/**
	 * Public empty Constructor
	 */
	public RatingSetContainer() {}
	/**
	 * Public constructor from a JDOM Element.
	 * @param ratingElement The JDOM Element. The document (root) node is expected to be
	 *        &lt;ratings&gt;, which is expected to have one or more &lt;rating&gt; or &lt;usgs-stream-rating&gt; child nodes, all of the same
	 *        rating specification.  Appropriate <rating-template> and &lt;rating-spec&gt; nodes are required for the rating set;
	 *        any other template and specification nodes are ignored.
	 */
	public RatingSetContainer(Element ratingElement) throws RatingException {
		populateFromXml(ratingElement);
	}
	/**
	 * Public constructor from an XML instance.
	 * @param xmlText The XML instance to construct the RatingSet object from. The document (root) node is expected to be
	 *        &lt;ratings&gt;, which is expected to have one or more &lt;rating&gt; or &lt;usgs-stream-rating&gt; child nodes, all of the same
	 *        rating specification.  Appropriate <rating-template> and &lt;rating-spec&gt; nodes are required for the rating set;
	 *        any other template and specification nodes are ignored.
	 * @throws RatingException
	 */
	public RatingSetContainer(String xmlText) throws RatingException {
		populateFromXml(xmlText);
	}
	/**
	 * Populates this RatingSetContainer object from a JDOM Element.
	 * @param ratingElement The JDOM Element. The document (root) node is expected to be
	 *        &lt;ratings&gt;, which is expected to have one or more &lt;rating&gt; or &lt;usgs-stream-rating&gt; child nodes, all of the same
	 *        rating specification.  Appropriate <rating-template> and &lt;rating-spec&gt; nodes are required for the rating set;
	 *        any other template and specification nodes are ignored.
	 */
	public void populateFromXml(Element ratingElement) throws RatingException {
		populateFromXml(RatingUtil.jdomElementToText(ratingElement));
	}
	/**
	 * Populates this RatingSetContainer object from an XML instance.
	 * @param xmlText The XML instance to construct the RatingSet object from. The document (root) node is expected to be
	 *        &lt;ratings&gt;, which is expected to have one or more &lt;rating&gt; or &lt;usgs-stream-rating&gt; child nodes, all of the same
	 *        rating specification.  Appropriate <rating-template> and &lt;rating-spec&gt; nodes are required for the rating set;
	 *        any other template and specification nodes are ignored.
	 * @throws RatingException
	 */
	public void populateFromXml(String xmlText) throws RatingException {
		RatingSetContainer rsc = RatingSetXmlParser.parseString(xmlText);
		ratingSpecContainer = rsc.ratingSpecContainer;
		abstractRatingContainers = rsc.abstractRatingContainers;
	}
	/**
	 * Creates another RatingSetContainer object and fills it with with data from this one
	 */
	public RatingSetContainer clone() {
		RatingSetContainer rsc  = abstractRatingContainers == null ? new ReferenceRatingContainer() : new RatingSetContainer();
		if (state != null) {
			rsc.state = (RatingSetStateContainer)state.clone();
		}
		rsc.ratingSpecContainer = ratingSpecContainer == null ? null : ratingSpecContainer.clone();
		if (abstractRatingContainers != null) {
			rsc.abstractRatingContainers = new AbstractRatingContainer[abstractRatingContainers.length];
			for (int i = 0; i < abstractRatingContainers.length; ++i) {
				rsc.abstractRatingContainers[i] = abstractRatingContainers[i].clone();
			}
		}
		return rsc;
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
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}

	public String toXml(CharSequence indent, int level) {
		return toXml(indent, level, true);
	}

	public String toXml(CharSequence indent, int level, boolean includeTemplate) {
		return toXml(indent, level, includeTemplate, true);
	}

	public String toXml(CharSequence indent, int level, boolean includeTemplate, boolean includeEmptyTableRatings) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level; ++i) sb.append(indent);
		String prefix = sb.toString();
		sb.delete(0, sb.length());
		List<String> ratingXmlStrings = new ArrayList<String>();
		SortedSet<String> templateXmlStrings = new TreeSet<String>();
		SortedSet<String> specXmlStrings = new TreeSet<String>();
		String thisTemplateXml = ratingSpecContainer.toTemplateXml(indent, level+1);
		if (level == 0) {
			sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			sb.append("<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
			if (abstractRatingContainers != null) {
				for (AbstractRatingContainer arc : abstractRatingContainers) {
					if (arc instanceof VirtualRatingContainer) {
						VirtualRatingContainer vrc = (VirtualRatingContainer)arc;
						vrc.getSoucreRatingsXml(indent, level+1, templateXmlStrings, specXmlStrings, ratingXmlStrings);
					}
					else if (arc instanceof TransitionalRatingContainer) {
						TransitionalRatingContainer trrc = (TransitionalRatingContainer)arc;
						trrc.getSoucreRatingsXml(indent, level+1, templateXmlStrings, specXmlStrings, ratingXmlStrings);
					}
				}
			}
		}
		for (String templateXml : templateXmlStrings) {
			if (!templateXml.equals(thisTemplateXml)) {
				sb.append(templateXml);
			}
		}
		if (ratingSpecContainer != null) {
			sb.append(ratingSpecContainer.toXml(indent, level+1, includeTemplate));
		}
		for (String specXml : specXmlStrings) {
			sb.append(specXml);
		}
		if (abstractRatingContainers != null) {
			for (AbstractRatingContainer arc : abstractRatingContainers) {
				if (includeEmptyTableRatings || !(arc instanceof TableRatingContainer) || ((TableRatingContainer)arc).values != null)
				sb.append(arc.toXml(indent, level+1));
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
	 * @see hec.data.IVerticalDatum#forceVerticalDatum(java.lang.String)
	 */
	@Override
	public boolean forceVerticalDatum(String datum) throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		boolean change = false;
		for (int i = 0; i < abstractRatingContainers.length; ++i) {
			if (abstractRatingContainers[i].forceVerticalDatum(datum)) {
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
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = obj == this;
		if (!result) {
			test:
				do {
					if (obj == null || obj.getClass() != getClass()) break;
					RatingSetContainer other = (RatingSetContainer)obj;
					if ((other.ratingSpecContainer == null) != (ratingSpecContainer == null)) break;
					if (ratingSpecContainer != null) {
						if (!other.ratingSpecContainer.equals(ratingSpecContainer)) break;
					}
					if ((other.abstractRatingContainers == null) != (abstractRatingContainers == null)) break;
					if (abstractRatingContainers != null) {
						if (other.abstractRatingContainers.length != abstractRatingContainers.length) break;
						for (int i = 0; i < abstractRatingContainers.length; ++i) {
							if ((abstractRatingContainers[i] == null) != (other.abstractRatingContainers[i] == null)) break test;
							if (abstractRatingContainers[i] != null) {
								if (!abstractRatingContainers[i].equals(other.abstractRatingContainers[i])) break test;
							}
						}
					}
					result = true;
				} while (false);
		}
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hashCode = getClass().getName().hashCode() + (ratingSpecContainer == null ? 3 : ratingSpecContainer.hashCode());
		if (abstractRatingContainers == null) {
			hashCode += 3;
		}
		else {
			hashCode += 5 * abstractRatingContainers.length;
			for (int i = 0; i < abstractRatingContainers.length; ++i) {
				hashCode += 7 * (abstractRatingContainers[i] == null ? i+1 : abstractRatingContainers[i].hashCode());
			}
		}
		if (state != null) hashCode += state.hashCode();
		return hashCode;
	}

	/**
	 * Returns the VerticalDatumContainer from the first RatingContainer
	 */
	@Override
	public VerticalDatumContainer getVerticalDatumContainer() {
		VerticalDatumContainer retval = null;
		if (abstractRatingContainers != null && abstractRatingContainers.length >= 1) {
			retval = abstractRatingContainers[0].getVerticalDatumContainer();
		}
		return retval;
	}

	/**
	 * Sets the VerticalDatumContainer on the first RatingContainer
	 * @param vdc
	 * @deprecated
	 */
	@Override
	public void setVerticalDatumContainer(VerticalDatumContainer vdc) {
		if (abstractRatingContainers != null && abstractRatingContainers.length >= 1) {
			abstractRatingContainers[0].setVerticalDatumContainer(vdc);
		}
	}
}
