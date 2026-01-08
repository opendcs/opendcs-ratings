/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import static hec.util.TextUtil.replaceAll;
import static hec.util.TextUtil.split;

import hec.data.DataSetException;
import hec.data.Parameter;

import hec.data.Units;
import hec.data.cwmsRating.RatingConst.RatingMethod;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.IndependentValuesContainer;
import hec.data.cwmsRating.io.RatingSetContainer;
import hec.data.cwmsRating.io.RatingSetStateContainer;
import hec.data.rating.IRatingSpecification;
import hec.data.rating.IRatingTemplate;
import hec.heclib.util.HecTime;
import hec.hecmath.TimeSeriesMath;
import hec.io.Conversion;
import hec.io.TextContainer;
import hec.io.TimeSeriesContainer;
import hec.lang.Const;
import hec.lang.Observable;
import hec.util.TextUtil;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Observer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import mil.army.usace.hec.metadata.VerticalDatumContainer;
import mil.army.usace.hec.metadata.VerticalDatumException;

/**
 * Implements CWMS-style ratings (time series of ratings)
 *
 * @author Mike Perryman
 */
public abstract class AbstractRatingSet extends RatingSet implements CwmsRatingSet {

    protected static final Logger LOGGER = Logger.getLogger(AbstractRatingSet.class.getPackage().getName());

    /**
     * Flag specifying whether new RatingSet objects will by default allow "risky" behavior such as using mismatched units, unknown parameters, etc.
     */
    protected static boolean alwaysAllowUnsafe = true;
    /**
     * Flag specifying whether new RatingSet objects will by default output messages about "risky" behavior such as using mismatched units, unknown parameters, etc.
     */
    protected static boolean alwaysWarnUnsafe = true;
    /**
     * Object that provides the Observable-by-composition functionality
     */
    protected Observable observationTarget = null;
    /**
     * The CWMS-style rating specification (including rating template)
     */
    protected RatingSpec ratingSpec = null;
    /**
     * The specified units of the data to rate
     */
    protected String[] dataUnits = null;
    /**
     * The time series of all ratings in this set, whether active or not
     */
    protected TreeMap<Long, AbstractRating> ratings = new TreeMap<>();
    /**
     * The time series of active ratings in this set.  A rating may be inactive by being explicitly marked as such
     * or by having a creation date later than the current value of the "ratingTime" field.
     */
    protected TreeMap<Long, AbstractRating> activeRatings = new TreeMap<>();
    /**
     * A time to associate with all values that don't specify their own times.  This time, along with the rating
     * effective dates, is used to determine which ratings to use to rate values.
     */
    protected long defaultValueTime = Const.UNDEFINED_TIME;
    /**
     * A time used to allow the rating of values with information that was known at a specific time. No ratings
     * with a creation date after this time will be used to rate values.
     */
    protected long ratingTime = Long.MAX_VALUE;
    /**
     * Flag specifying whether this rating set allows "risky" behavior such as using mismatched units, unknown parameters, etc.
     */
    protected boolean allowUnsafe = true;
    /**
     * Flag specifying whether this rating set outputs messges about "risky" behavior such as using mismatched units, unknown parameters, etc.
     */
    protected boolean warnUnsafe = true;

    protected AbstractRatingSet() {
        allowUnsafe = alwaysAllowUnsafe;
        warnUnsafe = alwaysWarnUnsafe;
        observationTarget = new Observable();
    }

    /**
     * Returns whether new RatingSet objects will by default allow "risky" behavior such as using mismatched units, unknown parameters, etc.
     *
     * @return A flag specifying whether new RatingSet objects will by default allow "risky" behavior
     */
    public static boolean getAlwaysAllowUnsafe() {
        return alwaysAllowUnsafe;
    }

    /**
     * Sets whether new RatingSet objects will by default allow "risky" behavior such as using mismatched units, unknown parameters, etc.
     *
     * @param alwaysAllowUnsafe A flag specifying whether new RatingSet objects will by default allow "risky" behavior
     */
    public static void setAlwaysAllowUnsafe(Boolean alwaysAllowUnsafe) {
        AbstractRatingSet.alwaysAllowUnsafe = alwaysAllowUnsafe;
    }

    /**
     * Returns whether new RatingSet objects will by default output messages about "risky" behavior such as using mismatched units, unknown parameters, etc.
     *
     * @return A flag specifying whether new RatingSet objects will by default output messages about "risky" behavior
     */
    public static boolean getAlwaysWarnUnsafe() {
        return alwaysWarnUnsafe;
    }

    /**
     * Sets whether new RatingSet objects will by default output messages about "risky" behavior such as using mismatched units, unknown parameters, etc.
     *
     * @param alwaysWarnUnsafe A flag specifying whether new RatingSet objects will by default output messages about "risky" behavior
     */
    public static void setAlwaysWarnUnsafe(Boolean alwaysWarnUnsafe) {
        AbstractRatingSet.alwaysWarnUnsafe = alwaysWarnUnsafe;
    }

    /**
     * Retrieves a map of all included rating specifications, keyed by their rating spec ids
     *
     * @param specMap a non-null map to add to
     * @param ratings the ratings to evaluate
     * @param path
     * @throws RatingException if the same spec id maps to two non-equal rating specs
     */
    private static void getAllRatingSpecs(HashMap<String, Object[]> specMap, Iterable<AbstractRating> ratings, String path) throws RatingException {
        if (specMap == null) {
            throw new RatingException("Cannot use a null specMap parameter");
        }
        for (AbstractRating r : ratings) {
            String thisPath = path + "/" + r.getRatingSpecId();
            if (r instanceof VirtualRating) {
                VirtualRating vr = (VirtualRating) r;
                for (SourceRating sr : vr.getSourceRatings()) {
                    if (sr.ratings != null) {
                        getAllRatingSpecs(specMap, Arrays.asList(sr.ratings.getRatings()), thisPath);
                    }
                }
            } else if (r instanceof TransitionalRating) {
                TransitionalRating tr = (TransitionalRating) r;
                for (SourceRating sr : tr.getSourceRatings()) {
                    if (sr.ratings != null) {
                        getAllRatingSpecs(specMap, Arrays.asList(sr.ratings.getRatings()), thisPath);
                    }
                }
            }
            RatingSpec spec = r.ratingSpec;
            if (spec != null) {
                String specId = spec.getRatingSpecId();
                if (specMap.containsKey(specId)) {
                    if (!spec.equals(specMap.get(specId)[1])) {
                        throw new RatingException("Ratings contain multiple definitions of rating spec \"" + specId + "\"");
                    }
                } else {
                    specMap.put(specId, new Object[] {thisPath, spec});
                }
            }
        }
    }

    /**
     * Adds a single rating to the existing ratings.
     *
     * @param rating The rating to add
     * @throws RatingException @see #addRatings(Iterable)
     */
    @Override
    public final void addRating(AbstractRating rating) throws RatingException {
        addRatings(Arrays.asList(new AbstractRating[] {rating}));
    }

    /**
     * Adds multiple ratings to the existing ratings.
     *
     * @param ratings The ratings to add
     * @throws RatingException @see #addRatings(Iterable)
     */
    @Override
    public final void addRatings(AbstractRating[] ratings) throws RatingException {
        addRatings(Arrays.asList(ratings));
    }

    /**
     * Adds multiple ratings to the existing ratings.
     *
     * @param ratings The ratings to add
     * @throws RatingException various errors with the input such a undefined effective dates,
     *                         effective date already exists, number of independent parameters not consistent,
     *                         rating specs not consistent, units incompatible, templates not consistent
     */
    @Override
    public synchronized void addRatings(Iterable<AbstractRating> ratings) throws RatingException {
        String ratingSpecId = null;
        String unitsId = null;
        for (AbstractRating rating : ratings) {
            if (rating.getEffectiveDate() == Const.UNDEFINED_TIME) {
                throw new RatingException("Cannot add rating with undefined effective date.");
            }
            if (this.ratings.containsKey(rating.getEffectiveDate())) {
                throw new RatingException("Rating with same effective date already exists; cannot add rating");
            }
            if (ratingSpec != null && rating.getIndParamCount() != ratingSpec.getIndParamCount()) {
                throw new RatingException("Number of independent parameters does not match rating specification");
            }
            if (ratingSpecId == null) {
                ratingSpecId = rating.getRatingSpecId();
            } else if (!rating.getRatingSpecId().equals(ratingSpecId)) {
                throw new RatingException("Ratings have inconsistent rating specifications.");
            }
            if (unitsId == null) {
                unitsId = rating.getRatingUnitsId();
            } else if (!AbstractRating.compatibleUnits(unitsId, rating.getRatingUnitsId())) {
                throw new RatingException(
                    String.format("Rating units of \"%s\" aren't compatible with rating units of \"%s\"", rating.getRatingUnitsId(), unitsId));
            }
        }
        HashMap<String, Object[]> newSpecs = new HashMap<>();
        AbstractRatingSet.getAllRatingSpecs(newSpecs, ratings, "");
        if (this.ratings.size() > 0) {
            if (!this.ratings.firstEntry().getValue().getRatingSpecId().equals(ratingSpecId)) {
                throw new RatingException("Cannot add ratings with different rating specification IDs");
            }
            HashMap<String, Object[]> oldSpecs = new HashMap<>();
            AbstractRatingSet.getAllRatingSpecs(oldSpecs, this.ratings.values(), "");
            for (String specId : oldSpecs.keySet()) {
                if (newSpecs.containsKey(specId) && !newSpecs.get(specId)[1].equals(oldSpecs.get(specId)[1])) {
                    StringBuilder sb = new StringBuilder("Cannot add ratings with different rating template or specification definitions.\n");
                    sb.append("Existing :").append((String) oldSpecs.get(specId)[0]).append("\n").append("Incoming :")
                      .append((String) newSpecs.get(specId)[0]).append("\n");
                    String oldXml = ((RatingSpec) oldSpecs.get(specId)[1]).toTemplateXml("  ", 3);
                    String newXml = ((RatingSpec) newSpecs.get(specId)[1]).toTemplateXml("  ", 3);
                    if (!newXml.equals(oldXml)) {
                        sb.append("Definitions for template \"").append(((RatingSpec) oldSpecs.get(specId)[1]).getTemplateId())
                          .append("\" differ.\nExisting Definition :\n").append(oldXml).append("New Definition :\n").append(newXml);
                    }
                    oldXml = ((RatingSpec) oldSpecs.get(specId)[1]).toSpecXml("  ", 3);
                    newXml = ((RatingSpec) newSpecs.get(specId)[1]).toSpecXml("  ", 3);
                    if (!newXml.equals(oldXml)) {
                        sb.append("Definitions for specification \"").append(((RatingSpec) oldSpecs.get(specId)[1]).getRatingSpecId())
                          .append("\"differ.\nExisting Definition :\n").append(oldXml).append("New Definition :\n").append(newXml);
                    }
                    throw new RatingException(sb.toString());
                }
            }
            if (!AbstractRating.compatibleUnits(unitsId, this.ratings.firstEntry().getValue().getRatingUnitsId())) {
                throw new RatingException(String.format("Rating units of \"%s\" aren't compatible with rating units of \"%s\"", unitsId,
                    this.ratings.firstEntry().getValue().getRatingUnitsId()));
            }
        }
        for (AbstractRating rating : ratings) {
            if (rating instanceof UsgsStreamTableRating) {
                UsgsStreamTableRating streamRating = (UsgsStreamTableRating) rating;
                if (streamRating.shifts != null) {
                    streamRating.shifts.getRatingSpec().inRangeMethod = this.ratingSpec.inRangeMethod;
                }
            }
            if (rating instanceof VirtualRating) {
                VirtualRating vr = (VirtualRating) rating;
                if (vr.isNormalized()) {
                    this.ratings.put(rating.getEffectiveDate(), vr);
                } else {
                    this.ratings.put(rating.getEffectiveDate(), vr.normalizedCopy());
                }
            } else {
                this.ratings.put(rating.getEffectiveDate(), rating);
            }
            AbstractRating ar = this.ratings.get(rating.getEffectiveDate());
            ar.ratingSpec = ratingSpec;
            ar.setDefaultValueTime(getDefaultValueTime());
            ar.setRatingTime(getRatingTime());
            ar.setDataUnits(getDataUnits());
            try {
                String currentDatum = rating.getCurrentVerticalDatum();
                ar.setVerticalDatumInfo(getVerticalDatumInfo());
                ar.vdc.forceVerticalDatum(currentDatum);
            } catch (VerticalDatumException e) {
                LOGGER.log(Level.FINE, "Could not set vertical datum info on rating being added to rating set.", e);
            }
            ar.setAllowUnsafe(doesAllowUnsafe());
            ar.setWarnUnsafe(doesWarnUnsafe());
            if (rating.isActive() && rating.createDate <= ratingTime) {
                activeRatings.put(rating.getEffectiveDate(), rating);
                activeRatings.get(rating.getEffectiveDate()).ratingSpec = ratingSpec;
            }
            rating.deleteObserver(this);
            rating.addObserver(this);
            validate();
        }
        if (observationTarget != null) {
            observationTarget.setChanged();
            observationTarget.notifyObservers();
        }
    }

    /**
     * Removes a single rating from the existing ratings.
     *
     * @param effectiveDate The effective date of the rating to remove, in Java milliseconds
     * @throws RatingException
     */
    @Override
    public synchronized final void removeRating(long effectiveDate) throws RatingException {
        ICwmsRating cwmsRating = ratings.get(effectiveDate);
        if (cwmsRating == null) {
            throw new RatingException("Rating with specified effective date does not exist; cannot remove rating");
        }
        cwmsRating.deleteObserver(this);
        ratings.remove(effectiveDate);
        if (activeRatings.containsKey(effectiveDate)) {
            activeRatings.remove(effectiveDate);
        }
        if (observationTarget != null) {
            observationTarget.setChanged();
            observationTarget.notifyObservers();
        }
    }

    /**
     * Removes all existing ratings.
     */
    @Override
    public synchronized final void removeAllRatings() {
        for (ICwmsRating cwmsRating : ratings.values()) {
            cwmsRating.deleteObserver(this);
        }
        ratings.clear();
        activeRatings.clear();
        if (observationTarget != null) {
            observationTarget.setChanged();
            observationTarget.notifyObservers();
        }
    }

    /**
     * Replaces a single rating in the existing ratings
     *
     * @param rating The rating to replace an existing one
     * @throws RatingException
     */
    @Override
    public synchronized final void replaceRating(AbstractRating rating) throws RatingException {
        Long effectiveDate = rating.getEffectiveDate();
        if (!ratings.containsKey(effectiveDate)) {
            throw new RatingException("Rating with same effective date does not exist; cannot replace rating");
        }
        if (ratingSpec != null && rating.getIndParamCount() != ratingSpec.getIndParamCount()) {
            throw new RatingException("Number of independent parameters does not match rating specification");
        }
        if (!rating.getRatingSpecId().equals(ratings.firstEntry().getValue().getRatingSpecId())) {
            throw new RatingException("Cannot replace rating with different rating specification.");
        }
        if (!rating.getRatingUnitsId().equals(ratings.firstEntry().getValue().getRatingUnitsId())) {
            throw new RatingException("Cannot replace rating with different units.");
        }
        rating.ratingSpec = ratingSpec;
        rating.setDataUnits(getDataUnits());
        rating.setDefaultValueTime(getDefaultValuetime());
        rating.setRatingTime(getRatingTime());
        rating.setName(getName());
        rating.setAllowUnsafe(doesAllowUnsafe());
        rating.setWarnUnsafe(doesWarnUnsafe());
        for (AbstractRating r : ratings.values()) {
            if (r.getVerticalDatumContainer() != null) {
                try {
                    rating.setVerticalDatumInfo(r.getVerticalDatumInfo());
                    break;
                } catch (VerticalDatumException ex) {
                    throw new RatingException(ex);
                }
            }
        }
        ratings.put(effectiveDate, rating).deleteObserver(this);
        if (rating.isActive()) {
            activeRatings.put(effectiveDate, rating);
        }
        rating.deleteObserver(this);
        rating.addObserver(this);
        validate();
        if (observationTarget != null) {
            observationTarget.setChanged();
            observationTarget.notifyObservers();
        }
    }

    /**
     * Replaces multiple ratings in the existing ratings.
     *
     * @param ratings The ratings to replace existing ones
     * @throws RatingException
     */
    @Override
    public synchronized final void replaceRatings(AbstractRating[] ratings) throws RatingException {
        replaceRatings(Arrays.asList(ratings));
    }

    /**
     * Replaces multiple ratings in the existing ratings.
     *
     * @param ratings The ratings to replace existing ones
     * @throws RatingException
     */
    @Override
    public synchronized void replaceRatings(Iterable<AbstractRating> ratings) throws RatingException {
        String ratingSpecId = null;
        String unitsId = null;
        for (AbstractRating rating : ratings) {
            if (!this.ratings.containsKey(rating.getEffectiveDate())) {
                throw new RatingException("Rating with same effective date does not exist; cannot replace rating");
            }
            if (ratingSpec != null && rating.getIndParamCount() != ratingSpec.getIndParamCount()) {
                throw new RatingException("Number of independent parameters does not match rating specification");
            }
            if (ratingSpecId == null) {
                ratingSpecId = rating.getRatingSpecId();
            } else if (!rating.getRatingSpecId().equals(ratingSpecId)) {
                throw new RatingException("Ratings have inconsistent rating specifications.");
            }
            if (unitsId == null) {
                unitsId = rating.getRatingUnitsId();
            } else if (!rating.getRatingUnitsId().equals(unitsId)) {
                throw new RatingException("Ratings have inconsistent units.");
            }
        }
        if (!this.ratings.firstEntry().getValue().getRatingSpecId().equals(ratingSpecId)) {
            throw new RatingException("Cannot replace ratings with different rating specification.");
        }
        if (!this.ratings.firstEntry().getValue().getRatingUnitsId().equals(unitsId)) {
            throw new RatingException("Cannot replace ratings with different units.");
        }
        for (AbstractRating rating : ratings) {
            this.ratings.put(rating.getEffectiveDate(), rating).deleteObserver(this);
            if (rating.isActive() && rating.createDate <= ratingTime) {
                activeRatings.put(rating.getEffectiveDate(), rating);
            }
            rating.deleteObserver(this);
            rating.addObserver(this);
        }
        validate();
        if (observationTarget != null) {
            observationTarget.setChanged();
            observationTarget.notifyObservers();
        }
    }

    /**
     * Retrieves a rated value for a specified single input value and time. The rating set must
     * be for a single independent parameter
     *
     * @param value     The value to rate
     * @param valueTime The time associated with the value, in Java milliseconds
     * @return the rated value
     * @throws RatingException
     */
    @Override
    public final double rate(double value, long valueTime) throws RatingException {
        double[] values = {value};
        return rate(values, valueTime)[0];
    }

    /**
     * Retrieves rated values for specified multiple input values at a single time. The rating set must
     * be for a single independent parameter
     *
     * @param values    The values to rate
     * @param valueTime The time associated with the values, in Java milliseconds
     * @return the rated value
     * @throws RatingException
     */
    @Override
    public final double[] rate(double[] values, long valueTime) throws RatingException {
        long[] valueTimes = new long[values.length];
        Arrays.fill(valueTimes, valueTime);
        return rateOne(values, valueTimes);
    }

    /**
     * Retrieves rated values for specified multiple input values and times. The rating set must
     * be for a single independent parameter
     *
     * @param values     The values to rate
     * @param valueTimes The times associated with the values, in Java milliseconds
     * @return the rated value
     * @throws RatingException
     */
    @Override
    public final double[] rateOne(double[] values, long[] valueTimes) throws RatingException {
        double[][] valueSets = new double[values.length][1];
        for (int i = 0; i < values.length; ++i) {
            valueSets[i][0] = values[i];
        }
        return rate(valueSets, valueTimes);
    }

    /**
     * Retrieves a single rated value for specified input value set at a single time. The rating set must
     * be for as many independent parameters as the length of the value set.
     *
     * @param valueSet  The value set to rate
     * @param valueTime The time associated with the values, in Java milliseconds
     * @return the rated value
     * @throws RatingException
     */
    @Override
    public final double rateOne(double[] valueSet, long valueTime) throws RatingException {
        double[][] valueSets = {valueSet};
        long[] valueTimes = {valueTime};
        return rate(valueSets, valueTimes)[0];
    }

    /**
     * Retrieves rated values for specified multiple input value Sets and times. The rating set must
     * be for as many independent parameter as each value set
     *
     * @param valueSets  The value sets to rate
     * @param valueTimes The times associated with the values, in Java milliseconds
     * @return the rated value
     * @throws RatingException
     */
    @Override
    public synchronized double[] rate(double[][] valueSets, long[] valueTimes) throws RatingException {
        double[] Y = new double[valueSets.length];
        int activeRatingCount = activeRatings.size();
        if (activeRatingCount == 0) {
            throw new RatingException("No active ratings.");
        }
        if (valueSets.length != valueTimes.length) {
            throw new RatingException("Values and times have different lengths");
        }
        for (int i = 1; i < valueSets.length; ++i) {
            if (valueSets[i].length != valueSets[0].length) {
                throw new RatingException("Value sets are not all of same length.");
            }
            if (valueSets[i].length != ratings.firstEntry().getValue().getIndParamCount()) {
                throw new RatingException("Value sets have different parameter counts than ratings.");
            }
        }
        if (getDataUnits() == null) {
            Entry<Long, AbstractRating> entry1 = ratings.firstEntry();
            Entry<Long, AbstractRating> entry2 = ratings.higherEntry(entry1.getKey());
            while (entry2 != null) {
                if (!(entry1.getValue().getRatingUnitsId().equalsIgnoreCase(entry2.getValue().getRatingUnitsId()))) {
                    throw new RatingException("Data units must be specified when rating set has multiple rating units.");
                }
                entry1 = entry2;
                entry2 = ratings.higherEntry(entry1.getKey());
            }
        }

        Entry<Long, AbstractRating> lowerRating = null;
        Entry<Long, AbstractRating> upperRating = null;
        IRating lastUsedRating = null;
        RatingMethod method = null;
        for (int i = 0; i < valueSets.length; ++i) {
            if (i > 0 && valueTimes[i] == valueTimes[i - 1] && lastUsedRating != null) {
                Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                continue;
            } else {
                lowerRating = activeRatings.floorEntry(valueTimes[i]);
                upperRating = activeRatings.ceilingEntry(valueTimes[i]);
                //-------------------------//
                // handle out of range low //
                //-------------------------//
                if (lowerRating == null) {
                    method = ratingSpec.getOutRangeLowMethod();
                    switch (method) {
                        case ERROR:
                            throw new RatingException("Effective date is before earliest rating");
                        case NULL:
                            Y[i] = Const.UNDEFINED_DOUBLE;
                            lastUsedRating = null;
                            continue;
                        case NEXT:
                        case NEAREST:
                        case HIGHER:
                        case CLOSEST:
                            lastUsedRating = activeRatings.firstEntry().getValue();
                            Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                            continue;
                        default:
                            break;
                    }
                    if (activeRatings.size() == 1) {
                        throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
                    }
                    lowerRating = activeRatings.firstEntry();
                    upperRating = activeRatings.higherEntry(lowerRating.getKey());
                }
                //--------------------------//
                // handle out of range high //
                //--------------------------//
                if (upperRating == null) {
                    method = ratingSpec.getOutRangeHighMethod();
                    switch (method) {
                        case ERROR:
                            throw new RatingException("Effective date is after latest rating");
                        case NULL:
                            Y[i] = Const.UNDEFINED_DOUBLE;
                            lastUsedRating = null;
                            continue;
                        case PREVIOUS:
                        case NEAREST:
                        case LOWER:
                        case CLOSEST:
                            lastUsedRating = activeRatings.lastEntry().getValue();
                            Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                            continue;
                        default:
                            break;
                    }
                    if (activeRatings.size() == 1) {
                        switch (method) {
                            case LINEAR:
                                //-----------------------------------------------------------------//
                                // allow LINEAR out of range high method with single active rating //
                                //-----------------------------------------------------------------//
                                lastUsedRating = activeRatings.lastEntry().getValue();
                                Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                                continue;
                            default:
                                throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
                        }
                    }
                    upperRating = activeRatings.lastEntry();
                    lowerRating = activeRatings.lowerEntry(upperRating.getKey());
                }
                //-----------------------------------//
                // handle in-range and extrapolation //
                //-----------------------------------//
                if (lowerRating.getKey() == valueTimes[i]) {
                    Y[i] = lowerRating.getValue().rateOne(valueTimes[i], valueSets[i]);
                    continue;
                }
                if (upperRating.getKey() == valueTimes[i]) {
                    lastUsedRating = upperRating.getValue();
                    Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                    continue;
                }
                switch (ratingSpec.getInRangeMethod()) {
                    case ERROR:
                        throw new RatingException("Effective date is between existing rating");
                    case NULL:
                        Y[i] = Const.UNDEFINED_DOUBLE;
                        lastUsedRating = null;
                        continue;
                    case PREVIOUS:
                    case LOWER:
                        lastUsedRating = lowerRating.getValue();
                        Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                        continue;
                    case NEXT:
                    case HIGHER:
                        lastUsedRating = upperRating.getValue();
                        Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                        continue;
                    case CLOSEST:
                        if (valueTimes[i] - lowerRating.getKey() < upperRating.getKey() - valueTimes[i]) {
                            lastUsedRating = lowerRating.getValue();
                        } else {
                            lastUsedRating = upperRating.getValue();
                        }
                        Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
                        continue;
                    default:
                        break;
                }
                //------------------------------------//
                // handle interpolation/extrapolation //
                //------------------------------------//
                method = ratingSpec.getInRangeMethod();
            }
            lastUsedRating = null;
            long transitionStartMillis = upperRating.getValue().getTransitionStartDate();
            long t = valueTimes[i];
            long t1 = lowerRating.getKey();
            long t2 = upperRating.getKey();
            double Y1 = lowerRating.getValue().rateOne(valueTimes[i], valueSets[i]);
            double Y2 = upperRating.getValue().rateOne(valueTimes[i], valueSets[i]);
            if (Y1 == Const.UNDEFINED_DOUBLE || Y2 == Const.UNDEFINED_DOUBLE) {
                Y[i] = Const.UNDEFINED_DOUBLE;
            } else {
                double y1 = Y1;
                double y2 = Y2;
                if (lowerRating.getValue() instanceof UsgsStreamTableRating) {
                    t1 = ((UsgsStreamTableRating) lowerRating.getValue()).getLatestEffectiveDate(t2);
                }
                if (transitionStartMillis > t1 && transitionStartMillis < t2) {
                    t1 = transitionStartMillis;
                }
                Y[i] = y1;
                if (t > t1) {
                    Y[i] += (((double) t - t1) / (t2 - t1)) * (y2 - y1);
                }
            }
        }
        return Y;
    }

    /**
     * Rates the values in a TimeSeriesContainer and returns the results in a new TimeSeriesContainer.
     * The rating must be for a single independent parameter.
     *
     * @param tsc The TimeSeriesContainer to rate
     * @return A TimeSeriesContainer of the rated values. The rated unit is the native unit of dependent parameter of the rating.
     * @throws RatingException
     */
    @Override
    public final TimeSeriesContainer rate(TimeSeriesContainer tsc) throws RatingException {
        return rate(tsc, null);
    }

    /**
     * Rates the values in a TimeSeriesContainer and returns the results in a new TimeSeriesContainer with the specified unit.
     * The rating must be for a single independent parameter.
     *
     * @param tsc          The TimeSeriesContainer to rate
     * @param ratedUnitStr The unit to return the rated values in.
     * @return A TimeSeriesContainer of the rated values. The rated unit is the specified unit.
     * @throws RatingException
     */
    @Override
    public synchronized final TimeSeriesContainer rate(TimeSeriesContainer tsc, String ratedUnitStr) throws RatingException {
        if (ratingSpec.getIndParamCount() != 1) {
            throw new RatingException(String.format("Cannot rate a TimeSeriesContainer with a rating that has %d independent parameters",
                ratingSpec.getIndParamCount()));
        }
        if (ratings.size() == 0) {
            throw new RatingException("No ratings.");
        }
        String[] units = getRatingUnits();
        String[] params = ratingSpec.getIndParameters();
        if (ratedUnitStr == null || ratedUnitStr.length() == 0) {
            ratedUnitStr = units[units.length - 1];
        }
        try {
            //-------------------------------------//
            // validate the time zone if specified //
            //-------------------------------------//
            TimeZone tz = null;
            if (tsc.timeZoneID != null) {
                tz = TimeZone.getTimeZone(tsc.timeZoneID);
                if (!tz.getID().equals(tsc.timeZoneID)) {
                    String msg = String.format("TimeSeriesContainer has invalid time zone \"%s\".", tsc.timeZoneID);
                    if (!allowUnsafe) {
                        throw new RatingException(msg);
                    }
                    if (warnUnsafe) {
                        LOGGER.warning(msg + "  Value times will be treated as UTC.");
                    }
                    tz = null;
                }
            }
            //-----------------------------------------//
            // validate the parameter and unit to rate //
            //-----------------------------------------//
            Parameter tscParam = null;
            Units tscUnit = null;
            Units ratedUnit = null;
            boolean convertTscUnit = false;
            boolean convertRatedUnit = false;
            try {
                tscParam = new Parameter(tsc.parameter);
            } catch (Throwable t) {
                if (!allowUnsafe) {
                    throw new RatingException(t);
                }
                if (warnUnsafe) {
                    LOGGER.warning(t.getMessage());
                }
            }
            if (tscParam != null) {
                if (!tscParam.getParameter().equals(params[0])) {
                    String msg = String.format("Parameter \"%s\" does not match rating parameter \"%s\".", tscParam.getParameter(), params[0]);
                    if (!allowUnsafe) {
                        throw new RatingException(msg);
                    }
                    if (warnUnsafe) {
                        LOGGER.warning(msg);
                    }
                }
            }
            try {
                tscUnit = new Units(tsc.units);
            } catch (Throwable t) {
                if (!allowUnsafe) {
                    throw new RatingException(t);
                }
                if (warnUnsafe) {
                    LOGGER.warning(t.getMessage());
                }
            }
            if (tscParam != null) {
                if (!Units.canConvertBetweenUnits(tsc.units, tscParam.getUnitsString())) {

                    String msg = String.format("Unit \"%s\" is not valid for parameter \"%s\".", tsc.units, tscParam.getParameter());
                    if (!allowUnsafe) {
                        throw new RatingException(msg);
                    }
                    if (warnUnsafe) {
                        LOGGER.warning(msg);
                    }
                }
            }
            if (tscUnit != null) {
                if (!tsc.units.equals(units[0])) {
                    if (Units.canConvertBetweenUnits(tsc.units, units[0])) {
                        convertTscUnit = true;
                    } else {
                        String msg = String.format("Cannot convert from \"%s\" to \"%s\".", tsc.units, units[0]);
                        if (!allowUnsafe) {
                            throw new RatingException(msg);
                        }
                        if (warnUnsafe) {
                            LOGGER.warning(msg + "  Rating will be performed on unconverted values.");
                        }
                    }
                }
            }
            //--------------------------//
            // validate the result unit //
            //--------------------------//
            try {
                ratedUnit = new Units(ratedUnitStr);
            } catch (Throwable t) {
                if (!allowUnsafe) {
                    throw new RatingException(t);
                }
                if (warnUnsafe) {
                    LOGGER.warning(t.getMessage());
                }
            }
            if (ratedUnit != null) {
                if (!ratedUnitStr.equals(units[units.length - 1])) {
                    if (Units.canConvertBetweenUnits(ratedUnitStr, units[units.length - 1])) {
                        convertRatedUnit = true;
                    } else {
                        String msg = String.format("Cannot convert from \"%s\" to \"%s\".", units[units.length - 1], ratedUnit);
                        if (!allowUnsafe) {
                            throw new RatingException(msg);
                        }
                        if (warnUnsafe) {
                            LOGGER.warning(msg + "  Rated values will be unconverted.");
                        }
                    }
                }
            }
            //-------------------------//
            // finally - do the rating //
            //-------------------------//
            double[] indVals = null;
            long[] millis = new long[tsc.times.length];
            if (tz == null) {
                for (int i = 0; i < millis.length; ++i) {
                    millis[i] = Conversion.toMillis(tsc.times[i]);
                }
            } else {
                Calendar cal = Calendar.getInstance();
                cal.setTimeZone(tz);
                SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyyyy, HH:mm");
                HecTime t = new HecTime();
                for (int i = 0; i < millis.length; ++i) {
                    t.set(tsc.times[i]);
                    cal.setTime(sdf.parse((t.dateAndTime(4))));
                    millis[i] = cal.getTimeInMillis();
                }
            }
            if (convertTscUnit) {
                indVals = Arrays.copyOf(tsc.values, tsc.values.length);
                Units.convertUnits(indVals, tsc.units, units[0]);
            } else {
                indVals = tsc.values;
            }
            double[] depVals = rateOne(indVals, millis);
            //-----------------------------------------//
            // construct the rated TimeSeriesContainer //
            //-----------------------------------------//
            if (convertRatedUnit) {
                Units.convertUnits(depVals, units[units.length - 1], ratedUnitStr);
            }
            TimeSeriesContainer ratedTsc = new TimeSeriesContainer();
            tsc.clone(ratedTsc);
            ratedTsc.values = depVals;
            String paramStr = getRatingSpec().getDepParameter();
            if (tsc.subParameter == null) {
                ratedTsc.fullName = replaceAll(tsc.fullName, tsc.parameter, paramStr, "IL");
            } else {
                ratedTsc.fullName = replaceAll(tsc.fullName, String.format("%s-%s", tsc.parameter, tsc.subParameter), paramStr, "IL");
            }
            String[] parts = split(paramStr, "-", "L", 2);
            ratedTsc.parameter = parts[0];
            ratedTsc.subParameter = parts.length > 1 ? parts[1] : null;
            ratedTsc.units = ratedUnitStr;
            return ratedTsc;
        } catch (Throwable t) {
            if (t instanceof RatingException) {
                throw (RatingException) t;
            }
            throw new RatingException(t);
        }
    }

    /**
     * Rates the values in a set of TimeSeriesContainers and returns the results in a new TimeSeriesContainer.
     * The rating must be for as many independent parameters as the number of TimeSeriesContainers.
     * If all the TimeSeriesContainers have the same interval the rated TimeSeriesContainer will have the same interval, otherwise
     * the rated TimeSeriesContainer will have an interval of 0 (irregular).  The rated TimeSeriesContainer will have values
     * only at times that are common to all the input TimeSeriesContainers.
     *
     * @param tscs The TimeSeriesContainers to rate, in order of the independent parameters of the rating.
     * @return A TimeSeriesContainer of the rated values. The rated unit is the native unit of the dependent parameter of the rating.
     * @throws RatingException
     */
    @Override
    public final TimeSeriesContainer rate(TimeSeriesContainer[] tscs) throws RatingException {
        return rate(tscs, null);
    }

    /**
     * Rates the values in a set of TimeSeriesContainers and returns the results in a new TimeSeriesContainer with the specified unit.
     * The rating must be for as many independent parameters as the number of TimeSeriesContainers.
     * If all the TimeSeriesContainers have the same interval the rated TimeSeriesContainer will have the same interval, otherwise
     * the rated TimeSeriesContainer will have an interval of 0 (irregular).  The rated TimeSeriesContainer will have values
     * only at times that are common to all the input TimeSeriesContainers.
     *
     * @param tscs         The TimeSeriesContainers to rate, in order of the independent parameters of the rating.
     * @param ratedUnitStr The unit to return the rated values in.
     * @return A TimeSeriesContainer of the rated values. The rated unit is the specified unit.
     * @throws RatingException
     */
    @Override
    public synchronized final TimeSeriesContainer rate(TimeSeriesContainer[] tscs, String ratedUnitStr) throws RatingException {
        if (ratingSpec.getIndParamCount() != tscs.length) {
            throw new RatingException(
                String.format("Cannot rate a set of %d TimeSeriesContainers with a rating that has %d independent parameters", tscs.length,
                    ratingSpec.getIndParamCount()));
        }
        String[] params = ratingSpec.getIndParameters();
        if (ratings.size() == 0) {
            throw new RatingException("No ratings.");
        }
        String[] units = getRatingUnits();
        if (ratedUnitStr == null || ratedUnitStr.length() == 0) {
            ratedUnitStr = units[units.length - 1];
        }
        int ratedInterval = tscs[0].interval;
        int indParamCount = tscs.length;
        try {
            //------------------------//
            // validate the intervals //
            //------------------------//
            for (int i = 1; i < tscs.length; ++i) {
                if (tscs[i].interval != tscs[0].interval) {
                    String msg = "TimeSeriesContainers have inconsistent intervals.";
                    if (!allowUnsafe) {
                        throw new RatingException(msg);
                    }
                    if (warnUnsafe) {
                        LOGGER.warning(msg + "  Rated values will be irregular interval.");
                    }
                    ratedInterval = 0;
                    break;
                }
            }
            //--------------------------------------//
            // validate the time zones if specified //
            //--------------------------------------//
            String tzid = tscs[0].timeZoneID;
            for (int i = 1; i < tscs.length; ++i) {
                if (!TextUtil.equals(tscs[i].timeZoneID, tzid)) {
                    String msg = "TimeSeriesContainers have inconsistent time zones.";
                    if (!allowUnsafe) {
                        throw new RatingException(msg);
                    }
                    if (warnUnsafe) {
                        LOGGER.warning(msg + "  Value times will be treated as UTC.");
                    }
                    tzid = null;
                    break;
                }
            }
            TimeZone tz = null;
            if (tzid != null) {
                tz = TimeZone.getTimeZone(tzid);
                if (!tz.getID().equals(tzid)) {
                    String msg = String.format("TimeSeriesContainers have invalid time zone \"%s\".", tzid);
                    if (!allowUnsafe) {
                        throw new RatingException(msg);
                    }
                    if (warnUnsafe) {
                        LOGGER.warning(msg + "  Value times will be treated as UTC.");
                    }
                    tz = null;
                }
            }
            //-------------------------------------------//
            // validate the parameters and units to rate //
            //-------------------------------------------//
            Parameter[] tscParam = new Parameter[indParamCount];
            Units[] tscUnit = new Units[indParamCount];
            Units ratedUnit = null;
            boolean[] convertTscUnit = new boolean[indParamCount];
            boolean convertRatedUnit = false;
            for (int i = 0; i < indParamCount; ++i) {
                tscParam[i] = null;
                try {
                    tscParam[i] = new Parameter(tscs[i].parameter);
                } catch (Throwable t) {
                    if (!allowUnsafe) {
                        throw new RatingException(t);
                    }
                    if (warnUnsafe) {
                        LOGGER.warning(t.getMessage());
                    }
                }
                if (tscParam[i] != null) {
                    String[] parts = TextUtil.split(params[i], "-", 2);
                    if (!tscParam[i].getBaseParameter().equals(parts[0]) ||
                        (parts.length == 2 && !tscParam[i].getSubParameter().equals(parts[1]))) {
                        String msg =
                            String.format("Parameter \"%s\" does not match rating parameter \"%s\".", tscParam[i].getParameter(), params[i]);
                        if (!allowUnsafe) {
                            throw new RatingException(msg);
                        }
                        if (warnUnsafe) {
                            LOGGER.warning(msg);
                        }
                    }
                }
                try {
                    tscUnit[i] = new Units(tscs[i].units);
                } catch (Throwable t) {
                    if (!allowUnsafe) {
                        throw new RatingException(t);
                    }
                    if (warnUnsafe) {
                        LOGGER.warning(t.getMessage());
                    }
                }
                if (tscParam[i] != null) {
                    if (!Units.canConvertBetweenUnits(tscs[i].units, tscParam[i].getUnitsString())) {

                        String msg = String.format("Unit \"%s\" is not valid for parameter \"%s\".", tscs[i].units, tscParam[i].getParameter());
                        if (!allowUnsafe) {
                            throw new RatingException(msg);
                        }
                        if (warnUnsafe) {
                            LOGGER.warning(msg);
                        }
                    }
                }
                if (tscUnit != null) {
                    if (!tscs[i].units.equals(units[i])) {
                        if (Units.canConvertBetweenUnits(tscs[i].units, units[i])) {
                            convertTscUnit[i] = true;
                        } else {
                            String msg = String.format("Cannot convert from \"%s\" to \"%s\".", tscs[i].units, units[i]);
                            if (!allowUnsafe) {
                                throw new RatingException(msg);
                            }
                            if (warnUnsafe) {
                                LOGGER.warning(msg + "  Rating will be performed on unconverted values.");
                            }
                        }
                    }
                }
            }
            //--------------------------//
            // validate the result unit //
            //--------------------------//
            try {
                ratedUnit = new Units(ratedUnitStr);
            } catch (Throwable t) {
                if (!allowUnsafe) {
                    throw new RatingException(t);
                }
                if (warnUnsafe) {
                    LOGGER.warning(t.getMessage());
                }
            }
            if (ratedUnit != null) {
                if (!ratedUnitStr.equals(units[units.length - 1])) {
                    if (Units.canConvertBetweenUnits(ratedUnitStr, units[units.length - 1])) {
                        convertRatedUnit = true;
                    } else {
                        String msg = String.format("Cannot convert from \"%s\" to \"%s\".", units[units.length - 1], ratedUnit);
                        if (!allowUnsafe) {
                            throw new RatingException(msg);
                        }
                        if (warnUnsafe) {
                            LOGGER.warning(msg + "  Rated values will be unconverted.");
                        }
                    }
                }
            }
            //-------------------------//
            // finally - do the rating //
            //-------------------------//
            IndependentValuesContainer ivc = RatingUtil.tscsToIvc(tscs, units, tz, allowUnsafe, warnUnsafe);
            double[] depVals = rate(ivc.indVals, ivc.valTimes);
            //-----------------------------------------//
            // construct the rated TimeSeriesContainer //
            //-----------------------------------------//
            if (convertRatedUnit) {
                Units.convertUnits(depVals, units[units.length - 1], ratedUnitStr);
            }
            TimeSeriesContainer ratedTsc = new TimeSeriesContainer();
            tscs[0].clone(ratedTsc);
            ratedTsc.interval = ratedInterval;
            if (ivc.valTimes.length == tscs[0].times.length) {
                ratedTsc.times = Arrays.copyOf(tscs[0].times, tscs[0].times.length);
            } else {
                ratedTsc.times = new int[ivc.valTimes.length];
                if (tz == null) {
                    for (int i = 0; i < ivc.valTimes.length; ++i) {
                        ratedTsc.times[i] = Conversion.toMinutes(ivc.valTimes[i]);
                    }
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeZone(tz);
                    SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyyyy, HH:mm");
                    HecTime t = new HecTime();
                    for (int i = 0; i < ivc.valTimes.length; ++i) {
                        cal.setTimeInMillis(ivc.valTimes[i]);
                        t.set(sdf.format(cal.getTime()));
                        ratedTsc.times[i] = t.value();
                    }
                }
            }
            ratedTsc.values = depVals;
            ratedTsc.numberValues = ratedTsc.times.length;
            String paramStr = ratingSpec.getDepParameter();
            if (ratedTsc.fullName.startsWith("/")) {
                paramStr = paramStr.toUpperCase();
            }
            if (tscs[0].subParameter == null || tscs[0].subParameter.length() == 0) {
                ratedTsc.fullName = replaceAll(tscs[0].fullName, tscs[0].parameter, paramStr, "IL");
            } else {
                ratedTsc.fullName = replaceAll(tscs[0].fullName, String.format("%s-%s", tscs[0].parameter, tscs[0].subParameter), paramStr, "IL");
            }
            String[] parts = split(paramStr, "-", "L", 2);
            ratedTsc.parameter = parts[0];
            ratedTsc.subParameter = parts.length > 1 ? parts[1] : null;
            ratedTsc.units = ratedUnitStr;
            return ratedTsc;
        } catch (Throwable t) {
            if (t instanceof RatingException) {
                throw (RatingException) t;
            }
            throw new RatingException(t);
        }
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(hec.hecmath.TimeSeriesMath)
     */
    @Override
    public final TimeSeriesMath rate(TimeSeriesMath tsm) throws RatingException {
        return rate(tsm, null);
    }

    /**
     * Rates the values in a TimeSeriesMath and returns the results in a new TimeSeriesMath with the specified unit.
     * The rating must be for a single independent parameter.
     *
     * @param tsm          The TimeSeriesMath to rate
     * @param ratedUnitStr The unit to return the rated values in.
     * @return A TimeSeriesMath of the rated values. The rated unit is the specified unit.
     * @throws RatingException
     */
    @Override
    public final TimeSeriesMath rate(TimeSeriesMath tsm, String ratedUnitStr) throws RatingException {
        try {
            return new TimeSeriesMath(rate((TimeSeriesContainer) tsm.getData(), ratedUnitStr));
        } catch (Throwable t) {
            if (t instanceof RatingException) {
                throw (RatingException) t;
            }
            throw new RatingException(t);
        }
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#rate(hec.hecmath.TimeSeriesMath[])
     */
    @Override
    public final TimeSeriesMath rate(TimeSeriesMath[] tsms) throws RatingException {
        return rate(tsms, null);
    }

    /**
     * Rates the values in a set of TimeSeriesMaths and returns the results in a new TimeSeriesMath with the specified unit.
     * The rating must be for as many independent parameters as the number of TimeSeriesMaths.
     * If all the TimeSeriesMaths have the same interval the rated TimeSeriesMath will have the same interval, otherwise
     * the rated TimeSeriesMath will have an interval of 0 (irregular).  The rated TimeSeriesMath will have values
     * only at times that are common to all the input TimeSeriesMaths.
     *
     * @param tsms         The TimeSeriesMaths to rate, in order of the independent parameters of the rating.
     * @param ratedUnitStr The unit to return the rated values in.
     * @return A TimeSeriesMath of the rated values. The rated unit is the specified unit.
     * @throws RatingException
     */
    @Override
    public final TimeSeriesMath rate(TimeSeriesMath[] tsms, String ratedUnitStr) throws RatingException {
        TimeSeriesContainer[] tscs = new TimeSeriesContainer[tsms.length];
        try {
            for (int i = 0; i < tsms.length; ++i) {
                tscs[i] = (TimeSeriesContainer) tsms[i].getData();
            }
            return new TimeSeriesMath(rate(tscs, ratedUnitStr));
        } catch (Throwable t) {
            if (t instanceof RatingException) {
                throw (RatingException) t;
            }
            throw new RatingException(t);
        }
    }

    /**
     * Retrieves the rating specification including all meta data.
     *
     * @return The rating specification
     */
    @Override
    public final synchronized RatingSpec getRatingSpec() {
        return ratingSpec;
    }


    /**
     * Returns the unique identifying parts for the rating specification.
     *
     * @return
     * @throws DataSetException
     */
    @Override
    public final synchronized IRatingSpecification getRatingSpecification() throws DataSetException {
        IRatingSpecification ratingSpecification = getRatingSpec().getRatingSpecification();
        return ratingSpecification;
    }

    /**
     * Returns the unique identifying parts for the rating template.
     *
     * @return
     * @throws DataSetException
     */
    @Override
    public final synchronized IRatingTemplate getRatingTemplate() throws DataSetException {
        IRatingTemplate ratingTemplate = getRatingSpec().getRatingTemplate();
        return ratingTemplate;
    }

    /**
     * Sets the rating specification.
     *
     * @param ratingSpec The rating specification
     * @throws RatingException
     */
    @Override
    public synchronized final void setRatingSpec(RatingSpec ratingSpec) throws RatingException {
        if (ratings != null && ratings.size() > 0) {
            if (ratingSpec.getIndParamCount() != ratings.firstEntry().getValue().getIndParamCount()) {
                throw new RatingException("Number of independent parameters does not match existing ratings");
            }
        }
        if (ratingSpec != null) {
            switch (ratingSpec.getInRangeMethod()) {
                case LOGARITHMIC:
                case LOG_LIN:
                case LIN_LOG:
                    throw new RatingException("Invalid in-range rating method for rating times: " + ratingSpec.getInRangeMethod());
                default:
                    break;
            }
            switch (ratingSpec.getOutRangeLowMethod()) {
                case LOGARITHMIC:
                case LOG_LIN:
                case LIN_LOG:
                case PREVIOUS:
                    throw new RatingException("Invalid out-of-range low rating method for rating times: " + ratingSpec.getOutRangeLowMethod());
                default:
                    break;
            }
            switch (ratingSpec.getOutRangeHighMethod()) {
                case LOGARITHMIC:
                case LOG_LIN:
                case LIN_LOG:
                case NEXT:
                    throw new RatingException("Invalid out-of-range high rating method for rating times: " + ratingSpec.getOutRangeHighMethod());
                default:
                    break;
            }
        }
        this.ratingSpec = ratingSpec;
        for (Long effectiveDate : ratings.keySet()) {
            ratings.get(effectiveDate).ratingSpec = this.ratingSpec;
        }
        for (Long effectiveDate : activeRatings.keySet()) {
            activeRatings.get(effectiveDate).ratingSpec = this.ratingSpec;
        }
    }

    /**
     * Retrieves the times series of ratings.
     *
     * @return The times series of ratings.
     */
    @Override
    public final synchronized AbstractRating[] getRatings() {
        return ratings.values().toArray(new AbstractRating[ratings.size()]);
    }

    @Override
    public NavigableSet<AbstractRating> getRatingsSorted() {
        TreeSet<AbstractRating> retval = new TreeSet<>(Comparator.comparing(AbstractRating::getEffectiveDate));
        retval.addAll(ratings.values());
        return retval;
    }

    @Override
    public final synchronized TreeMap<Long, AbstractRating> getRatingsMap() {
        return ratings;
    }

    @Override
    public final synchronized AbstractRating getRating(Long effectiveDate) {
        AbstractRating retval = null;
        if (ratings != null) {
            retval = ratings.get(effectiveDate);
        }
        return retval;
    }

    @Override
    public final synchronized AbstractRating getFloorRating(Long effectiveDate) {
        AbstractRating retval = null;
        if (ratings != null && effectiveDate != null) {
            Entry<Long, AbstractRating> floorEntry = ratings.floorEntry(effectiveDate);
            if (floorEntry != null) {
                retval = floorEntry.getValue();
            }
        }
        return retval;
    }

    /**
     * Sets the times series of ratings, replacing any existing ratings.
     *
     * @param ratings The time series of ratings
     * @throws RatingException
     */
    @Override
    public final synchronized void setRatings(AbstractRating[] ratings) throws RatingException {
        removeAllRatings();
        addRatings(ratings);
    }

    /**
     * Retrieves the number of ratings in this set.
     *
     * @return The number of ratings in this set
     */
    @Override
    public final synchronized int getRatingCount() {
        return ratings == null ? 0 : ratings.size();
    }

    /**
     * Retrieves the number of active ratings in this set.
     *
     * @return The number of active ratings in this set
     */
    @Override
    public final synchronized int getActiveRatingCount() {
        int count = ratings.size();
        for (AbstractRating abstractRating : ratings.values()) {
            if (!abstractRating.active) {
                --count;
            }
        }
        return count;
    }

    /**
     * Retrieves the default value time. This is used for rating values that have no inherent times.
     *
     * @return The default value time
     * @deprecated Use {@link AbstractRatingSet#getDefaultValueTime()} instead
     */
    @Deprecated
    @Override
    public final synchronized long getDefaultValuetime() {
        return defaultValueTime;
    }

    /**
     * Resets the default value time. This is used for rating values that have no inherent times.
     */
    @Override
    public final synchronized void resetDefaultValuetime() {
        resetDefaultValueTime();
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#getRatingTime()
     */
    @Override
    public final synchronized long getRatingTime() {
        return ratingTime;
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#setRatingTime(long)
     */
    @Override
    public final synchronized void setRatingTime(long ratingTime) {
        this.ratingTime = ratingTime;
        for (AbstractRating rating : ratings.values()) {
            rating.setRatingTime(ratingTime);
        }
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#resetRatingtime()
     */
    @Override
    public final synchronized void resetRatingTime() {
        ratingTime = Long.MAX_VALUE;
        Long[] effectiveDates = ratings.keySet().toArray(new Long[0]);
        for (Long effectiveDate : effectiveDates) {
            ratings.get(effectiveDate).resetRatingTime();
        }
    }

    /**
     * Retrieves whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
     *
     * @return A flag specifying whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
     */
    @Override
    public final synchronized boolean doesAllowUnsafe() {
        return allowUnsafe;
    }

    /**
     * Sets whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
     *
     * @param allowUnsafe A flag specifying whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
     */
    @Override
    public final synchronized void setAllowUnsafe(boolean allowUnsafe) {
        this.allowUnsafe = allowUnsafe;
        for (AbstractRating ar : ratings.values()) {
            ar.setAllowUnsafe(allowUnsafe);
        }
    }

    /**
     * Retrieves whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
     *
     * @return A flag specifying whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
     */
    @Override
    public final synchronized boolean doesWarnUnsafe() {
        return warnUnsafe;
    }

    /**
     * Sets whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
     *
     * @param warnUnsafe A flag specifying whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
     */
    @Override
    public final synchronized void setWarnUnsafe(boolean warnUnsafe) {
        this.warnUnsafe = warnUnsafe;
        for (AbstractRating ar : ratings.values()) {
            ar.setWarnUnsafe(allowUnsafe);
        }
    }

    /**
     * Retrieves the standard HEC-DSS pathname for this rating set
     *
     * @return The standard HEC-DSS pathname for this rating set
     */
    @Override
    public final synchronized String getDssPathname() {
        return ratingSpec == null ? null : ratingSpec.getDssPathname();
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#getName()
     */
    @Override
    public final synchronized String getName() {
        String name = null;
        if (ratingSpec == null) {
            if (ratings.size() > 0) {
                name = ratings.firstEntry().getValue().getName();
            }
        } else {
            name = ratingSpec.getRatingSpecId();
        }
        return name;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#setName(java.lang.String)
     */
    @Override
    public final synchronized void setName(String name) throws RatingException {
        for (AbstractRating rating : ratings.values()) {
            rating.setName(name);
        }
        if (ratingSpec != null) {
            String[] parts = split(name, SEPARATOR1, "L");
            ratingSpec.setLocationId(parts[0]);
            ratingSpec.setParametersId(parts[1]);
            ratingSpec.setTemplateVersion(parts[2]);
            ratingSpec.setVersion(parts[3]);
        }
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#getRatingParameters()
     */
    @Override
    public final synchronized String[] getRatingParameters() {
        return getRatingSpec().getParameters();
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#getRatingUnits()
     */
    @Override
    public synchronized String[] getRatingUnits() {
        String[] units = null;
        if (ratings.size() > 0) {
            units = ratings.firstEntry().getValue().getRatingUnits();
        }
        return units;
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#getDataUnits()
     */
    @Override
    public synchronized String[] getDataUnits() {
        String[] units = null;
        if (dataUnits != null) {
            units = Arrays.copyOf(dataUnits, dataUnits.length);
        } else {
            if (ratings.size() > 0) {
                units = ratings.firstEntry().getValue().getDataUnits();
            }
        }
        return units;
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#setDataUnits(java.lang.String[])
     */
    @Override
    public synchronized void setDataUnits(String[] units) throws RatingException {
        for (AbstractRating rating : ratings.values()) {
            rating.setDataUnits(units);
        }
        dataUnits = units == null ? null : Arrays.copyOf(units, units.length);
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getRatingExtents()
     */
    @Override
    public double[][] getRatingExtents() throws RatingException {
        return getRatingExtents(getRatingTime());
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getRatingExtents(long)
     */
    @Override
    public synchronized double[][] getRatingExtents(long ratingTime) throws RatingException {
        if (activeRatings.size() == 0) {
            throw new RatingException("No active ratings.");
        }
        Entry<Long, AbstractRating> rating = activeRatings.floorEntry(ratingTime);

        if (rating == null) {
            rating = activeRatings.ceilingEntry(ratingTime);
        }
        return rating.getValue().getRatingExtents();
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getEffectiveDates()
     */
    @Override
    public synchronized long[] getEffectiveDates() {
        long[] effectiveDates = new long[activeRatings.size()];
        Iterator<AbstractRating> it = activeRatings.values().iterator();
        for (int i = 0; i < effectiveDates.length; ++i) {
            effectiveDates[i] = it.next().effectiveDate;
        }
        return effectiveDates;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#getCreateDates()
     */
    @Override
    public synchronized long[] getCreateDates() {
        long[] createDates = new long[activeRatings.size()];
        Iterator<AbstractRating> it = activeRatings.values().iterator();
        for (int i = 0; i < createDates.length; ++i) {
            createDates[i] = it.next().createDate;
        }
        return createDates;
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#getDefaultValueTime()
     */
    @Override
    public final synchronized long getDefaultValueTime() {
        return defaultValueTime;
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#setDefaultValueTime(long)
     */
    @Override
    public final synchronized void setDefaultValueTime(long defaultValueTime) {
        this.defaultValueTime = defaultValueTime;
        for (ICwmsRating rating : ratings.values()) {
            if (rating != null) {
                rating.setDefaultValueTime(defaultValueTime);
            }
        }
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rate(double)
     */
    @Override
    public final synchronized double rate(double indVal) throws RatingException {
        if (defaultValueTime == Const.UNDEFINED_TIME) {
            throw new RatingException("Default value time is not set");
        }
        return rate(indVal, defaultValueTime);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rate(double[])
     */
    @Override
    public final synchronized double rateOne(double... indVals) throws RatingException {
        if (defaultValueTime == Const.UNDEFINED_TIME) {
            throw new RatingException("Default value time is not set");
        }
        return rateOne(indVals, defaultValueTime);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rate(double[])
     */
    @Override
    public final synchronized double rateOne2(double[] indVals) throws RatingException {
        if (defaultValueTime == Const.UNDEFINED_TIME) {
            throw new RatingException("Default value time is not set");
        }
        return rateOne(indVals, defaultValueTime);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rateOne(double[])
     */
    @Override
    public final synchronized double[] rate(double[] indVals) throws RatingException {
        if (defaultValueTime == Const.UNDEFINED_TIME) {
            throw new RatingException("Default value time is not set");
        }
        return rate(indVals, defaultValueTime);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rate(double[][])
     */
    @Override
    public final synchronized double[] rate(double[][] indVals) throws RatingException {
        if (defaultValueTime == Const.UNDEFINED_TIME) {
            throw new RatingException("Default value time is not set");
        }
        long[] valTimes = new long[indVals.length];
        Arrays.fill(valTimes, defaultValueTime);
        return rate(indVals, valTimes);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rate(long, double)
     */
    @Override
    public final synchronized double rate(long valTime, double indVal) throws RatingException {
        return rate(indVal, valTime);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rate(long, double[])
     */
    @Override
    public final synchronized double rateOne(long valTime, double... indVals) throws RatingException {
        return rateOne(indVals, valTime);
    }

    @Override
    public double[] rateOne(long[] valueTimes, double[] indVals) throws RatingException {
        return rateOne(indVals, valueTimes);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rate(long, double[])
     */
    @Override
    public final synchronized double rateOne2(long valTime, double... indVals) throws RatingException {
        return rateOne(indVals, valTime);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rateOne(long, double[])
     */
    @Override
    public final synchronized double[] rate(long valTime, double[] indVals) throws RatingException {
        return rate(indVals, valTime);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rateOne(long[], double[])
     */
    @Override
    public final synchronized double[] rate(long[] valTimes, double[] indVals) throws RatingException {
        return rateOne(indVals, valTimes);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rate(long, double[][])
     */
    @Override
    public final synchronized double[] rate(long valTime, double[][] indVals) throws RatingException {
        long[] valTimes = new long[indVals.length];
        Arrays.fill(valTimes, valTime);
        return rate(indVals, valTimes);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#rate(long[], double[][])
     */
    @Override
    public final synchronized double[] rate(long[] valTimes, double[][] indVals) throws RatingException {
        return rate(indVals, valTimes);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#reverseRate(double)
     */
    @Override
    public final synchronized double reverseRate(double depVal) throws RatingException {
        if (defaultValueTime == Const.UNDEFINED_TIME) {
            throw new RatingException("Default value time is not set");
        }
        long[] valTimes = {defaultValueTime};
        double[] depVals = {depVal};
        return reverseRate(valTimes, depVals)[0];
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#reverseRate(double[])
     */
    @Override
    public final synchronized double[] reverseRate(double[] depVals) throws RatingException {
        if (defaultValueTime == Const.UNDEFINED_TIME) {
            throw new RatingException("Default value time is not set");
        }
        return reverseRate(defaultValueTime, depVals);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#reverseRate(long, double)
     */
    @Override
    public final synchronized double reverseRate(long valTime, double depVal) throws RatingException {
        long[] valTimes = {valTime};
        double[] depVals = {depVal};
        return reverseRate(valTimes, depVals)[0];
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#reverseRate(long, double[])
     */
    @Override
    public final synchronized double[] reverseRate(long valTime, double[] depVals) throws RatingException {
        long[] valTimes = new long[depVals.length];
        Arrays.fill(valTimes, valTime);
        return reverseRate(valTimes, depVals);
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#reverseRate(long[], double[])
     */
    @Override
    public synchronized double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException {
        double[] Y = new double[depVals.length];
        if (activeRatings.size() == 0) {
            throw new RatingException("No active ratings.");
        }
        if (getDataUnits() == null) {
            Entry<Long, AbstractRating> entry1 = ratings.firstEntry();
            Entry<Long, AbstractRating> entry2 = ratings.higherEntry(entry1.getKey());
            while (entry2 != null) {
                if (!(entry1.getValue().getRatingUnitsId().equalsIgnoreCase(entry2.getValue().getRatingUnitsId()))) {
                    throw new RatingException("Data units must be specified when rating set has multiple rating units.");
                }
                entry1 = entry2;
                entry2 = ratings.higherEntry(entry1.getKey());
            }
        }
        Entry<Long, AbstractRating> lowerRating = null;
        Entry<Long, AbstractRating> upperRating = null;
        IRating lastUsedRating = null;
        RatingMethod method = null;
        for (int i = 0; i < depVals.length; ++i) {
            if (i > 0 && valTimes[i] == valTimes[i - 1]) {
                if (lastUsedRating == null) {
                    Y[i] = Y[i - 1];
                } else {
                    Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                }
                continue;
            } else {
                lowerRating = activeRatings.floorEntry(valTimes[i]);
                upperRating = activeRatings.ceilingEntry(valTimes[i]);
                //-------------------------//
                // handle out of range low //
                //-------------------------//
                if (lowerRating == null) {
                    method = ratingSpec.getOutRangeLowMethod();
                    switch (method) {
                        case ERROR:
                            throw new RatingException("Effective date is before earliest rating");
                        case NULL:
                            Y[i] = Const.UNDEFINED_DOUBLE;
                            lastUsedRating = null;
                            continue;
                        case NEXT:
                        case NEAREST:
                        case HIGHER:
                        case CLOSEST:
                            lastUsedRating = activeRatings.firstEntry().getValue();
                            Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                            continue;
                        default:
                            break;
                    }
                    if (activeRatings.size() == 1) {
                        throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
                    }
                    lowerRating = activeRatings.firstEntry();
                    upperRating = activeRatings.higherEntry(lowerRating.getKey());
                }
                //--------------------------//
                // handle out of range high //
                //--------------------------//
                if (upperRating == null) {
                    method = ratingSpec.getOutRangeHighMethod();
                    switch (method) {
                        case ERROR:
                            throw new RatingException("Effective date is after latest rating");
                        case NULL:
                            Y[i] = Const.UNDEFINED_DOUBLE;
                            lastUsedRating = null;
                            continue;
                        case PREVIOUS:
                        case NEAREST:
                        case LOWER:
                        case CLOSEST:
                            lastUsedRating = activeRatings.lastEntry().getValue();
                            Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                            continue;
                        default:
                            break;
                    }
                    if (activeRatings.size() == 1) {
                        switch (method) {
                            case LINEAR:
                                //-----------------------------------------------------------------//
                                // allow LINEAR out of range high method with single active rating //
                                //-----------------------------------------------------------------//
                                lastUsedRating = activeRatings.lastEntry().getValue();
                                Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                                continue;
                            default:
                                throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
                        }
                    }
                    upperRating = activeRatings.lastEntry();
                    lowerRating = activeRatings.lowerEntry(upperRating.getKey());
                }
                //-----------------------------------//
                // handle in-range and extrapolation //
                //-----------------------------------//
                if (lowerRating.getKey() == valTimes[i]) {
                    Y[i] = lowerRating.getValue().reverseRate(valTimes[i], depVals[i]);
                    continue;
                }
                if (upperRating.getKey() == valTimes[i]) {
                    lastUsedRating = upperRating.getValue();
                    Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                    continue;
                }
                switch (ratingSpec.getInRangeMethod()) {
                    case ERROR:
                        throw new RatingException("Effective date is between existing rating");
                    case NULL:
                        Y[i] = Const.UNDEFINED_DOUBLE;
                        lastUsedRating = null;
                        continue;
                    case PREVIOUS:
                    case LOWER:
                        lastUsedRating = lowerRating.getValue();
                        Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                        continue;
                    case NEXT:
                    case HIGHER:
                        lastUsedRating = upperRating.getValue();
                        Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                        continue;
                    case CLOSEST:
                        if (valTimes[i] - lowerRating.getKey() < upperRating.getKey() - valTimes[i]) {
                            lastUsedRating = lowerRating.getValue();
                        } else {
                            lastUsedRating = upperRating.getValue();
                        }
                        Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
                        continue;
                    default:
                        break;
                }
                //------------------------------------//
                // handle interpolation/extrapolation //
                //------------------------------------//
                method = ratingSpec.getInRangeMethod();
            }
            lastUsedRating = null;
            boolean ind_log = method == RatingMethod.LOGARITHMIC || method == RatingMethod.LIN_LOG;
            boolean dep_log = method == RatingMethod.LOGARITHMIC || method == RatingMethod.LOG_LIN;
            double x = valTimes[i];
            double x1 = lowerRating.getKey();
            double x2 = upperRating.getKey();
            double Y1 = lowerRating.getValue().reverseRate(valTimes[i], depVals[i]);
            double Y2 = upperRating.getValue().reverseRate(valTimes[i], depVals[i]);
            double y1 = Y1;
            double y2 = Y2;
            if (ind_log) {
                x = Math.log10(x);
                x1 = Math.log10(x1);
                x2 = Math.log10(x2);
                if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(x1) || Double.isInfinite(x1) || Double.isNaN(x2) ||
                    Double.isInfinite(x2)) {
                    //-------------------------------------------------//
                    // fall back from LOGARITHMIC or LOG_LIN to LINEAR //
                    //-------------------------------------------------//
                    x = valTimes[i];
                    x1 = lowerRating.getKey();
                    x2 = upperRating.getKey();
                    dep_log = false;
                }
            }
            if (dep_log) {
                y1 = Math.log10(y1);
                y2 = Math.log10(y2);
                if (Double.isNaN(y1) || Double.isInfinite(y1) || Double.isNaN(y2) || Double.isInfinite(y2)) {
                    //-------------------------------------------------//
                    // fall back from LOGARITHMIC or LIN_LOG to LINEAR //
                    //-------------------------------------------------//
                    x = valTimes[i];
                    x1 = lowerRating.getKey();
                    x2 = upperRating.getKey();
                    y1 = Y1;
                    y2 = Y2;
                    dep_log = false;
                }
            }
            double y = y1 + ((x - x1) / (x2 - x1)) * (y2 - y1);
            if (dep_log) {
                y = Math.pow(10, y);
            }
            Y[i] = y;
        }
        return Y;
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#reverseRate(hec.io.TimeSeriesContainer)
     */
    @Override
    public final synchronized TimeSeriesContainer reverseRate(TimeSeriesContainer tsc) throws RatingException {
        TimeSeriesContainer[] tscs = {tsc};
        String[] units = {tsc.units};
        TimeZone tz = null;
        if (tsc.timeZoneID != null) {
            tz = TimeZone.getTimeZone(tsc.timeZoneID);
            if (!tz.getID().equals(tsc.timeZoneID)) {
                String msg = String.format("TimeSeriesContainers have invalid time zone \"%s\".", tsc.timeZoneID);
                if (!allowUnsafe) {
                    throw new RatingException(msg);
                }
                if (warnUnsafe) {
                    LOGGER.warning(msg + "  Value times will be treated as UTC.");
                }
                tz = null;
            }
        }
        IndependentValuesContainer ivc = RatingUtil.tscsToIvc(tscs, units, tz, allowUnsafe, warnUnsafe);
        TimeSeriesContainer ratedTsc = new TimeSeriesContainer();
        tsc.clone(ratedTsc);
        double[] depVals = new double[ivc.indVals.length];
        for (int i = 0; i < depVals.length; ++i) {
            depVals[i] = ivc.indVals[i][0];
        }
        ratedTsc.values = reverseRate(ivc.valTimes, depVals);
        String[] params = getRatingParameters();
        String paramStr = params[0];
        if (tsc.subParameter == null) {
            ratedTsc.fullName = replaceAll(tsc.fullName, tsc.parameter, paramStr, "IL");
        } else {
            ratedTsc.fullName = replaceAll(tsc.fullName, String.format("%s-%s", tsc.parameter, tsc.subParameter), paramStr, "IL");
        }
        String[] parts = split(paramStr, "-", "L", 2);
        ratedTsc.parameter = parts[0];
        ratedTsc.subParameter = parts.length > 1 ? parts[1] : null;
        String[] dataUnits = getDataUnits();
        ratedTsc.units = dataUnits == null ? getRatingUnits()[0] : dataUnits[0];
        return ratedTsc;
    }

    /* (non-Javadoc)
     * @see hec.data.IRating#reverseRate(hec.hecmath.TimeSeriesMath)
     */
    @Override
    public final synchronized TimeSeriesMath reverseRate(TimeSeriesMath tsm) throws RatingException {
        try {
            return new TimeSeriesMath(reverseRate((TimeSeriesContainer) tsm.getData()));
        } catch (Throwable t) {
            if (t instanceof RatingException) {
                throw (RatingException) t;
            }
            throw new RatingException(t);
        }
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.IRating#getIndParamCount()
     */
    @Override
    public final synchronized int getIndParamCount() throws RatingException {
        return ratingSpec != null ? ratingSpec.getIndParamCount() : ratings.firstEntry().getValue().getIndParamCount();
    }

    /* (non-Javadoc)
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(java.util.Observable arg0, Object arg1) {
        observationTarget.setChanged();
        observationTarget.notifyObservers();
    }

    /**
     * Adds an Observer to this RatingSet. The Observer will be notified of any changes to this RatingSet
     *
     * @param o The Observer object to add
     * @see Observer
     */
    @Override
    public synchronized final void addObserver(Observer o) {
        observationTarget.addObserver(o);
    }

    /**
     * Deletes an Observer from this RatingSet. The Observer will no longer be notified of any changes to this RatingSet
     *
     * @param o The Observer object to delete
     * @see Observer
     */
    @Override
    public synchronized final void deleteObserver(Observer o) {
        observationTarget.deleteObserver(o);
    }

    /**
     * Retrieves a RatingSetContainer containing the data of this object.
     *
     * @return The RatingSetContainer
     */
    @Override
    public synchronized RatingSetContainer getData() {
        RatingSetContainer rsc = new RatingSetContainer();
        if (ratingSpec != null) {
            rsc.ratingSpecContainer = ratingSpec.getData();
        }
        if (ratings.size() > 0) {
            rsc.abstractRatingContainers = new AbstractRatingContainer[ratings.size()];
            Iterator<AbstractRating> it = ratings.values().iterator();
            for (int i = 0; it.hasNext(); ++i) {
                rsc.abstractRatingContainers[i] = it.next().getData();
            }
        }
        return rsc;
    }

    /**
     * Sets the data from this object from a RatingSetContainer
     *
     * @param rsc The RatingSetContainer with the data
     * @throws RatingException any errors transferring the data
     */
    @Override
    public synchronized void setData(RatingSetContainer rsc) throws RatingException {
        try {
            removeAllRatings();
            if (rsc.ratingSpecContainer == null) {
                ratingSpec = null;
            } else {
                setRatingSpec(new RatingSpec(rsc.ratingSpecContainer));
            }
            if (rsc.abstractRatingContainers == null) {
                throw new RatingObjectDoesNotExistException("RatingSetContainer contains no ratings.");
            } else {
                for (int i = 0; i < rsc.abstractRatingContainers.length; ++i) {
                    addRating(rsc.abstractRatingContainers[i].newRating());
                }
            }
            if (observationTarget != null) {
                observationTarget.setChanged();
                observationTarget.notifyObservers();
            }
        } catch (RuntimeException t) {
            throw new RatingException(t);
        }
    }

    /**
     * Returns whether this object has any vertical datum info
     *
     * @return whether this object has any vertical datum info
     */
    @Override
    public boolean hasVerticalDatum() {
        try {
            return getNativeVerticalDatum() != null;
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            return false;
        }
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNativeVerticalDatum()
     */
    @Override
    public String getNativeVerticalDatum() throws VerticalDatumException {
        return getData().getNativeVerticalDatum();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getCurrentVerticalDatum()
     */
    @Override
    public String getCurrentVerticalDatum() throws VerticalDatumException {
        return getData().getCurrentVerticalDatum();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#isCurrentVerticalDatumEstimated()
     */
    @Override
    public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
        return getData().isCurrentVerticalDatumEstimated();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toNativeVerticalDatum()
     */
    @Override
    public boolean toNativeVerticalDatum() throws VerticalDatumException {
        RatingSetContainer rsc = getData();
        boolean change = rsc.toNativeVerticalDatum();
        if (change) {
            try {
                setData(rsc);
            } catch (RatingException e) {
                throw new VerticalDatumException(e);
            }
        }
        return change;
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toNGVD29()
     */
    @Override
    public boolean toNGVD29() throws VerticalDatumException {
        RatingSetContainer rsc = getData();
        boolean change = rsc.toNGVD29();
        if (change) {
            try {
                setData(rsc);
            } catch (RatingException e) {
                throw new VerticalDatumException(e);
            }
        }
        return change;
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toNAVD88()
     */
    @Override
    public boolean toNAVD88() throws VerticalDatumException {
        RatingSetContainer rsc = getData();
        boolean change = rsc.toNAVD88();
        if (change) {
            try {
                setData(rsc);
            } catch (RatingException e) {
                throw new VerticalDatumException(e);
            }
        }
        return change;
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#toVerticalDatum(java.lang.String)
     */
    @Override
    public boolean toVerticalDatum(String datum) throws VerticalDatumException {
        RatingSetContainer rsc = getData();
        boolean change = rsc.toVerticalDatum(datum);
        if (change) {
            try {
                setData(rsc);
            } catch (RatingException e) {
                throw new VerticalDatumException(e);
            }
        }
        return change;
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#forceVerticalDatum(java.lang.String)
     */
    @Override
    public boolean forceVerticalDatum(String datum) throws VerticalDatumException {
        boolean change;
        RatingSetContainer rsc = getData();
        change = rsc.forceVerticalDatum(datum);
        if (change) {
            try {
                setData(rsc);
            } catch (RatingException e) {
                throw new VerticalDatumException(e);
            }
        }
        return change;
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getCurrentOffset()
     */
    @Override
    public double getCurrentOffset() throws VerticalDatumException {
        return getData().getCurrentOffset();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getCurrentOffset(java.lang.String)
     */
    @Override
    public double getCurrentOffset(String unit) throws VerticalDatumException {
        return getData().getCurrentOffset(unit);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNGVD29Offset()
     */
    @Override
    public double getNGVD29Offset() throws VerticalDatumException {
        return getData().getNGVD29Offset();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNGVD29Offset(java.lang.String)
     */
    @Override
    public double getNGVD29Offset(String unit) throws VerticalDatumException {
        return getData().getNGVD29Offset(unit);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNAVD88Offset()
     */
    @Override
    public double getNAVD88Offset() throws VerticalDatumException {
        return getData().getNAVD88Offset();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getNAVD88Offset(java.lang.String)
     */
    @Override
    public double getNAVD88Offset(String unit) throws VerticalDatumException {
        return getData().getNAVD88Offset(unit);
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#isNGVD29OffsetEstimated()
     */
    @Override
    public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
        return getData().isNGVD29OffsetEstimated();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#isNAVD88OffsetEstimated()
     */
    @Override
    public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
        return getData().isNAVD88OffsetEstimated();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#getVerticalDatumInfo()
     */
    @Override
    public String getVerticalDatumInfo() throws VerticalDatumException {
        return getData().getVerticalDatumInfo();
    }

    /* (non-Javadoc)
     * @see hec.data.VerticalDatum#setVerticalDatumInfo(java.lang.String)
     */
    @Override
    public void setVerticalDatumInfo(String xmlStr) throws VerticalDatumException {
        for (AbstractRating ar : ratings.values()) {
            ar.setVerticalDatumInfo(xmlStr);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        boolean same = obj == this || (obj instanceof AbstractRatingSet && ((AbstractRatingSet) obj).allowUnsafe == allowUnsafe &&
            ((AbstractRatingSet) obj).warnUnsafe == warnUnsafe && ((AbstractRatingSet) obj).defaultValueTime == defaultValueTime &&
            ((AbstractRatingSet) obj).ratingTime == ratingTime && getData().equals(((AbstractRatingSet) obj).getData()) &&
            (((AbstractRatingSet) obj).activeRatings == null) == (activeRatings == null));
        if (same) {
            if (activeRatings != null) {
                if (((AbstractRatingSet) obj).activeRatings.size() != activeRatings.size()) {
                    return false;
                }
                for (Long key : activeRatings.keySet()) {
                    if (!((AbstractRatingSet) obj).activeRatings.containsKey(key)) {
                        return false;
                    }
                    if (!((AbstractRatingSet) obj).activeRatings.get(key).equals(activeRatings.get(key))) {
                        return false;
                    }
                }
            }
        }
        return same;
    }

    /* (non-Javadoc)
     * @see hec.data.cwmsRating.AbstractRating#hashCode()
     */
    @Override
    public int hashCode() {
        int hashCode = getClass().getName().hashCode() + 11 * (allowUnsafe ? 1 : 11) + 13 * (warnUnsafe ? 1 : 13) + 17 * (int) defaultValueTime +
            19 * (int) ratingTime + getData().hashCode();
        if (activeRatings != null) {
            Iterator<AbstractRating> it = activeRatings.values().iterator();
            for (int i = 0; it.hasNext(); ++i) {
                hashCode += 23 * i * it.next().hashCode();
            }
        }
        return hashCode;
    }

    /**
     * Validates the rating set
     *
     * @throws RatingException if the rating set is not valid
     */
    protected synchronized void validate() throws RatingException {
        if (ratings.size() == 0) {
            return;
        }
        String unitsId = ratings.firstEntry().getValue().getRatingUnitsId();
        String[] units = unitsId == null ? null : split(unitsId.replace(SEPARATOR2, SEPARATOR3), SEPARATOR3, "L");
        String[] params = null;
        boolean[] validParams = null;
        boolean[] validUnits = null;
        try {
            if (ratingSpec != null) {
                params = ratingSpec.getIndParameters();
                validParams = new boolean[ratingSpec.getIndParamCount() + 1];
                validUnits = new boolean[ratingSpec.getIndParamCount() + 1];
                Parameter ratingParam = null;
                Units ratingUnit = null;
                for (int i = 0; i < params.length; ++i) {
                    ratingParam = null;
                    validParams[i] = false;
                    if (ratingSpec != null) {
                        try {
                            ratingParam = new Parameter(params[i]);
                            validParams[i] = true;
                        } catch (Throwable t) {
                            if (!allowUnsafe) {
                                throw new RatingException(t);
                            }
                            if (warnUnsafe) {
                                LOGGER.warning(t.getMessage());
                            }
                        }
                    }
                }
                if (units != null) {
                    for (int i = 0; i < units.length; ++i) {
                        ratingUnit = null;
                        validUnits[i] = false;
                        try {
                            ratingUnit = new Units(units[i], true);
                            validUnits[i] = true;
                        } catch (Throwable t) {
                            if (!allowUnsafe) {
                                throw new RatingException(t);
                            }
                            if (warnUnsafe) {
                                LOGGER.warning(t.getMessage());
                            }
                        }
                    }
                }
                for (int i = 0; i < params.length; ++i) {
                    if (validParams[i] && validUnits[i]) {
                        ratingParam = new Parameter(params[i]);
                        ratingUnit = new Units(units[i], true);
                        if (!Units.canConvertBetweenUnits(ratingParam.getUnitsString(), ratingUnit.toString())) {
                            String msg = String.format("Unit \"%s\" is not consistent with parameter \"%s\".", units[i], params[i]);
                            if (!allowUnsafe) {
                                throw new RatingException(msg);
                            }
                            if (warnUnsafe) {
                                LOGGER.warning(msg + "  Rating will be performed on unconverted values.");
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException t) {
            throw new RatingException(t);
        }
    }

    /**
     * If dbrating == null, this method returns the first VerticalDatumContainer found in the AbstractRatings.
     * Otherwise it returns the vertical datum container from the dbrating.
     *
     * @return NULL
     */
    @Override
    public VerticalDatumContainer getVerticalDatumContainer() {
        VerticalDatumContainer retval = null;
        for (AbstractRating ar : ratings.values()) {
            if (ar.hasVerticalDatum()) {
                retval = ar.getVerticalDatumContainer();
                break;
            }
        }
        return retval;
    }

    /**
     * If dbrating == null, this method sets the VerticalDatumContainer on all AbstractRatings.
     * Otherwise it sets the vertical datum container from the dbrating.
     *
     * @param vdc vertical datum data
     */
    @Override
    public void setVerticalDatumContainer(VerticalDatumContainer vdc) {
        for (AbstractRating ar : ratings.values()) {
            ar.setVerticalDatumContainer(vdc);
        }
    }

    /**
     * Retrieves a TextContainer containing the data of this object, suitable for storing to DSS.
     *
     * @return The TextContainer
     * @throws RatingException any errors reading from dss or processing the data
     */
    @Override
    public synchronized TextContainer getDssData() throws RatingException {
        throw new RatingException("getDssData() unsupported. Use factory methods instead");
    }

    @Override
    public String toCompressedXmlString() throws RatingException {
        throw new RatingException("toCompressedXmlString() unsupported. Use factory methods instead");
    }

    @Override
    public String toXmlString(CharSequence indent) throws RatingException {
        throw new RatingException("toXmlString(CharSequence) unsupported. Use factory methods instead");
    }

    @Deprecated
    @Override
    public void getConcreteRatings(Connection conn) throws RatingException {
        //No-op
    }

    @Deprecated
    @Override
    public void getConcreteRatings(long date) throws RatingException {
        //No-op
    }

    @Deprecated
    @Override
    public void getConcreteRatings() throws RatingException {
        //No-op
    }

    @Deprecated
    @Override
    public RatingSetStateContainer getState() {
        //Only supported by JdbcRatingSet implementation
        return null;
    }

    @Override
    public synchronized void resetDefaultValueTime() {
        this.defaultValueTime = Const.UNDEFINED_TIME;
    }
}