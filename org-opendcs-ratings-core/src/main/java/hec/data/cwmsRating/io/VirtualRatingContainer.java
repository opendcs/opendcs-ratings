/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package org.opendcs.ratings.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.AbstractRating;
import org.opendcs.ratings.RatingSpec;
import org.opendcs.ratings.VirtualRating;
import hec.util.TextUtil;
import mil.army.usace.hec.metadata.VerticalDatumException;

import org.jdom.Element;

import static org.opendcs.ratings.RatingConst.SEPARATOR1;
import static org.opendcs.ratings.RatingConst.SEPARATOR2;
import static org.opendcs.ratings.RatingConst.SEPARATOR3;

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
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#virtualRatingContainer(Element) instead
	 */
	@Deprecated
	public VirtualRatingContainer(Element ratingElement) throws RatingException {
		populateFromXml(ratingElement);
	}

	/**
	 * Public constructor from an XML snippet. The connections and sourceRatings fields will be null
	 * @param xmlText The XML snippet
	 * @throws RatingException any issues with processing the XML data.
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#virtualRatingContainer(Element) instead
	 */
	@Deprecated
	public VirtualRatingContainer(String xmlText) throws RatingException {
		populateFromXml(xmlText);
	}

	/**
	 * Populates the VirtualRatingContainer from a JDOM Element. The connections and sourceRatings fields will be null
	 * @param ratingElement The JDOM Element
	 * @throws RatingException any issues with processing the XML data.
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#virtualRatingContainer(Element) instead
	 */
	@Deprecated
	public void populateFromXml(Element ratingElement) throws RatingException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		VirtualRatingContainer virtualRatingContainer = service.createVirtualRatingContainer(ratingElement);
		virtualRatingContainer.clone(this);
	}
	/**
	 * Populates the VirtualRatingContainer from an XML snippet. The connections and sourceRatings fields will be null
	 * @param xmlText The XML snippet
	 * @throws RatingException
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#virtualRatingContainer(String) instead
	 */
	@Deprecated
	public void populateFromXml(String xmlText) throws RatingException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		VirtualRatingContainer virtualRatingContainer = service.createVirtualRatingContainer(xmlText);
		virtualRatingContainer.clone(this);
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
		if (sourceRatingIds != null) {
			vrc.sourceRatingIds = new String[sourceRatingIds.length];
			for (int i = 0; i < sourceRatingIds.length; ++i) {
				vrc.sourceRatingIds[i] = sourceRatingIds[i];
			}
		}
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		VirtualRatingContainer that = (VirtualRatingContainer) o;
		return Arrays.equals(sourceRatingIds, that.sourceRatingIds) && Arrays.equals(sourceRatings, that.sourceRatings) &&
			Objects.equals(connections, that.connections);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(super.hashCode(), connections);
		result = 31 * result + Arrays.hashCode(sourceRatingIds);
		result = 31 * result + Arrays.hashCode(sourceRatings);
		return result;
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

	/**
	 *
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#toXml(VirtualRatingContainer, CharSequence, int) instead
	 */
	@Override
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}

	/**
	 *
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#toXml(VirtualRatingContainer, CharSequence, int) instead
	 */
	@Override
	public String toXml(CharSequence indent, int level) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.toXml(this, indent, level);
	}

	/**
	 *
	 * @deprecated will be removed as this should be internal API only
	 */
	public void getSoucreRatingsXml(CharSequence indent, int level, Set<String> templateStrings, Set<String> specStrings, List<String> ratingStrings) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		service.getSourceRatingsXml(this, indent, level, templateStrings, specStrings, ratingStrings);
	}
}
