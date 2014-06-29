/*
 * This file is part of TILT.
 *
 *  TILT is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  TILT is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TILT.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2014
 */

package tilt.image;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import tilt.image.page.*;
import java.util.ArrayList;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
/**
 * Using the lines lists search for words
 * @author desmond
 */
public class FindWords 
{
    BufferedImage src;
    WritableRaster wr;
    static float WORDSQUARE_RATIO = 0.032f;
    int wordSquare;
    float average;
    /**
     * Create a word-finder
     * @param src the src image in pure black and white
     * @param page the page object with postulated lines
     * @param ppAverage the per pixel average blackness for the whole image
     */
    public FindWords( BufferedImage src, Page page, float ppAverage ) 
    {
        wr = src.getRaster();
        average = ppAverage;
        wordSquare = Math.round(WORDSQUARE_RATIO*wr.getWidth());
        this.src = src;
        ArrayList<Line> lines = page.getLines();
        for ( int i=0;i<lines.size();i++ )
        {
            Point[] points = lines.get(i).getPoints();
            for ( int j=0;j<points.length;j++ )
            {
                Area a = new Area();
                propagateRect( a, points[j].x, points[j].y, 
                    wordSquare, wordSquare );
            }
        }
    }
    /**
	 * Try to expand an area by testing to the top, left, bottom and right 
	 * of where we are
	 * @param a the area to add shapes to
	 * @param x the left coordinate of the top-left of the square
	 * @param y the top coordinate of the top-left of the square 
	 * @param xsize the width of the square in pixels
	 * @param ysize the height of the square in pixels
	 */
	private void propagateRect( Area a, int x, int y, int xsize, int ysize )
	{
		// accept current rectangle
		Rectangle r = new Rectangle( x, y, xsize, ysize );
		a.add( new Area(r) );
		// above
		int yValue = Math.max(y-wordSquare,0);
		int yHeight = Math.min(wordSquare,y);
		if ( yHeight > 0 )
			augmentArea( a, x, yValue, wordSquare, yHeight );
		// below
		yValue = y+ysize;
		yHeight = Math.min(wordSquare,wr.getHeight()-yValue);
		if ( yHeight > 0 )
			augmentArea( a, x, yValue, xsize, yHeight );
		// left
		int xValue = Math.max(x-wordSquare,0);
		int xWidth = x-xValue;
		if ( xWidth > 0 )
			augmentArea( a, xValue, y, xWidth, ysize );
		// right
		xValue = x+xsize;
		xWidth = Math.min(wordSquare,wr.getWidth()-xValue);
		if ( xWidth > 0 )
			augmentArea( a, xValue, y, xWidth, ysize );
	}
	/**
	 * Grow an area recursively, working with the wr raster
	 * @param a the are to add shapes to
	 * @param x the left coordinate of the top-left of the square
	 * @param y the top coordinate of the top-left of the square 
	 * @param xsize the width of the square in pixels
	 * @param ysize the height of the square in pixels
	 */
	void augmentArea( Area a, int x, int y, int xsize, int ysize  )
	{
		Rectangle r = new Rectangle( x, y, xsize, ysize );
		if ( !a.contains(r) )
		{
			int[] dArray = new int[xsize*ysize];
			wr.getPixels( x, y, xsize, ysize, dArray );
			int total = 0;
			for ( int i=0;i<dArray.length;i++ )
				if ( dArray[i]==0 )
					total++;
			float areaAverage = (float)total/(float)dArray.length;
			if ( areaAverage > average )
			{
				propagateRect(a, x, y, xsize, ysize );
			}
			else if ( xsize==wordSquare && ysize==wordSquare )
			{
				// try subdivision
				if ( testSmallArray(x,y,wordSquare/2,wordSquare/2) )
					propagateRect(a,x,y,xsize,ysize);
				else if ( testSmallArray(x+wordSquare/2,y,wordSquare/2,
					wordSquare/2) )
					propagateRect(a,x,y,xsize,ysize);
				else if ( testSmallArray(x,y+wordSquare/2,wordSquare/2,
					wordSquare/2) )
					propagateRect(a,x,y,xsize,ysize);
				else if ( testSmallArray(x+wordSquare/2,y+wordSquare/2,
					wordSquare/2, wordSquare/2) )
					propagateRect(a,x,y,xsize,ysize);					
			}
		}
	}
	/**
	 * Retest one quarter of a failed big square
	 * @param x the x-coordinate of the sub-square
	 * @param y the y-coordinate of the sub-square
	 * @param xsize the x-size
	 * @param ysize its y-size
	 * @return true if it made the cut
	 */
	private boolean testSmallArray( int x, int y, int xsize, int ysize )
	{
		int[] smallArray = new int[xsize*ysize];
		int total = 0;
		wr.getPixels( x, y, xsize, ysize, smallArray );
		for ( int j=0;j<smallArray.length;j++ )
			if ( smallArray[j] == 0 )
				total++;
		float smallAverage = (float)total/(float)smallArray.length;
		return smallAverage > average;
	}
    /**
     * Print the word shapes over the top of the original image
     * @param original the original raster to write to
     */
	public void print( WritableRaster original )
    {
    }
}
