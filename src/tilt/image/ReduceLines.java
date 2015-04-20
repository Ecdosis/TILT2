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
import tilt.image.page.Page;
import java.util.ArrayList;
import tilt.image.page.Line;
import java.awt.Point;
import tilt.handler.post.Options;

/**
 * Remove lines that intersect with blobs that already have a longer line
 * @author desmond
 */
public class ReduceLines 
{
    Page page;
    BufferedImage src;
    WritableRaster wr;
    WritableRaster dirty;
    Options options;
    /** standoff for outlying blobs on lines */
    int hStandoff;
    int vStandoff;
    /**
     * 1. Sort lines by decreasing length.
     * 2. Identify blobs attached to each line
     * 3. If all blobs found are already present, drop that line
     * @param src the blurred source image
     * @param page the page with the lines
     * @param options the options from the GeoJSON
     */
    public ReduceLines( BufferedImage src, Page page, Options options )
    {
        this.page = page;
        this.src = src;
        this.options = options;
        this.hStandoff = Math.round(
            options.getFloat(Options.Keys.outlierStandoff)
            *src.getWidth());
        this.vStandoff = Math.round(
            options.getFloat(Options.Keys.whiteStandoff)
            *src.getWidth());
        WritableRaster wr = src.getRaster();
        ArrayList<Line>lines = page.getLines();
        // sortLines(lines);
        dirty = src.copyData(null);
        Blob.setToWhite(dirty);
        ArrayList<Line> removals = new ArrayList<>();
        int[] iArray = new int[1];
        int[] dArray = new int[1];
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            Point[] points = l.getPoints();
            if ( points.length> 1 )
            {
                ArrayList<Point> delPoints = new ArrayList<>();
                for ( int j=1;j<points.length;j++ )
                {
                    Point last = points[j-1];
                    Point curr = points[j];
                    float dy_dx = (float)(curr.y-last.y)/(float)(curr.x-last.x);
                    boolean hasNewBlobs = false;
                    for ( int x=last.x;x<curr.x;x++ )
                    {
                        int y = last.y + Math.round((x-last.x)*dy_dx);
                        dirty.getPixel(x,y,iArray);
                        if ( iArray[0] != 0 )
                        {
                            wr.getPixel(x,y,dArray);
                            if ( dArray[0] == 0 )
                            {
                                Blob b = new Blob( wr, options, null );
                                b.save( dirty, wr, new Point(x,y) );
                                hasNewBlobs = true;
                            }
                        }
                        else
                        {
                            hasNewBlobs = true;
                        }
                    }
                    if ( !hasNewBlobs )
                    {
                        delPoints.add( last );
                        if ( j==points.length-1 )
                        {
                            delPoints.add( curr );
                        }
                    }
                }
                if ( delPoints.size()==points.length )
                    removals.add( l );
                else if ( delPoints.size()>0 )
                {
                    l.removePoints(delPoints);
                }
            }
            else
                removals.add(l);
        }
        for ( int i=0;i<removals.size();i++ )
        {
            lines.remove(removals.get(i));
        }
        trimOutliers( lines );
        page.sortLines();
        page.joinLines();
        page.draw( src.getGraphics() ); 
    }
    /**
     * Sort all lines based on their decreasing size 
     */
    private void sortLines( ArrayList<Line> lines )
    {
        int i, j, k, h; 
        Line v;
        int[] incs = { 1391376, 463792, 198768, 86961, 33936,
            13776, 4592, 1968, 861, 336, 
            112, 48, 21, 7, 3, 1 };
        for ( k = 0; k < 16; k++)
        {
            for ( h=incs[k],i=h;i<lines.size();i++ )
            { 
                v = lines.get(i); 
                j = i;
                while (j >= h && lines.get(j-h).countPoints()<v.countPoints() )
                { 
                    lines.set(j,lines.get(j-h)); 
                    j -= h; 
                }
                lines.set(j,v);
            }
        }
    }
    /**
     * Look for a blob isolated at the end and near the cropRet margin
     * @param l the line to trim
     */
    private void trimRightEnd( Line l )
    {
        int np = l.countPoints();
        ArrayList<Point> delPoints = new ArrayList<>();
        Point[] points = l.getPoints();
        int[] iArray = new int[1];
        int[] dArray = new int[1];
        WritableRaster wr = src.getRaster();
        for ( int i=np-1;i>np-3&&i>0;i-- )
        {
            Point last = points[i];
            Point curr = points[i-1];
            float dy_dx = (float)(last.y-curr.y)/(float)(last.x-curr.x);
            for ( int x=last.x;x>curr.x;x-- )
            {
                int y = last.y + Math.round((x-last.x)*dy_dx);
                wr.getPixel(x,y,iArray);
                if ( iArray[0] == 0 )
                {
                    dirty.getPixel(x,y,dArray);
                    if ( dArray[0] != 0 )
                    {
                        Blob b = new Blob( wr, options, null );
                        b.save( dirty, wr, new Point(x,y) );
                        // is b isolated by lots of white space?
                        if ( b.hasWhiteStandoff(wr, hStandoff,vStandoff) )
                            delPoints.add(curr);
                    }
                }
            }       
        }
        if ( delPoints.size()>0 )
            l.removePoints(delPoints);
    }
    /**
     * Trim outlying blobs from left of line
     * @param l the line to trim
     */
    private void trimLeftEnd( Line l )
    {  
        int np = l.countPoints();
        ArrayList<Point> delPoints = new ArrayList<>();
        Point[] points = l.getPoints();
        int[] iArray = new int[1];
        int[] dArray = new int[1];
        WritableRaster wr = src.getRaster();
        for ( int i=0;i<3&&i<np-1;i++ )
        {
            Point last = points[i];
            Point curr = points[i+1];
            float dy_dx = (float)(curr.y-last.y)/(float)(curr.x-last.x);
            for ( int x=last.x;x<curr.x;x++ )
            {
                int y = last.y + Math.round((x-last.x)*dy_dx);
                wr.getPixel(x,y,iArray);
                if ( iArray[0] == 0 )
                {
                    dirty.getPixel(x,y,dArray);
                    if ( dArray[0] != 0 )
                    {
                        Blob b = new Blob( wr, options, null );
                        b.save( dirty, wr, new Point(x,y) );
                        // is b isolated by lots of white space?
                        if ( b.hasWhiteStandoff(wr, hStandoff,vStandoff) )
                            delPoints.add(curr);
                    }
                }
            }       
        }
        if ( delPoints.size()>0 )
            l.removePoints(delPoints);
    }
    /**
     * Trim components on the extreme ends near the border
     * @param lines the array of lines to check for outliers
     */
    private void trimOutliers( ArrayList<Line> lines )
    {
        dirty = src.copyData(null);
        Blob.setToWhite(dirty);
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            trimRightEnd(l);
            trimLeftEnd(l);
        }
    }
}
