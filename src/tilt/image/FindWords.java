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
import java.awt.Shape;
import tilt.image.page.*;
import java.util.ArrayList;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
/**
 * Using the lines lists search for words
 * @author desmond
 */
public class FindWords 
{
    BufferedImage src;
    WritableRaster wr;
    WritableRaster dirty;
    static float WORDSQUARE_RATIO = 0.032f;
    Rectangle wordSquare;
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
        dirty = src.copyData(null);
        int[] data = new int[dirty.getWidth()];
        for ( int i=0;i<data.length;i++ )
            data[i] = 255;
        for ( int i=0;i<dirty.getHeight();i++ )
            dirty.setPixels( 0, i, data.length, 1, data );
        average = ppAverage;
        page.finalise(wr );
        wordSquare = new Rectangle(0,0,page.getWordGap(),page.getLineHeight()/2);
        this.src = src;
        int shapesFound = 0;
        ArrayList<Line> lines = page.getLines();
        for ( int i=0;i<lines.size();i++ )
        {
            Line line = lines.get(i);
            Point[] points = line.getPoints();
            for ( int j=0;j<points.length;j++ )
            {
                Shape s = findWord( points[j].x, points[j].y, true );
                Line inLine = page.contains(s);
                if ( s != null )
                {
                    if ( inLine == null )
                    {
                        line.add( s );
                        page.addShape( s, line );
                    }
                    else if ( inLine != line )
                        line.addShared( inLine, s );
                    // temporary
                    shapesFound++;
                    break;
                }
            }
            if ( shapesFound > 4 )
                break;
        }
        page.mergeLines();
    }
    /**
     * Find a word
     * @param x the x-coordinate of the start in true dimensions
     * @param y the y-coordinate of the start point in true coordinates
     * @param polygon true if a polygon is desired, else a Rectangle 
     * @return a shape (rectangle or polygon) or null if not found
     */
    private Shape findWord( int x,int y, boolean polygon )
    {
        Area a = new Area();
        float mean =  averageArea( x, y, wordSquare.width, wordSquare.height );
        if ( mean > average )
        {
            Rectangle r = new Rectangle(x,y,wordSquare.width, 
                wordSquare.height);
            a.add( new Area(r) );
            extendAreaUp( a, x, y-wordSquare.height, wordSquare.width );
            extendAreaDown( a, x, y+1, wordSquare.width );
            Shape shape = areaToPolygon( a ); 
            if ( !polygon )
                return shape.getBounds();
            else
                return shape;
        }
        return null;
    }
    /**
     * Average the pixel intensity in a rectangle
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param xsize the width
     * @param ysize the height
     * @return the average number of black pixels per pixel
     */
    private float averageArea( int x, int y, int xsize, int ysize )
    {
        int N = xsize*ysize;
        int total = 0;
        int[] dArray = new int[N];
        wr.getPixels( x, y, xsize, ysize, dArray );
        for ( int i=0;i<N;i++ )
        {
            if ( dArray[i] == 0 )
                total++;
        }
        return (float)total/(float)N;
    }
    /**
     * Extend any pixels in the lowermost row upwards
     * @param a the area to extend
     * @param x the x-coordinate of the left
     * @param y the y coordinate of the top
     * @param xsize the width of the region
     */
    void extendAreaUp( Area a, int x, int y,  int xsize )
    {
        int[] iArray = new int[1];
        int hend = x+xsize;
        for ( int h=x;h<hend;h++ )
        {
            wr.getPixel(h,y,iArray);
            if ( iArray[0] == 0 )
            {
                dirty.getPixel(h,y,iArray);
                if ( iArray[0] != 0 )
                {
                    Blob b = new Blob( dirty );
                    b.save( dirty, wr, new Point(h,y));
                    Rectangle r = new Rectangle(b.topLeft());
                    r.width = b.getWidth();
                    r.height = b.getHeight();
                    a.add( new Area(r) );
                }
            }
        }
    }
    /**
     * Extend any pixels in the topmost row downwards
     * @param a the area to extend
     * @param x the x-coordinate of the left
     * @param y the y coordinate of the top
     * @param xsize the width of the region
     */
    void extendAreaDown( Area a, int x, int y,  int xsize )
    {
        int[] iArray = new int[1];
        int hend = x+xsize;
        for ( int h=x;h<hend;h++ )
        {
            wr.getPixel(h,y,iArray);
            if ( iArray[0] == 0 )
            {
                dirty.getPixel(h,y,iArray);
                if ( iArray[0] != 0 )
                {
                    Blob b = new Blob( dirty );
                    b.save( dirty, wr, new Point(h,y));
                    Rectangle r = new Rectangle(b.topLeft());
                    r.width = b.getWidth();
                    r.height = b.getHeight();
                    a.add( new Area(r) );
                }
            }
        }
    }       
    /**
	 * Use convex hull algorithm to compute the outer reaches of Area
	 * @param a the input area
	 * @return a polygon 
	 */
	Polygon areaToPolygon( Area a )
	{
		// convert to ArrayList<Point>
		ArrayList<Point> points = new ArrayList<Point>();
		PathIterator iter = a.getPathIterator(null);
		float[] coords = new float[6];
		while ( !iter.isDone() )
		{
			int type = iter.currentSegment( coords );
			switch ( type )
			{
				case PathIterator.SEG_MOVETO: case PathIterator.SEG_LINETO:
					points.add( new Point(Math.round(coords[0]), 
						Math.round(coords[1])) );
				default:
					break;
			}
			iter.next();
		}
		ArrayList<Point> hull = FastConvexHull.execute( points );
		Polygon poly = new Polygon();
		if ( hull != null )
		{
			for ( int i=0;i<hull.size();i++ )
			{
				Point p = hull.get( i );
				poly.addPoint( p.x, p.y );
                if ( i==hull.size()-1 )
                {
                    // join back to start
                    Point q = hull.get(0);
                    poly.addPoint( q.x,q.y );
                }
			}
		}
		return poly;
	}
}
