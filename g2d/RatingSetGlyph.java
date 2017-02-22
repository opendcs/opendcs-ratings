/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hec.data.cwmsRating.g2d;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;
import hec.data.cwmsRating.geom.RatingSetPathIterator;
import hec.geometry.Axis;
import hec.geometry.Scale;
import hec.gfx2d.G2dGlyph;
import hec.gfx2d.G2dObject;
import hec.gfx2d.SimpleLogArrayGlyph;
import hec.gfx2d.Viewport;

/**
 *
 * @author Shannon J. Newbold (sjnewbold@rmanet.com)
 */

public class RatingSetGlyph extends G2dGlyph
{

    public RatingSetGlyph(Viewport view, RatingSetG2dObject map, Scale scl)
    {
        super(view, map, scl);
    }

    @Override
    public void draw(Graphics g)
    {
        int xmin = java.lang.Math.min(_scale.getAxis(Axis.XAXIS).w2l(_scale.getAxis(0).getActMin()), _scale.getAxis(Axis.XAXIS).w2l(_scale.getAxis(0).getActMax()));
        int ymax = java.lang.Math.max(_scale.getAxis(Axis.YAXIS).w2l(_scale.getAxis(1).getActMax()), _scale.getAxis(Axis.YAXIS).w2l(_scale.getAxis(1).getActMin()));
        int xmax = java.lang.Math.max(_scale.getAxis(Axis.XAXIS).w2l(_scale.getAxis(0).getActMin()), _scale.getAxis(Axis.XAXIS).w2l(_scale.getAxis(0).getActMax()));
        int ymin = java.lang.Math.min(_scale.getAxis(Axis.YAXIS).w2l(_scale.getAxis(1).getActMax()), _scale.getAxis(Axis.YAXIS).w2l(_scale.getAxis(1).getActMin()));
        Rectangle rect = new Rectangle(xmin, ymin, xmax - xmin, ymax - ymin);

        G2dObject g2dObject = this.getG2dObject();


        double dataViewMax = _scale.getAxis(Axis.YAXIS).getActMax();
        double dataViewMin = _scale.getAxis(Axis.YAXIS).getActMin();

        double dataViewHeight = dataViewMax - dataViewMin;
        double pixels = Math.max(_viewport.getWidth(), _viewport.getHeight());

        double resolution = dataViewHeight / pixels;
        try
        {
            final RatingSet ratingSet = ((RatingSetG2dObject) g2dObject).getRatingSet();
            final long ratingDate = ((RatingSetG2dObject) g2dObject).getRatingSetDate();
            double[][] ratingExtents = ratingSet.getRatingExtents();
            RatingSetPathIterator.RangeValue rangeView = new RatingSetPathIterator.RangeValue(Math.max(ratingExtents[0][0], dataViewMin), Math.min(ratingExtents[1][0], dataViewMax));

            RatingSetPathIterator.RatingProvider rp = new RatingSetPathIterator.RatingProvider()
            {
                @Override
                public double rateOne(double... values) throws RatingException
                {
                    return ratingSet.rateOne(ratingDate, values);
                }
            };

            RatingSetPathIterator pspi = new RatingSetPathIterator(Collections.singletonList(rangeView), rp, resolution);
            double[] values = new double[6];
            int numPoints = 0;
            double[] xarray = new double[1000];
            double[] yarray = new double[1000];
            do
            {
                if (numPoints >= xarray.length)
                {
                    double[] tempx = xarray;
                    double[] tempy = yarray;

                    xarray = new double[xarray.length * 2];
                    yarray = new double[yarray.length * 2];
                    System.arraycopy(tempx, 0, xarray, 0, numPoints);
                    System.arraycopy(tempy, 0, yarray, 0, numPoints);
                }
                pspi.currentSegment(values);
                xarray[numPoints] = values[0];
                yarray[numPoints] = values[1];
                pspi.next();
                numPoints++;
            }
            while (!pspi.isDone());

            _line.setLineData(xarray, yarray, numPoints);
            _line.drawLine(g, rect);
        }
        catch (RatingException ex)
        {
            Logger.getLogger(SimpleLogArrayGlyph.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
