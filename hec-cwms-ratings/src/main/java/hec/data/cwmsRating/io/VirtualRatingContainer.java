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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hec.data.RatingException;
import hec.data.RatingRuntimeException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.RatingSetXmlParser;
import hec.data.cwmsRating.RatingSpec;
import hec.data.cwmsRating.VirtualRating;
import hec.util.TextUtil;
import mil.army.usace.hec.metadata.VerticalDatumException;

import org.jdom.Element;
import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;

/**
 *
 * @author Mike Perryman
 */
public class VirtualRatingContainer extends AbstractRatingContainer {

	public transient String[] sourceRatingIds = null;
	/**
	 * Contains rating to connect together to form virtual rating
	 */
	public SourceRatingContainer[] sourceRatings = null;

	/**
	 * String specifying how ratings are connected.
	 */
	public String connections = null;
	/**
	 * Public empty constructor
	 */
	public VirtualRatingContainer() {}
	/**
	 * Public constructor from a JDOM Element. The connections and sourceRatings fields will be null
	 * @param ratingElement The JDOM Element
	 * @throws RatingException
	 */
	public VirtualRatingContainer(Element ratingElement) throws RatingException {
		populateFromXml(ratingElement);
	}
	/**
	 * Public constructor from an XML snippet. The connections and sourceRatings fields will be null
	 * @param xmlText The XML snippet
	 * @throws RatingException any issues with processing the XML data.
	 */
	public VirtualRatingContainer(String xmlText) throws RatingException {
		populateFromXml(xmlText);
	}
	/**
	 * Populates the VirtualRatingContainer from a JDOM Element. The connections and sourceRatings fields will be null
	 * @param ratingElement The JDOM Element
	 * @throws RatingException any issues with processing the XML data.
	 */
	public void populateFromXml(Element ratingElement) throws RatingException {
		try {
			AbstractRatingContainer.populateCommonDataFromXml(ratingElement, this);
		}
		catch (VerticalDatumException e) {
			throw new RatingException(e);
		}
	}
	/**
	 * Populates the VirtualRatingContainer from an XML snippet. The connections and sourceRatings fields will be null
	 * @param xmlText The XML snippet
	 * @throws RatingException
	 */
	public void populateFromXml(String xmlText) throws RatingException {
		RatingSetContainer rsc = RatingSetXmlParser.parseString(xmlText);
		if (rsc.abstractRatingContainers.length == 1 && rsc.abstractRatingContainers[0] instanceof VirtualRatingContainer) {
			rsc.abstractRatingContainers[0].clone(this);
		}
		else {
			throw new RatingException("XML text does not specify an VirtualRating object.");
		}
	}
	/**
	 * Populates the source ratings of this object from the soureRatingIds field and input parameters
	 *
	 * @param ratings A collection of ratings that includes the necessary source ratings
	 * @param specs A collection of rating specifications for the source ratings
	 * @param templates A collection of rating templates for the source ratings.
	 * @throws RatingException
	 */
	public void populateSourceRatings(
			Map<String, SortedSet<AbstractRatingContainer>> ratings,
			Map<String, RatingSpecContainer> specs,
			Map<String, RatingTemplateContainer> templates) throws RatingException {

		List<SourceRatingContainer> srList = new ArrayList<SourceRatingContainer>();
		List<String> specsToUse = new ArrayList<String>();
		Map<String, String>unitsBySpecId = new HashMap<String, String>();
		for (String specId : sourceRatingIds) {
			String[] parts = TextUtil.split(specId, "{");
			specId = parts[0].trim();
			unitsBySpecId.put(specId, TextUtil.split(parts[1], "}")[0].trim());
			specsToUse.add(specId);
		}
		for (String specId : specsToUse) {
			SourceRatingContainer src = new SourceRatingContainer();
			srList.add(src);
			if (RatingSpec.isValidRatingSpecId(specId)) {
				RatingSpecContainer rspc = new RatingSpecContainer();
				String[] parts = TextUtil.split(specId, SEPARATOR1);
				String templateId = TextUtil.join(SEPARATOR1, parts[1], parts[2]);
				specs.get(specId).clone(rspc);
				templates.get(templateId).clone(rspc);
				RatingSetContainer rsc = new RatingSetContainer();
				rsc.ratingSpecContainer = rspc;
				rsc.abstractRatingContainers = ratings.get(specId).toArray(new AbstractRatingContainer[0]);
				for (AbstractRatingContainer arc : rsc.abstractRatingContainers) {
					if (arc instanceof VirtualRatingContainer) {
						VirtualRatingContainer vrc = (VirtualRatingContainer)arc;
						vrc.populateSourceRatings(ratings, specs, templates);
					}
					else if (arc instanceof TransitionalRatingContainer) {
						TransitionalRatingContainer trrc = (TransitionalRatingContainer)arc;
						trrc.populateSourceRatings(ratings, specs, templates);
					}
				}
				src.rsc = rsc;
			}
			else {
				src.mathExpression = TextUtil.split(specId, "/", 2)[1].trim();
			}
			String units = unitsBySpecId.get(specId);
			units = units.replace(SEPARATOR2, SEPARATOR3);
			String[] parts = TextUtil.split(units, SEPARATOR3);
			src.units = parts;
		}
		sourceRatings = srList.toArray(new SourceRatingContainer[0]);
		int indParamCount = TextUtil.split(ratingSpecId, SEPARATOR3).length;
		String[] units = new String[indParamCount+1];
		Arrays.fill(units, null);
		//                                  groups   1 2       3     4 5
		Pattern connectionPattern = Pattern.compile("(I(\\d+)|R(\\d+)(I(\\d+)|D)|D)");
		Matcher[] m = new Matcher[2];
		HashSet<String> connected = new HashSet<String>();
		//-------------------------------------------------------//
		// first get the units of specified external connections //
		//-------------------------------------------------------//
		for (String connectionPair : TextUtil.split(connections, ",")) {
			String[] parts = TextUtil.split(connectionPair, "=");
			if (parts.length != 2) {
				throw new RatingException("Invalid connection string: "+connections);
			}
			connected.add(parts[0]);
			connected.add(parts[1]);
			m[0] = connectionPattern.matcher(parts[0]);
			m[1] = connectionPattern.matcher(parts[1]);
			if (!m[0].matches() || !m[1].matches()) {
				throw new RatingException("Invalid connection string: "+connections);
			}
			int matchedIdx = m[0].group(1).charAt(0) == 'I' ? 0 : m[1].group(1).charAt(0) == 'I' ? 1 : -1;
			if (matchedIdx != -1) {
				//-------------------------------------------//
				// external independent connection specified //
				//-------------------------------------------//
				int inputIdx = Integer.parseInt(m[matchedIdx].group(2));
				int connectedIdx = (matchedIdx + 1) % 2;
				if (m[connectedIdx].group(1).charAt(0) != 'R') {
					throw new RatingException("Invalid connection string: "+connections);
				}
				int ratingIdx = Integer.parseInt(m[connectedIdx].group(3));
				switch (m[connectedIdx].group(4).charAt(0)) {
				case 'I' :
					int ratingInput = Integer.parseInt(m[connectedIdx].group(5));
					units[inputIdx-1] = sourceRatings[ratingIdx-1].units[ratingInput-1];
					break;
				case 'D' :
					units[inputIdx-1] = sourceRatings[ratingIdx-1].units[sourceRatings[ratingIdx-1].units.length-1];
					break;
				default :
					throw new RatingException("Invalid connection string: "+connections);
				}
			}
			else {
				matchedIdx = m[0].group(1).charAt(0) == 'D' ? 0 : m[1].group(1).charAt(0) == 'D' ? 1 : -1;
				if (matchedIdx != -1) {
					//-----------------------------------------//
					// external dependent connection specified //
					//-----------------------------------------//
					int connectedIdx = (matchedIdx + 1) % 2;
					if (m[connectedIdx].group(1).charAt(0) != 'R') {
						throw new RatingException("Invalid connection string: "+connections);
					}
					int ratingIdx = Integer.parseInt(m[connectedIdx].group(3));
					switch (m[connectedIdx].group(4).charAt(0)) {
					case 'I' :
						int ratingInput = Integer.parseInt(m[connectedIdx].group(5));
						units[indParamCount] = sourceRatings[ratingIdx-1].units[ratingInput-1];
						break;
					case 'D' :
						units[indParamCount] = sourceRatings[ratingIdx-1].units[sourceRatings[ratingIdx-1].units.length-1];
						break;
					default :
						throw new RatingException("Invalid connection string: "+connections);
					}
				}
			}
		}
		//------------------------------------------------//
		// now populate unassigned units in default order //
		//------------------------------------------------//
		LinkedList<Integer> unassignedUnits = new LinkedList<Integer>();
		for (int i = 0; i < units.length; ++i) {
			if (units[i] == null) {
				unassignedUnits.add(i);
			}
		}
		indUnits:
		for (int i = 0; i < sourceRatings.length; ++i) {
			for (int j = 0; j < sourceRatings[i].units.length - 1; ++j) {
				if (!connected.contains("R"+(i+1)+"I"+(j+1))) {
					units[unassignedUnits.get(0)] = sourceRatings[i].units[j];
					unassignedUnits.remove();
					if (unassignedUnits.peek() == null) {
						break indUnits;
					}
				}
			}
		}
		if (unassignedUnits.peek() != null) {
			for (int i = 0; i < sourceRatings.length; ++i) {
				if (!connected.contains("R"+(i+1)+"D")) {
					units[unassignedUnits.get(0)] = sourceRatings[i].units[sourceRatings[i].units.length-1];
					if (unassignedUnits.peek() == null) {
						break;
					}
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < units.length; ++i) {
			sb.append(i == 0 ? "" : i == units.length-1 ? ";" : ",").append(units[i]);
		}
		unitsId = sb.toString();
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#clone(hec.data.cwmsRating.io.AbstractRatingContainer)
	 */
	@Override
	public void clone(AbstractRatingContainer other) {
		if (!(other instanceof VirtualRatingContainer)) {
			throw new IllegalArgumentException("Clone-to object must be a VirtualRatingContainer.");
		}
		VirtualRatingContainer vrc = (VirtualRatingContainer)other;
		super.clone(vrc);
		vrc.connections = connections;
		if (sourceRatings != null) {
			vrc.sourceRatings = new SourceRatingContainer[sourceRatings.length];
			for (int i = 0; i < sourceRatings.length; ++i) {
				vrc.sourceRatings[i] = new SourceRatingContainer();
				sourceRatings[i].clone(vrc.sourceRatings[i]);
			}
		}
		else if (sourceRatingIds != null) {
			vrc.sourceRatingIds = new String[sourceRatingIds.length];
			for (int i = 0; i < sourceRatingIds.length; ++i) {
				vrc.sourceRatingIds[i] = sourceRatingIds[i];
			}
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = obj == this;
		if (!result) {
			test:
				do {
					if (obj == null || obj.getClass() != getClass()) break;
					if (!super.equals(obj)) break;
					VirtualRatingContainer other = (VirtualRatingContainer)obj;
					if ((other.connections == null) != (connections == null)) break;
					if (!other.connections.equals(connections)) break;
					if ((other.sourceRatings == null) != (sourceRatings == null)) break;
					if (sourceRatings != null) {
						for (int i = 0; i < sourceRatings.length; ++i) {
							if ((other.sourceRatings[i] == null) != (sourceRatings[i] == null)) break test;
							if (sourceRatings[i] != null) {
								if (!other.sourceRatings[i].equals(sourceRatings[i])) break test;
							}
						}
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
		int hashCode = getClass().getName().hashCode() + super.hashCode() + 3 * (connections == null ? 1 : connections.hashCode());
		if (sourceRatings == null) {
			hashCode += 5;
		}
		else {
			hashCode += 7 * sourceRatings.length;
			for (int i = 0; i < sourceRatings.length; ++i) {
				hashCode += 11 * (sourceRatings[i] == null ? i+1 : sourceRatings[i].hashCode());
			}
		}
		return hashCode;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#clone()
	 */
	@Override
	public AbstractRatingContainer clone() {
		VirtualRatingContainer vrc = new VirtualRatingContainer();
		this.clone(vrc);
		return vrc;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#getInstance()
	 */
	@Override
	public AbstractRatingContainer getInstance() {
		return new VirtualRatingContainer();
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#newRating()
	 */
	@Override
	public AbstractRating newRating() throws RatingException {
		VirtualRating rating = new VirtualRating(this);
		return rating;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#toNativeVerticalDatum()
	 */
	@Override
	public boolean toNativeVerticalDatum() throws VerticalDatumException {
		for (SourceRatingContainer src : sourceRatings) {
			if (src.rsc != null) {
				for (AbstractRatingContainer arc : src.rsc.abstractRatingContainers) {
					if (arc.vdc != null) {
						arc.toNativeVerticalDatum();
					}
				}
			}
		}
		return super.toNativeVerticalDatum();
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#toNGVD29()
	 */
	@Override
	public boolean toNGVD29() throws VerticalDatumException {
		for (SourceRatingContainer src : sourceRatings) {
			if (src.rsc != null) {
				for (AbstractRatingContainer arc : src.rsc.abstractRatingContainers) {
					if (arc.vdc != null) {
						arc.toNGVD29();
					}
				}
			}
		}
		return super.toNGVD29();
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#toNAVD88()
	 */
	@Override
	public boolean toNAVD88() throws VerticalDatumException {
		for (SourceRatingContainer src : sourceRatings) {
			if (src.rsc != null) {
				for (AbstractRatingContainer arc : src.rsc.abstractRatingContainers) {
					if (arc.vdc != null) {
						arc.toNAVD88();
					}
				}
			}
		}
		return super.toNAVD88();
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#toVerticalDatum(java.lang.String)
	 */
	@Override
	public boolean toVerticalDatum(String datum) throws VerticalDatumException {
		for (SourceRatingContainer src : sourceRatings) {
			if (src.rsc != null) {
				for (AbstractRatingContainer arc : src.rsc.abstractRatingContainers) {
					if (arc.vdc != null) {
						arc.toVerticalDatum(datum);
					}
				}
			}
		}
		return super.toVerticalDatum(datum);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#addOffset(int, double)
	 */
	@Override
	public void addOffset(int paramNum, double offset) throws RatingException {
		throw new RatingException("The addOffset(int, double) method is not available for virtual ratings");
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#setVerticalDatumInfo(java.lang.String)
	 */
	@Override
	public void setVerticalDatumInfo(String xmlStr)
			throws VerticalDatumException {
		for (SourceRatingContainer src : sourceRatings) {
			if (src.rsc != null) {
				for (AbstractRatingContainer arc : src.rsc.abstractRatingContainers) {
					if (arc.vdc != null) {
						arc.setVerticalDatumInfo(xmlStr);
					}
				}
			}
		}
		super.setVerticalDatumInfo(xmlStr);
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
				VirtualRatingContainer _clone = (VirtualRatingContainer)this.clone();
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
			SortedSet<String> templateXmlStrings = new TreeSet<String>();
			SortedSet<String> specXmlStrings = new TreeSet<String>();
			List<String> ratingXmlStrings = new ArrayList<String>();
			getSoucreRatingsXml(indent, level+1, templateXmlStrings, specXmlStrings, ratingXmlStrings);
			for (String templateXml : templateXmlStrings) {
				sb.append(templateXml);
			}
			for (String specXml : specXmlStrings) {
				sb.append(specXml);
			}
			for (String specXml : ratingXmlStrings) {
				sb.append(specXml);
			}
			prefix += indent;
		}
		sb.append(super.toXml(prefix, indent, "virtual-rating"));
		if (connections == null || connections.length() == 0) {
			sb.append(prefix).append(indent).append("<connections/>\n");
		}
		else {
			sb.append(prefix).append(indent).append("<connections>").append(connections).append("</connections>\n");
		}
		if (sourceRatings == null || sourceRatings.length == 0) {
			sb.append(prefix).append(indent).append("<source-ratings/>\n");
		}
		else {
			sb.append(prefix).append(indent).append("<source-ratings>\n");
			StringBuilder sourceRatingUnits = new StringBuilder();
			for (int i = 0; i < sourceRatings.length; ++i) {
				sourceRatingUnits.setLength(0);
				sourceRatingUnits.append(" {");
				for (int j = 0; j < sourceRatings[i].units.length; ++j) {
					sourceRatingUnits.append(j == 0 ? "" : j == sourceRatings[i].units.length-1 ? ";" : ",")
					                 .append(TextUtil.xmlEntityEncode(sourceRatings[i].units[j]));
				}
				sourceRatingUnits.append("}");
				sb.append(prefix).append(indent).append(indent).append("<source-rating position=\""+(i+1)+"\">\n");
				if (sourceRatings[i].rsc == null) {
					sb.append(prefix).append(indent).append(indent).append(indent)
					  .append("<rating-expression>").append(TextUtil.xmlEntityEncode(sourceRatings[i].mathExpression))
					  .append(sourceRatingUnits.toString())
					  .append("</rating-expression>\n");
				}
				else {
					sb.append(prefix).append(indent).append(indent).append(indent)
					  .append("<rating-spec-id>").append(TextUtil.xmlEntityEncode(sourceRatings[i].rsc.ratingSpecContainer.specId))
					  .append(sourceRatingUnits.toString())
					  .append("</rating-spec-id>\n");
				}
				sb.append(prefix).append(indent).append(indent).append("</source-rating>\n");
			}
			sb.append(prefix).append(indent).append("</source-ratings>\n");
		}
		sb.append(prefix).append("</virtual-rating>\n");
		if (level == 0) {
			sb.append("</ratings>\n");
		}
		return sb.toString();
	}

	public void getSoucreRatingsXml(CharSequence indent, int level, Set<String> templateStrings, Set<String> specStrings, List<String> ratingStrings) {
		if (sourceRatings != null) {
			for (SourceRatingContainer src : sourceRatings) {
				if (src.rsc != null) {
					templateStrings.add(src.rsc.ratingSpecContainer.toTemplateXml(indent, level));
					specStrings.add(src.rsc.ratingSpecContainer.toSpecXml(indent, level));
					for (AbstractRatingContainer arc : src.rsc.abstractRatingContainers) {
						ratingStrings.add(arc.toXml(indent, level));
					}
					if (src.rsc.abstractRatingContainers[0] instanceof VirtualRatingContainer) {
						VirtualRatingContainer vrc = (VirtualRatingContainer)src.rsc.abstractRatingContainers[0];
						vrc.getSoucreRatingsXml(indent, level, templateStrings, specStrings, ratingStrings);
					}
					else if (src.rsc.abstractRatingContainers[0] instanceof TransitionalRatingContainer) {
						TransitionalRatingContainer trc = (TransitionalRatingContainer)src.rsc.abstractRatingContainers[0];
						trc.getSoucreRatingsXml(indent, level, templateStrings, specStrings, ratingStrings);
					}
				}
			}
		}
	}
}
