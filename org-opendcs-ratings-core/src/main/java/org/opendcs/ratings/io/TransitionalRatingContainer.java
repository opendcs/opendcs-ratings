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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.AbstractRating;
import org.opendcs.ratings.TransitionalRating;
import hec.util.TextUtil;
import mil.army.usace.hec.metadata.VerticalDatumException;
import org.w3c.dom.Element;

import static org.opendcs.ratings.RatingConst.SEPARATOR1;
import static org.opendcs.ratings.RatingConst.SEPARATOR2;
import static org.opendcs.ratings.RatingConst.SEPARATOR3;

/**
 *
 * @author Mike Perryman
 */
public class TransitionalRatingContainer extends AbstractRatingContainer {

	public transient String[] sourceRatingIds = null;
	/**
	 * Contains the conditions to match
	 */
	public String[] conditions = null;
	/**
	 * Contains the evaluation expressions - one for each condition plus the default one.
	 */
	public String[] evaluations = null;
	/**
	 * Contains rating to connect together to form virtual rating
	 */
	public SourceRatingContainer[] sourceRatings = null;
	/**
	 * Public empty constructor
	 */
	public TransitionalRatingContainer() {}
	/**
	 * Public constructor from a DOM Element. The conditions, evaluations, and sourceRatings fields will be null
	 * @param ratingElement The DOM Element
	 * @throws RatingException on error
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#transitionalRatingContainer(Element) instead
	 */
	@Deprecated
	public TransitionalRatingContainer(Element ratingElement) throws RatingException {
		populateFromXml(ratingElement);
	}
	/**
	 * Public constructor from an XML snippet. The conditions, evaluations, and sourceRatings fields will be null
	 * @param xmlText The XML snippet
	 * @throws RatingException on error
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#transitionalRatingContainer(String) instead
	 */
	@Deprecated
	public TransitionalRatingContainer(String xmlText) throws RatingException {
		populateFromXml(xmlText);
	}
	/**
	 * Populates the TransitionalRatingContainer from a DOM Element. The conditions, evaluations, and sourceRatings fields will be null
	 * @param ratingElement The DOM element
	 * @throws RatingException on error
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#transitionalRatingContainer(Element) instead
	 */
	@Deprecated
	public void populateFromXml(Element ratingElement) throws RatingException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		TransitionalRatingContainer transitionalRatingContainer = service.createTransitionalRatingContainer(ratingElement);
		transitionalRatingContainer.clone(this);
	}
	/**
	 * Populates the TransitionalRatingContainer from an XML snippet. The conditions, evaluations, and sourceRatings fields will be null
	 * @param xmlText The XML snippet
	 * @throws RatingException in error
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#transitionalRatingContainer(String) instead
	 */
	@Deprecated
	public void populateFromXml(String xmlText) throws RatingException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		TransitionalRatingContainer transitionalRatingContainer = service.createTransitionalRatingContainer(xmlText);
		transitionalRatingContainer.clone(this);
	}
	/**
	 * Populates the source ratings of this object from the soureRatingIds field and input parameters
	 *
	 * @param ratings A collection of ratings that includes the necessary source ratings
	 * @param specs A collection of rating specifications for the source ratings
	 * @param templates A collection of rating templates for the source ratings.
	 * @throws RatingException on error
	 */
	public void populateSourceRatings(
			Map<String, SortedSet<AbstractRatingContainer>> ratings,
			Map<String, RatingSpecContainer> specs,
			Map<String, RatingTemplateContainer> templates) throws RatingException {

		if (sourceRatingIds != null) {
			List<SourceRatingContainer> srList = new ArrayList<>();
			for (String specId : sourceRatingIds) {
				SourceRatingContainer src = new SourceRatingContainer();
				srList.add(src);
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
				src.units = TextUtil.split(src.rsc.abstractRatingContainers[0].unitsId.replace(SEPARATOR2, SEPARATOR3), SEPARATOR3);
			}
			sourceRatings = srList.toArray(new SourceRatingContainer[0]);
		}
	}

	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#clone(hec.data.cwmsRating.io.AbstractRatingContainer)
	 */
	@Override
	public void clone(AbstractRatingContainer other) {
		if (!(other instanceof TransitionalRatingContainer)) {
			throw new IllegalArgumentException("Clone-to object must be a TransitionalRatingContainer.");
		}
		TransitionalRatingContainer trc = (TransitionalRatingContainer)other;
		super.clone(trc);
		if (conditions != null) {
			trc.conditions = Arrays.copyOf(conditions, conditions.length);
		}
		if (evaluations != null) {
			trc.evaluations = Arrays.copyOf(evaluations, evaluations.length);
		}
		if (sourceRatings != null) {
			trc.sourceRatings = new SourceRatingContainer[sourceRatings.length];
			for (int i = 0; i < sourceRatings.length; ++i) {
				trc.sourceRatings[i] = new SourceRatingContainer();
				sourceRatings[i].clone(trc.sourceRatings[i]);
			}
		}
		if (sourceRatingIds != null) {
			trc.sourceRatingIds = new String[sourceRatingIds.length];
            System.arraycopy(sourceRatingIds, 0, trc.sourceRatingIds, 0, sourceRatingIds.length);
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
		TransitionalRatingContainer that = (TransitionalRatingContainer) o;
		return Arrays.equals(sourceRatingIds, that.sourceRatingIds) && Arrays.equals(conditions, that.conditions) &&
			Arrays.equals(evaluations, that.evaluations) && Arrays.equals(sourceRatings, that.sourceRatings);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + Arrays.hashCode(sourceRatingIds);
		result = 31 * result + Arrays.hashCode(conditions);
		result = 31 * result + Arrays.hashCode(evaluations);
		result = 31 * result + Arrays.hashCode(sourceRatings);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#clone()
	 */
	@Override
	public AbstractRatingContainer clone() {
        TransitionalRatingContainer trc = new TransitionalRatingContainer();
		this.clone(trc);
		return trc;
	}

	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#getInstance()
	 */
	@Override
	public AbstractRatingContainer getInstance() {
		return new TransitionalRatingContainer();
	}

	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#newRating()
	 */
	@Override
	public AbstractRating newRating() throws RatingException {
		return new TransitionalRating(this);
	}

	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#toNativeVerticalDatum()
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
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#toNGVD29()
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
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#toNAVD88()
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
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#toVerticalDatum(java.lang.String)
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
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#addOffset(int, double)
	 */
	@Override
	public void addOffset(int paramNum, double offset) throws RatingException {
		for (SourceRatingContainer src : sourceRatings) {
			if (src.rsc != null) {
				for (AbstractRatingContainer arc : src.rsc.abstractRatingContainers) {
					if (arc.vdc != null) {
						arc.addOffset(paramNum, offset);
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#setVerticalDatumInfo(java.lang.String)
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
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#toXml(TransitionalRatingContainer, CharSequence, int) instead
	 */
	@Deprecated
	@Override
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}

	/**
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#toXml(TransitionalRatingContainer, CharSequence, int) instead
	 */
	@Deprecated
	@Override
	public String toXml(CharSequence indent, int level) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.toXml(this, indent, level);
	}

	/**
	 *
	 * @deprecated will be removed as this should be internal API only
	 */
	@Deprecated
	public void getSoucreRatingsXml(CharSequence indent, int level, Set<String> templateStrings, Set<String> specStrings, List<String> ratingStrings) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		service.getSourceRatingsXml(this, indent, level, templateStrings, specStrings, ratingStrings);
	}

}
