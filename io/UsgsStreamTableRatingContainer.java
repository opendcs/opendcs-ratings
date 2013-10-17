package hec.data.cwmsRating.io;

import java.util.List;

import hec.data.RatingException;
import hec.heclib.util.HecTime;

import org.jdom.Element;

import static hec.lang.Const.UNDEFINED_TIME;

/**
 * Data container class for UsgsStreamTableRating data
 * @author Mike Perryman
 */
public class UsgsStreamTableRatingContainer extends TableRatingContainer {

	/**
	 * The dated shift adjustments
	 */
	public RatingSetContainer shifts = null;
	/**
	 * The logarithmic interpolation offsets
	 */
	public TableRatingContainer offsets = null;
	
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTableContainer#clone(hec.data.cwmsRating.RatingContainer)
	 */
	@Override
	public void clone(AbstractRatingContainer other) {
		if (!(other instanceof UsgsStreamTableRatingContainer)) {
			throw new IllegalArgumentException("Clone-to object must be a UsgsStreamTableRatingContainer.");
		}
		super.clone(other);
		UsgsStreamTableRatingContainer ustrc = (UsgsStreamTableRatingContainer)other;
		if (shifts == null) {
			ustrc.shifts = null;
		}
		else {
			if (ustrc.shifts == null) ustrc.shifts = new RatingSetContainer();
			shifts.clone(ustrc.shifts);
		}
		if (offsets == null) {
			ustrc.offsets = null;
		}
		else {
			if (ustrc.offsets == null) ustrc.offsets = new TableRatingContainer();
			offsets.clone(ustrc.offsets);
		}
	}
	
	@Override
	public AbstractRatingContainer getInstance()
	{
		return new UsgsStreamTableRatingContainer();
	}
	
	public static UsgsStreamTableRatingContainer fromXml(Element ratingElement) throws RatingException {
		UsgsStreamTableRatingContainer ustrc = new UsgsStreamTableRatingContainer();
		AbstractRatingContainer.fromXml(ratingElement, ustrc);
		Element elem = null;
		@SuppressWarnings("rawtypes")
		List elems = ratingElement.getChildren("height-shifts");
		
		if (elems.size() > 0) {
			HecTime hectime = new HecTime();
			ustrc.shifts = new RatingSetContainer();
			ustrc.shifts.abstractRatingContainers = new TableRatingContainer[elems.size()];
			for (int i = 0; i < elems.size(); ++i) {
				elem =  (Element)elems.get(i);
				String data = elem.getChildTextTrim("effective-date");
				if (data != null) {
					hectime.set(data);
					ustrc.shifts.abstractRatingContainers[i].effectiveDateMillis = hectime.getTimeInMillis();
				}
				data = elem.getChildTextTrim("create-date");
				if (data != null) {
					hectime.set(data);
					ustrc.shifts.abstractRatingContainers[i].createDateMillis = hectime.getTimeInMillis();
				}
				ustrc.shifts.abstractRatingContainers[i].active = Boolean.parseBoolean(elem.getChildTextTrim("active"));
				ustrc.shifts.abstractRatingContainers[i].description = elem.getChildTextTrim("description");
				@SuppressWarnings("rawtypes")
				List pointElems = elem.getChildren("point");
				TableRatingContainer trc = (TableRatingContainer)ustrc.shifts.abstractRatingContainers[i];
				if (pointElems.size() > 0) {
					trc.values = new RatingValueContainer[pointElems.size()];
					for (int j = 0; j < pointElems.size(); ++j) {
						elem = (Element)pointElems.get(j);
						trc.values[j].indValue = Double.parseDouble(elem.getChildTextTrim("ind"));
						trc.values[j].depValue = Double.parseDouble(elem.getChildTextTrim("dep"));
						trc.values[j].note = elem.getChildTextTrim("note");
					}
				}
			}
		}
		elem = ratingElement.getChild("height-offsets");
		if (elem != null) {
			ustrc.offsets = new TableRatingContainer();
			TableRatingContainer trc = ustrc.offsets;
			@SuppressWarnings("rawtypes")
			List pointElems = elem.getChildren("point");
			if (pointElems.size() > 0) {
				trc.values = new RatingValueContainer[pointElems.size()];
				for (int i = 0; i < pointElems.size(); ++i) {
					elem = (Element)pointElems.get(i);
					trc.values[i].indValue = Double.parseDouble(elem.getChildTextTrim("ind"));
					trc.values[i].depValue = Double.parseDouble(elem.getChildTextTrim("dep"));
					trc.values[i].note = elem.getChildTextTrim("note");
				}
			}
		}
		elem = ratingElement.getChild("rating-points");
		if (elem != null) {
			@SuppressWarnings("rawtypes")
			List pointElems = elem.getChildren("point");
			if (pointElems.size() > 0) {
				ustrc.values = new RatingValueContainer[pointElems.size()];
				for (int i = 0; i < pointElems.size(); ++i) {
					elem = (Element)pointElems.get(i);
					ustrc.values[i].indValue = Double.parseDouble(elem.getChildTextTrim("ind"));
					ustrc.values[i].depValue = Double.parseDouble(elem.getChildTextTrim("dep"));
					ustrc.values[i].note = elem.getChildTextTrim("note");
				}
			}
		}
		elem = ratingElement.getChild("extension-points");
		if (elem != null) {
			@SuppressWarnings("rawtypes")
			List pointElems = elem.getChildren("point");
			if (pointElems.size() > 0) {
				ustrc.values = new RatingValueContainer[pointElems.size()];
				for (int i = 0; i < pointElems.size(); ++i) {
					elem = (Element)pointElems.get(i);
					ustrc.values[i].indValue = Double.parseDouble(elem.getChildTextTrim("ind"));
					ustrc.values[i].depValue = Double.parseDouble(elem.getChildTextTrim("dep"));
					ustrc.values[i].note = elem.getChildTextTrim("note");
				}
			}
		}
		return ustrc;
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
		sb.append(super.toXml(prefix, indent, "usgs-stream-rating"));
		String pointPrefix = prefix + indent + indent;
		if (shifts != null && shifts.abstractRatingContainers != null) {
			HecTime hectime = new HecTime();
			for (AbstractRatingContainer arc : shifts.abstractRatingContainers) {
				sb.append(prefix).append(indent).append("<height-shifts>\n");
				if (arc.effectiveDateMillis == UNDEFINED_TIME) {
					sb.append(prefix).append(indent).append(indent).append("<effective-date/>\n");
				}
				else {
						hectime.setTimeInMillis(arc.effectiveDateMillis);
						sb.append(prefix).append(indent).append(indent).append("<effective-date>").append(hectime.getXMLDateTime(0)).append("</effective-date>\n");
				}
				if (arc.createDateMillis == UNDEFINED_TIME) {
					sb.append(prefix).append(indent).append(indent).append("<create-date/>\n");
				}
				else {
						hectime.setTimeInMillis(arc.createDateMillis);
						sb.append(prefix).append(indent).append(indent).append("<create-date>").append(hectime.getXMLDateTime(0)).append("</create-date>\n");
				}
				sb.append(prefix).append(indent).append(indent).append("<active>").append(arc.active).append("</active>\n");
				if (arc.description != null) {
					sb.append(prefix).append(indent).append(indent).append("<description>").append(arc.description).append("</description>\n");
				}
				TableRatingContainer trc = (TableRatingContainer)arc;
				if (trc.values != null) {
					for (RatingValueContainer rvc : trc.values) {
						rvc.toXml(pointPrefix, indent, sb);
					}
				}
				sb.append(prefix).append(indent).append("</height-shifts>\n");
			}
		}
		if (offsets != null && offsets.values != null) {
			sb.append(prefix).append(indent).append("<height-offsets>\n");
			for (RatingValueContainer rvc : offsets.values) {
				rvc.toXml(pointPrefix, indent, sb);
			}
			sb.append(prefix).append(indent).append("</height-offsets>\n");
		}
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
		sb.append(prefix).append("</usgs-stream-rating>\n");
		if (level == 0) {
			sb.append("</ratings>\n");
		}
		return sb.toString();
	}
}
