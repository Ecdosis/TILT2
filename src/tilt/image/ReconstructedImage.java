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
 *  (c) copyright Desmond Schmidt 2015
 */

package tilt.image;

import java.awt.Point;
import tilt.handler.post.Options;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.Rectangle;
/**
 * Reconstruct an image by extending the binarised image in line with the 
 * original greyscale when overlaid.
 * @author desmond
 */
public class ReconstructedImage 
{
    BufferedImage tt;
    BufferedImage gi;
    BufferedImage ci;
    BufferedImage mask;
    WritableRaster greyLevel;
    Rectangle cropRect;
    /**
     * Set instance vars with constructor
     * @param ci the cleaned twotone image
     * @param tt the two-tone or binarised image using Sauvola's method
     * @param gi the original greyscale before binarisation
     * @param cropRect the crop rectangle (perhaps the whole image)
     */
    public ReconstructedImage( BufferedImage ci, BufferedImage tt, 
            BufferedImage gi, Rectangle cropRect )
    {
        this.tt = tt;
        this.gi = gi;
        this.ci = ci;
        this.cropRect = cropRect;
    }
    public BufferedImage reconstruct( Options opts )
    {
        WritableRaster wr = ci.copyData(null);
        mask = new BufferedImage(ci.getColorModel(), wr, false,null);
        findBlackBlobs( opts );
        subtractMask();
        return this.ci;
    }
    private void subtractMask()
    {
        int blurRadius = mask.getHeight()/150;
        if ( blurRadius == 0 )
            blurRadius = 5;
        BlurImage bli = new BlurImage( mask, blurRadius );
        BufferedImage blurred = bli.blur();
        WritableRaster wr = ci.getRaster();
        WritableRaster br = blurred.getRaster();
        int width = wr.getWidth();
        int height = wr.getHeight();
        int[] dArray = new int[width];
        int[] iArray = new int[width];
        for ( int y=0;y<height;y++ )
        {
            wr.getPixels(0,y, width, 1, dArray);
            br.getPixels(0,y, width, 1, iArray);
            for ( int x=0;x<width;x++ )
            {
                if ( iArray[x] == 255 && dArray[x] == 0 )
                {
                    dArray[x] = 255;
                }
            }
            wr.setPixels( 0, y, width, 1, dArray );
        }
        ci.setData(wr);
    }
    private void findBlackBlobs( Options opts )
    {
        WritableRaster binarised = ci.getRaster();
        WritableRaster extended = ci.copyData(null);
        Blob.setToWhite(extended);
        int width = binarised.getWidth();
        int height = binarised.getHeight();
        int[] iArray = new int[1];
        this.greyLevel = getAverageGrey();
        WritableRaster gr = gi.getRaster();
        for ( int y=0;y<height;y++ )
        {
            for ( int x=0;x<width;x++ )
            {
                binarised.getPixel(x,y,iArray);
                if ( iArray[0] == 0 )
                {
                    if ( !isDirty(x,y,extended) )
                    {
                        Blob b = new Blob( gr, opts, greyLevel );
                        b.save( extended, gr, new Point(x,y) );
                    }
                }
            }
        }
        // save the result on top of the original binarised image
        ci.setData(extended);
    }
    WritableRaster getAverageGrey()
    {
        int blurRadius = gi.getHeight()/80;
        BlurImage bi = new BlurImage( gi, blurRadius );
        BufferedImage blurred = bi.blur();
        return blurred.getRaster();
    }
    /**
     * Is there a matching black pixel at these coordinates in darkRegions?
     * @param x the point's x-location
     * @param y the point's y-location
     * @param darkRegions the raster with dark bits already added
     * @return true if it is already dark there
     */
    private boolean isDirty( int x, int y, WritableRaster darkRegions )
    {
        int[] iArray = new int[1];
        darkRegions.getPixel(x,y,iArray);
        return iArray[0]==0;
    }
}
