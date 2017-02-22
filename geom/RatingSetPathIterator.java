/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hec.data.cwmsRating.geom;

import java.awt.geom.PathIterator;
import java.util.List;

import hec.data.RatingException;

/**
 *
 * @author Shannon J. Newbold (sjnewbold@rmanet.com)
 */
public class RatingSetPathIterator implements PathIterator
{

    private final double mResolution;
    private boolean isDone = false;
    private final List<RangeValue> mRangeValues;
    private final RatingProvider mRatingProvider;
    private int mRangeIndex = 0;
    private double currentRangeValue = 0;

    /**
     * Iterator that iterates over a list of ranges at a given resolution. i.e.
     * for minValue -> maxValue += resolution Each range in the list will be
     * treated as a list as its own path the number of points between
     *
     * @param path
     * @param ratingSet
     * @param date
     * @param resolution
     */
    public RatingSetPathIterator(List<RangeValue> rangeValues, RatingProvider ratingProvider, double resolution)
    {
        mResolution = resolution;
        mRangeValues = rangeValues;
        mRatingProvider = ratingProvider;
        mRangeIndex = 0;
        RangeValue rangeValue = rangeValues.get(mRangeIndex);
        currentRangeValue = rangeValue.minValue;
    }

    @Override
    public int getWindingRule()
    {
        return WIND_NON_ZERO;
    }

    @Override
    public boolean isDone()
    {
        return isDone;
    }

    @Override
    public void next()
    {
        currentRangeValue += mResolution;
        if (currentRangeValue >= mRangeValues.get(mRangeIndex).getMaxValue())
        {
            mRangeIndex++;
            isDone = !(mRangeIndex < mRangeValues.size());
            if (!isDone)
            {
                currentRangeValue = mRangeValues.get(mRangeIndex).getMinValue();
            }
        }
    }

    @Override
    public int currentSegment(float[] coords)
    {
        if (mRangeIndex >= mRangeValues.size())
        {
            return SEG_CLOSE;
        }
        try
        {
            double rateOne = mRatingProvider.rateOne(currentRangeValue);
            coords[0] = (float) rateOne;
            coords[1] = (float) currentRangeValue;
            return mRangeValues.get(mRangeIndex).getMinValue() == currentRangeValue ? SEG_MOVETO : SEG_LINETO;
        }
        catch (RatingException ex)
        {
//            Logger.getLogger(RatingSetPathIterator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return SEG_CLOSE;
    }

    @Override
    public int currentSegment(double[] coords)
    {
        if (mRangeIndex >= mRangeValues.size())
        {
            return SEG_CLOSE;
        }
        try
        {
            double rateOne = mRatingProvider.rateOne(currentRangeValue);
            coords[0] = (float) rateOne;
            coords[1] = (float) currentRangeValue;
            return mRangeValues.get(mRangeIndex).getMinValue() == currentRangeValue ? SEG_MOVETO : SEG_LINETO;
        }
        catch (RatingException ex)
        {
//            Logger.getLogger(RatingSetPathIterator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return SEG_CLOSE;
    }

//    private int getCurrentSegment(double[] coords)
//    {
//        if (mRangeIndex >= mRangeValues.size())
//        {
//            return SEG_CLOSE;
//        }
//        try
//        {
//            double rateOne = mRatingSet.rateOne(currentRangeValue);
//            coords[0] = currentRangeValue;
//            coords[1] = rateOne;
//            return mRangeValues.get(mRangeIndex).getMinValue() == currentRangeValue ? SEG_MOVETO : SEG_LINETO;
//        }
//        catch (RatingException ex)
//        {
//            Logger.getLogger(RatingSetPathIterator.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return SEG_CLOSE;
//    }
    public static interface RatingProvider
    {

        public double rateOne(double... values) throws RatingException;
    }
    public static class RangeValue
    {

        private double minValue;
        private double maxValue;

        public RangeValue(double min, double max)
        {
            minValue = min;
            maxValue = max;
        }

        public double getMinValue()
        {
            return minValue;
        }

        public double getMaxValue()
        {
            return maxValue;
        }
    }
}
