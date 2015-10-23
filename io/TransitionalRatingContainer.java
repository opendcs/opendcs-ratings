/**
 * 
 */
package hec.data.cwmsRating.io;

import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import hec.data.RatingException;
import hec.data.VerticalDatumException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.TransitionalRating;
import hec.util.TextUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
	 * Populates the source ratings of this object from the soureRatingIds field and input parameters
	 * 
	 * @param ratings A collection of ratings that includes the necessary source ratings
	 * @param specs A collection of rating specifications for the source ratings
	 * @param temlates A collection of rating templates for the source ratings.
	 */
	public void populateSourceRatings(
			Map<String, SortedSet<AbstractRatingContainer>> ratings, 
			Map<String, RatingSpecContainer> specs, 
			Map<String, RatingTemplateContainer> templates) {
		
		if (sourceRatingIds != null) {
			List<SourceRatingContainer> srList = new ArrayList<SourceRatingContainer>();
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
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#clone(hec.data.cwmsRating.io.AbstractRatingContainer)
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
		else if (sourceRatingIds != null) {
			trc.sourceRatingIds = new String[sourceRatingIds.length];
			for (int i = 0; i < sourceRatingIds.length; ++i) {
				trc.sourceRatingIds[i] = sourceRatingIds[i];
			}
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#clone()
	 */
	@Override
	public AbstractRatingContainer clone() {
		TransitionalRatingContainer trc = new TransitionalRatingContainer();
		this.clone(trc);
		return trc;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#getInstance()
	 */
	@Override
	public AbstractRatingContainer getInstance() {
		return new TransitionalRatingContainer();
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#newRating()
	 */
	@Override
	public AbstractRating newRating() throws RatingException {
		return new TransitionalRating(this);
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
				TransitionalRatingContainer _clone = (TransitionalRatingContainer)this.clone();
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
		sb.append(super.toXml(prefix, indent, "transitional-rating"));
		sb.append(prefix).append(indent).append("<select>\n");
		for (int i = 0; i < conditions.length; ++i) {
			sb.append(prefix).append(indent).append(indent).append("<case position=\""+(i+1)+"\">\n");
			sb.append(prefix).append(indent).append(indent).append(indent).append(String.format("<when>%s</when>\n", TextUtil.xmlEntityEncode(conditions[i])));
			sb.append(prefix).append(indent).append(indent).append(indent).append(String.format("<then>%s</then>\n", TextUtil.xmlEntityEncode(evaluations[i])));
			sb.append(prefix).append(indent).append(indent).append("</case>\n");
		}
		sb.append(prefix).append(indent).append(indent).append(String.format("<default>%s</default>\n", TextUtil.xmlEntityEncode(evaluations[evaluations.length-1])));
		sb.append(prefix).append(indent).append("</select>\n");
		if (sourceRatings == null) {
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
				sb.append(prefix).append(indent).append(indent).append("<rating-spec-id position=\""+(i+1)+"\">\n");
				sb.append(prefix).append(indent).append(indent).append(indent).append(TextUtil.xmlEntityEncode(sourceRatings[i].rsc.ratingSpecContainer.specId)).append("\n");
				sb.append(prefix).append(indent).append(indent).append("</rating-spec-id>\n");
			}
			sb.append(prefix).append(indent).append("</source-ratings>\n");
		}
		sb.append(prefix).append("</transitional-rating>\n");
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
