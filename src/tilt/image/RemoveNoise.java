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
/**
 * Remove big black blobs from two-tone pictures.
 * @author desmond
 */
public class RemoveNoise 
{
    BufferedImage src;
    WritableRaster darkRegions;
    static float BORDER = 0.1f;
    public RemoveNoise( BufferedImage src )
    {
        this.src = src;
        darkRegions = src.copyData(null);
        Blob.setToWhite( darkRegions );
    }
    
    /**
     * Clear pixels in the corresponding image that are marked "dark" 
     * @param wr the writable raster to clean
     */
    private void clearPixels( WritableRaster wr )
    {
        int[] iArray = new int[1];
        Point p = new Point(0,0);
        for ( int y=0;y<wr.getHeight();y++ )
        {
            for ( int x=0;x<wr.getWidth();x++ )
            {
                wr.getPixel(x,y,iArray);
                if ( iArray[0] == 0 )
                {
                    p.x = x;
                    p.y = y;
                    darkRegions.getPixel(x,y,iArray);
                    if ( iArray[0] == 0 )
                    {
                        iArray[0] = 255;
                        wr.setPixel(x,y,iArray );
                    }
                }
            }
        }
    }
    private boolean isDirty( Point loc )
    {
        int[] iArray =new int[1];
        darkRegions.getPixel(loc.x,loc.y,iArray);
        return iArray[0]==0;
    }
    private void mergeIntoDarkness( Blob b )
    {
        WritableRaster other = b.dirt;
        int[] iArray = new int[1];
        for ( int y=0;y<other.getHeight();y++ )
        {
            for ( int x=0;x<other.getWidth();x++ )
            {
                other.getPixel(x,y,iArray);
                if ( iArray[0] == 0 )
                    darkRegions.setPixel(x,y,iArray);
            }
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
        int topYLimit = Math.round(BORDER*wr.getHeight());
        int botYLimit = wr.getHeight()-topYLimit;
        int leftXLimit = Math.round(BORDER*wr.getWidth());
        int rightXLimit = wr.getWidth()-leftXLimit;
        for ( int y=0;y<wr.getHeight();y++ )
        {
            loc.y = y;
            for ( int x=0;x<wr.getWidth();x++ )
            {
                if ( y <= topYLimit || y >= botYLimit 
                    || x <= leftXLimit || x >= rightXLimit )
                {
                    loc.x = x;
                    wr.getPixel(x,y,iArray);
                    if ( iArray[0] == 0 )
                    {
                        if ( !isDirty(loc) )
                        {
                            Blob b = new Blob(darkRegions);
                            b.expandArea( wr, loc );
                            if ( b.isValid(wr.getWidth(),wr.getHeight()) )
                            {
                                b.save(src.copyData(null),wr,loc);
                                mergeIntoDarkness( b );
                                System.out.println("Added blob x="+x+" y="+y
                                    +" width="+b.getWidth()+" height="+b.getHeight());
                            }
                        }
                    }
                }
            }
        }
        clearPixels( wr );
    }
}
