/**
 * 
 */
package hec.data.cwmsRating.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import hec.data.RatingException;
import hec.data.VerticalDatumException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.RatingSpec;
import hec.data.cwmsRating.VirtualRating;
import hec.util.TextUtil;
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
		boolean result = false;
		test:
		do {
			if (!(obj instanceof VirtualRatingContainer)) break;
			VirtualRatingContainer other = (VirtualRatingContainer)obj;
			if (!super.equals(obj)) break;
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
		return result;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#hashCode()
	 */
	@Override
	public int hashCode() {
		int hashCode = getClass().getName().hashCode() + super.hashCode() + connections == null ? 3 : connections.hashCode();
		if (sourceRatings == null) {
			hashCode += 7;
		}
		else {
			for (int i = 0; i < sourceRatings.length; ++i) {
				hashCode += (sourceRatings[i] == null ? i+1 : sourceRatings[i].hashCode());
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
		sb.append(super.toXml(prefix, indent, "virtual-rating"));
		sb.append(prefix).append(indent).append("<connections>").append(connections).append("</connections>\n");
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
		sb.append(prefix).append("</virtual-rating>\n");
		if (level == 0) {
			sb.append("</ratings>\n");
		}
		return sb.toString();
	}
	
	public void getSoucreRatingsXml(CharSequence indent, int level, Set<String> templateStrings, Set<String> specStrings, List<String> ratingStrings) {
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
