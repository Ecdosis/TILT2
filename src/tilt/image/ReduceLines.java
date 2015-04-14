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
        Point pt =new Point();
        this.page = page;
        this.src = src;
        WritableRaster wr = src.getRaster();
        ArrayList<Line>lines = page.getLines();
        sortLines(lines);
        dirty = src.copyData(null);
        Blob.setToWhite(dirty);
        ArrayList<Line> removals = new ArrayList<>();
        int[] iArray = new int[1];
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
                            Blob b = new Blob( wr, options, null );
                            b.save( dirty, wr, new Point(x,y) );
                            dirty.getPixel(x,y,iArray);
                            if ( iArray[0]== 0 )
                                hasNewBlobs = true;
                        }
                    }
                    if ( !hasNewBlobs )
                    {
                        delPoints.add( last );
                        if ( j==points.length-1 )
                        {
                            delPoints.add( curr );
                            pt.x = curr.x;
                            pt.y = curr.y;
                        }
                    }
                }
                if ( delPoints.size()==points.length )
                    removals.add( l );
                else if ( removals.size()>0 )
                {
                    l.removePoints(delPoints);
                    System.out.println("removed "+removals.size()+" points");
                }
            }
            else
                removals.add(l);
        }
        for ( int i=0;i<removals.size();i++ )
            lines.remove(removals.get(i));
        System.out.println("removed "+removals.size()+" lines");
        page.sortLines();
        page.joinLines();
        page.draw( src.getGraphics() ); 
    }
    /**
     * I see a grey pixel and I want it painted black ... black.
     * No colours any more I want them to turn black...
     * @param img a blurred image with grey pixels
     */
    private void blackify( BufferedImage img )
    {
        WritableRaster wr= img.getRaster();
        int wd = img.getWidth();
        int ht = img.getHeight();
        int[] iArray= new int[1];
        for ( int y=0;y<ht;y++ )
        {
            for ( int x=0;x<wd;x++ )
            {
                wr.getPixel(x,y,iArray);
                if ( iArray[0] < 255 && iArray[0] != 0 )
                {
                    iArray[0] = 0;
                    wr.setPixel(x,y,iArray );
                }
            }
        }
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
}
