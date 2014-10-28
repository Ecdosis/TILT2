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
import java.awt.Point;
import java.util.ArrayList;
import java.awt.geom.Area;
import tilt.Utils;
import tilt.handler.Options;
/**
 * Remove big black blobs from two-tone pictures. The border
 * @author desmond
 */
public class RemoveNoise 
{
    /** the original image src (B&W) */
    BufferedImage src;
    /** pixel register of accepted blobs that will be removed*/
    WritableRaster darkRegions;
    /** pixel register of rejected blobs */
    WritableRaster rejectedRegions;
    /** the border region, consisting of the outermost thin border, any blobs 
     * found starting there and any long and large blobs in the inner border */
    Border border;
    /** blobs that were too small but were in the outermost border */
    ArrayList<Blob> rejects;
    Options options;
    /**
     * Create a new RemoveNoise object 
     * @param src the source B&W image
     */
    public RemoveNoise( BufferedImage src, Options options )
    {
        this.src = src;
        this.options = options;
        darkRegions = src.copyData(null);
        float average = Blob.setToWhite( darkRegions );
        border = new Border( src.getRaster(), average );
        rejectedRegions = Utils.copyRaster( darkRegions, 0, 0 );
        this.rejects = new ArrayList<Blob>();
    }
    /**
     * Clear pixels in the corresponding image that are marked "dark" 
     * @param wr the writable raster to clean
     */
    private void clearPixels( WritableRaster wr )
    {
        int[] iArray = new int[1];
        iArray[0] = 0;
//        Point p = new Point(0,0);
        for ( int y=0;y<wr.getHeight();y++ )
        {
            for ( int x=0;x<wr.getWidth();x++ )
            {
                wr.getPixel(x,y,iArray);
                if ( iArray[0] == 0 )
                {
                    darkRegions.getPixel(x,y,iArray);
                    if ( iArray[0] == 0 )
                    {
                        iArray[0] = 255;
                        wr.setPixel(x,y,iArray );
                    }
                }
//                p.x = x;
//                p.y = y;
//                if ( border.area.contains(p) )
//                    wr.setPixel(x,y,iArray );
            }
        }
    }
    /**
     * Is there a matching black pixel at these coordinates in darkRegions?
     * @param loc the point to test
     * @return true if it is already dark there
     */
    private boolean isRejected( Point loc )
    {
        int[] iArray =new int[1];
        rejectedRegions.getPixel(loc.x,loc.y,iArray);
        return iArray[0]==0;
    }
    /**
     * Is there a matching black pixel at these coordinates in darkRegions?
     * @param loc the point to test
     * @return true if it is already dark there
     */
    private boolean isDirty( Point loc )
    {
        int[] iArray =new int[1];
        darkRegions.getPixel(loc.x,loc.y,iArray);
        return iArray[0]==0;
    }
    /**
     * Add any formerly rejected blobs that are within the outer border
     * @param wr the raster of the image
     */
    private void calcWhiteArea( WritableRaster wr )
    {
        Area whiteArea = border.area;
        for ( int i=0;i<rejects.size();i++ )
        {
            Blob b = rejects.get(i);
            if ( whiteArea.contains(b.topLeft()) 
                || whiteArea.contains(b.botRight()) )
                b.save( darkRegions, wr, b.firstBlackPixel );
        }
    }
    /**
     * Identify large areas of adjacent black pixels and remove them.
     */
    public void clean()
    {
        WritableRaster wr = src.getRaster();
        Point loc= new Point(0,0);
        int[] iArray = new int[1];
        Point p = new Point(500,500);
        for ( int y=0;y<wr.getHeight();y++ )
        {
            loc.y = y;
            for ( int x=0;x<wr.getWidth();x++ )
            {
                loc.x = x;
                if ( border.area.contains(loc) )
                {
                    wr.getPixel(x,y,iArray);
                    if ( iArray[0] == 0 )
                    {
                        if ( !isDirty(loc) && !isRejected(loc) )
                        {
                            Blob b = new Blob(darkRegions,options);
                            b.expandArea( wr, loc );
                            if ( b.isValid(wr.getWidth(),wr.getHeight())
                                ||b.isOddShaped(wr.getWidth(),wr.getHeight()) )
                            {
                                b.save(darkRegions,wr,loc);
                            }
                            
                            else
                            {
                                b.save(rejectedRegions,wr,loc);
                                rejects.add( b );
                            }
                        }
                    }
                }
            }
        }
        calcWhiteArea( wr );
        clearPixels( wr );
    }
}
