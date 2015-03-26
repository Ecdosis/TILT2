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
import tilt.image.page.Polygon;
import java.awt.Rectangle;
import tilt.handler.post.Options;
/**
 * Using the lines lists search for words
 * @author desmond
 */
public class FindWords 
{
    BufferedImage src;
    Page page;
    WritableRaster wr;
    WritableRaster dirty;
    Rectangle wordSquare;
    Options opts;
    /**
     * Create a word-finder
     * @param src the src image in pure black and white
     * @param page the page object with postulated lines
     * @param opts the options from the GeoJSON document
     */
    public FindWords( BufferedImage src, Page page, Options opts ) 
    {
        wr = src.getRaster();
        dirty = src.copyData(null);
        Blob.setToWhite(dirty);
        this.page = page;
        this.src = src;
        this.opts = opts;
        ArrayList<Line> lines = page.getLines();
        int nShapes = 0;
        for ( int i=0;i<lines.size();i++ )
        {
            Line line = lines.get(i);
            Point[] points = line.getPoints();
            for ( int j=0;j<points.length-1;j++ )
            {
                Polygon[] s = findPartWord( points[j], points[j+1] );
                if ( s != null )
                {
                    for ( int k=0;k<s.length;k++ )
                    {
                        line.add( s[k] );
                        page.addShape( s[k], line );
                        nShapes++;
                    }
                }
            }
            Rectangle bounds = line.getBounds();
            // clear the relevant portion of dirty so we can recognise the 
            // same shapes on subsequent lines and later merge them
            // onto the correct line
            Blob.setToWhite( dirty, bounds );
        }
        System.out.println("nShapes="+nShapes);
        page.mergeLines();
        page.pruneShortLines();
        page.getWordGap(); 
        page.joinWords();
    }
    /**
     * Find part of a word. All blobs near the line will be added.
     * @param p1 the start of the baseline segment
     * @param p2 the end of this baseline segment
     * @return a polygon or null if not found
     */
    private Polygon[] findPartWord( Point p1, Point p2 )
    {
        int midY = (p1.y+p2.y)/2;
        int rHt = 2*page.getLineHeight()/3;
        int top = midY-rHt/2;
        if ( top < 0 )
            top = 0;
        // compute rectangle in which to look for blobs
        Rectangle r = new Rectangle( p1.x, top, p2.x-p1.x, rHt );
        int[] iArray = new int[1];
        int endY = r.y+r.height;
        int endX = r.x+r.width;
        ArrayList<Blob> blobs = new ArrayList<>();
        for ( int y=r.y;y<endY;y++ )
        {
            for ( int x=r.x;x<endX;x++ )
            {
                wr.getPixel( x, y, iArray );
                if ( iArray[0] == 0 )
                {
                    dirty.getPixel( x, y, iArray );
                    if ( iArray[0]!= 0 )
                    {
                        Blob b = new Blob( dirty, opts );
                        b.save( dirty, wr, new Point(x,y) );
                        blobs.add( b );
                    }
                }
            }
        }
        // compose an array of polygons out of the blobs
        if ( blobs.size()> 0 )
        {
            ArrayList<Polygon> shapes = new ArrayList<>();
            for ( int i=0;i<blobs.size();i++ )
            {
                Blob b = blobs.get(i);
                Polygon pg = b.toPolygon();
                shapes.add( pg );
                Rectangle bounds = pg.getBounds();
                //if ( bounds.height > 5*rHt )
                //    System.out.println("Height="+bounds.height);
            }
            // convert to array
            Polygon[] ss = new Polygon[shapes.size()];
            shapes.toArray(ss);
            return ss;
        }
        else
            return null;
    }
}
