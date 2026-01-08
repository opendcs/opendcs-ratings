/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hec.cwms.ratings.g2d;

import java.util.logging.Level;
import java.util.logging.Logger;


import hec.data.cwmsRating.RatingSet;
import hec.geometry.Axis;
import hec.geometry.WorldRect;
import hec.gfx2d.G2dData;
import hec.gfx2d.G2dGlyphFactory;

/**
 *
 * @author Shannon J. Newbold (sjnewbold@rmanet.com)
 */
public class RatingSetG2dObject extends G2dData
{
    static
    {
        G2dGlyphFactory.addMapping(RatingSetG2dObject.class.getName(), RatingSetGlyph.class.getName());
    }
    private final RatingSet mRatingSet;
    private final long mDate;

    public RatingSetG2dObject(RatingSet ratingSet, long date)
    {
        super();
        mRatingSet = ratingSet;
        this.setName(mRatingSet.getName());
        mDate = date;
    }

    @Override
    public WorldRect getBounds()
    {
        try
        {
            double[][] ratingExtents = mRatingSet.getRatingExtents();
            double minY = ratingExtents[0][0];
            double maxY = ratingExtents[1][0];
            double minX = ratingExtents[0][1];
            double maxX = ratingExtents[1][1];

            return new WorldRect(minX, maxY, maxX, minY);

        }
        catch (RatingException ex)
        {
            Logger.getLogger(RatingSetG2dObject.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public String getXAxisName()
    {
        return mRatingSet.getRatingUnits()[1];
    }

    @Override
    public String getYAxisName()
    {
        return mRatingSet.getRatingUnits()[0];
    }

    @Override
    public int getXAxisType()
    {
        return Axis.LINEAR;
    }

    @Override
    public int getYAxisType()
    {
        return Axis.LINEAR;
    }

    @Override
    public void load()
    {
    }

    @Override
    public void unload()
    {
    }

    RatingSet getRatingSet()
    {
        return mRatingSet;
    }

    public long getRatingSetDate()
    {
        return mDate;
    }
}
