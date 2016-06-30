package hec.data.cwmsRating.io;

import java.util.List;

import hec.data.RatingException;
import hec.data.VerticalDatumException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.UsgsStreamTableRating;
import hec.heclib.util.HecTime;
import hec.io.VerticalDatumContainer;
import hec.util.TextUtil;

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
	 * @see hec.data.cwmsRating.io.TableRatingContainer#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = obj == this;
		if(!result) {
			do {
				if (obj == null || obj.getClass() != getClass()) break;
				if (!super.equals(obj)) break;
				UsgsStreamTableRatingContainer other = (UsgsStreamTableRatingContainer)obj;
				if ((other.shifts == null) != (shifts == null)) break;
				if (shifts != null) {
					if (!other.shifts.equals(shifts)) break;
				}
				if ((other.offsets == null) != (offsets == null)) break;
				if (offsets != null) {
					if (!other.offsets.equals(offsets)) break;
				}
				result = true;
			} while(false);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.TableRatingContainer#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode()
				+ super.hashCode()
				+ 3 * (offsets == null ? 1 : offsets.hashCode())
				+ 5 * (shifts == null ? 1 : shifts.hashCode());
	}

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
	public AbstractRatingContainer getInstance() {
		return new UsgsStreamTableRatingContainer();
	}
	
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.TableRatingContainer#newRating()
	 */
	@Override
	public AbstractRating newRating() throws RatingException {
		UsgsStreamTableRating rating = new UsgsStreamTableRating(this);
		return rating;
	}

	public static UsgsStreamTableRatingContainer fromXml(Element ratingElement) throws RatingException {
		UsgsStreamTableRatingContainer ustrc = new UsgsStreamTableRatingContainer();
		try {
			AbstractRatingContainer.fromXml(ratingElement, ustrc);
		}
		catch (VerticalDatumException e1) {
			throw new RatingException(e1);
		}
		Element elem = null;
		
		@SuppressWarnings("rawtypes")
		List elems = ratingElement.getChildren("height-shifts");
		if (elems.size() > 0) {
			HecTime hectime = new HecTime();
			ustrc.shifts = new RatingSetContainer();
			ustrc.shifts.abstractRatingContainers = new TableRatingContainer[elems.size()];
			for (int i = 0; i < elems.size(); ++i) {
				ustrc.shifts.abstractRatingContainers[i] = new TableRatingContainer();
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
						trc.values[j] = new RatingValueContainer();
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
					trc.values[i] = new RatingValueContainer();
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
					ustrc.values[i] = new RatingValueContainer();
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
					ustrc.values[i] = new RatingValueContainer();
					elem = (Element)pointElems.get(i);
					ustrc.values[i].indValue = Double.parseDouble(elem.getChildTextTrim("ind"));
					ustrc.values[i].depValue = Double.parseDouble(elem.getChildTextTrim("dep"));
					ustrc.values[i].note = elem.getChildTextTrim("note");
				}
			}
		}
		Element verticalDatumElement = ratingElement.getChild("vertical-datum-info");
		if (verticalDatumElement != null) {
			try {
				ustrc.vdc = new VerticalDatumContainer(verticalDatumElement.toString());
			}
			catch (VerticalDatumException e) {
				throw new RatingException(e);
			}
		}
		return ustrc;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.TableRatingContainer#toNativeVerticalDatum()
	 */
	@Override
	public boolean toNativeVerticalDatum() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		boolean change = false;
		if (getParamsAndUnits()[1][0].startsWith("Elev")) {
			double offset = vdc.getCurrentOffset();
			if (offset != 0.) {
				change = true;
				try {
					addOffset(0, -offset);
					if (offsets != null) {
						offsets.addOffset(0, -offset);
					}
					if (shifts != null && shifts.abstractRatingContainers != null)
						for (int i = 0; i < shifts.abstractRatingContainers.length; ++i) {
							shifts.abstractRatingContainers[i].addOffset(0, -offset);
					}
				}
				catch (RatingException e) {
					throw new VerticalDatumException(e);
				}
			}
			vdc.toNativeVerticalDatum();
		}
		return change;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.TableRatingContainer#toNGVD29()
	 */
	@Override
	public boolean toNGVD29() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		boolean change = false;
		String[][] paramsAndUnits = getParamsAndUnits();
		String[] params = paramsAndUnits[0];
		String[] units  = paramsAndUnits[1];
		if (params[0].startsWith("Elev")) {
			if (!vdc.getCurrentVerticalDatum().equals("NGVD29")) {
				change = true;
				double offset1 = vdc.getNGVD29Offset(units[0]);
				double offset2 = vdc.getCurrentOffset(units[0]);
				try {
					addOffset(0, offset1 - offset2);
					if (offsets != null) {
						offsets.addOffset(0, offset1 - offset2);
					}
					if (shifts != null && shifts.abstractRatingContainers != null)
						for (int i = 0; i < shifts.abstractRatingContainers.length; ++i) {
							shifts.abstractRatingContainers[i].addOffset(0, offset1 - offset2);
					}
				}
				catch (RatingException e) {
					throw new VerticalDatumException(e);
				}
			}
			vdc.toNGVD29();
		}
		return change;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.TableRatingContainer#toNAVD88()
	 */
	@Override
	public boolean toNAVD88() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		boolean change = false;
		String[][] paramsAndUnits = getParamsAndUnits();
		String[] params = paramsAndUnits[0];
		String[] units  = paramsAndUnits[1];
		if (params[0].startsWith("Elev")) {
			if (!vdc.getCurrentVerticalDatum().equals("NAVD88")) {
				change = true;
				double offset1 = vdc.getNAVD88Offset(units[0]);
				double offset2 = vdc.getCurrentOffset(units[0]);
				try {
					addOffset(0, offset1 - offset2);
					if (offsets != null) {
						offsets.addOffset(0, offset1 - offset2);
					}
					if (shifts != null && shifts.abstractRatingContainers != null)
						for (int i = 0; i < shifts.abstractRatingContainers.length; ++i) {
							shifts.abstractRatingContainers[i].addOffset(0, offset1 - offset2);
					}
				}
				catch (RatingException e) {
					throw new VerticalDatumException(e);
				}
			}
			vdc.toNAVD88();
		}
		return change;
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
		boolean hasShifts = shifts != null && shifts.abstractRatingContainers != null;
		boolean hasOffsets = offsets != null && offsets.values != null; 
		if (!hasShifts && !hasOffsets) {
			//-----------------------------//
			// serialize as a table rating //
			//-----------------------------//
			return super.toXml(indent, level);
		}
		try {
			if (vdc != null && vdc.getCurrentOffset() != 0.) {
				UsgsStreamTableRatingContainer _clone = new UsgsStreamTableRatingContainer();
				clone(_clone);
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
			prefix += indent;
		}
		sb.append(super.toXml(prefix, indent, "usgs-stream-rating"));
		String pointPrefix = prefix + indent + indent;
		if (hasShifts) {
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
				if (arc.transitionStartDateMillis == UNDEFINED_TIME) {
					sb.append(prefix).append(indent).append(indent).append("<transition-start-date/>\n");
				}
				else {
						hectime.setTimeInMillis(arc.transitionStartDateMillis);
						sb.append(prefix).append(indent).append(indent).append("<transition-start-date>").append(hectime.getXMLDateTime(0)).append("</transition-start-date>\n");
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
					sb.append(prefix).append(indent).append(indent).append("<description>").append(TextUtil.xmlEntityEncode(arc.description)).append("</description>\n");
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
		if (hasOffsets) {
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

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.TableRatingContainer#addOffset(int, double)
	 */
	@Override
	public void addOffset(int paramNum, double offset) throws RatingException {
		super.addOffset(paramNum, offset);
		if (paramNum == 0) {
			if (offsets != null) {
				offsets.addOffset(paramNum, offset);
			}
			if (shifts != null) {
				shifts.addOffset(paramNum, offset);
			}
		}
	}
}
