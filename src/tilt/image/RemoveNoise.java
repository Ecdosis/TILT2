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
import java.awt.Rectangle;
import java.awt.Point;
import java.util.ArrayList;
import java.awt.geom.Area;
import javax.imageio.ImageIO;
import tilt.Utils;
import tilt.handler.post.Options;
import java.io.File;
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
    /** scratch space needed for Blob */
    WritableRaster scratch;
    /** the border region, consisting of the outermost thin border, any blobs 
     * found starting there and any long and large blobs in the inner border */
    Border border;
    /** blobs that were too small but were in the outermost border */
    ArrayList<Blob> rejects;
    Options options;
    boolean despeckleBody;
    int speckleSize;
    int speckleStandoff;
    Rectangle cropRect;
    /**
     * Create a new RemoveNoise object 
     * @param src the source B&W image
     */
    public RemoveNoise( BufferedImage src, Options options, Rectangle cropRect )
    {
        this.src = src;
        this.options = options;
        this.cropRect = cropRect;
        this.speckleStandoff = Math.round(options.getFloat(
            Options.Keys.whiteStandoff)*src.getWidth());
        this.despeckleBody = options.getBoolean(Options.Keys.despeckleBody);
        darkRegions = src.copyData(null);
        scratch = src.copyData(null);
        float average = Blob.setToWhite( darkRegions );
        Blob.setToWhite( scratch );
        border = new Border( getBlurred(), average, cropRect );
        rejectedRegions = Utils.copyRaster( darkRegions, 0, 0 );
        this.rejects = new ArrayList<Blob>();
        speckleSize = Math.round(options.getFloat(Options.Keys.speckleSize)
            *src.getWidth());
        if ( speckleSize == 0 )
            speckleSize = 1;        
    }
    void writeDarkRegions()
    {
        File dst = new File("/tmp/cleaned.png");
        try
        {
            dst.createNewFile();
            BufferedImage bi = new BufferedImage(src.getColorModel(), 
                darkRegions, false,null);
            ImageIO.write( bi, "png", dst );
        }
        catch ( Exception e )
        {
            
        }
    }
    final WritableRaster getBlurred()
    {
        int blurRadius = src.getHeight()/300;
        if ( blurRadius == 0 )
            blurRadius = 5;
        BlurImage bi = new BlurImage( src, blurRadius );
        BufferedImage bImage = bi.blur();
        WritableRaster br = bImage.getRaster();
        int width = br.getWidth();
        int height = br.getHeight();
        int[] iArray = new int[width];
        // blackify
        for ( int y=0;y<height;y++ )
        {
            br.getPixels(0,y,width,1,iArray);
            for ( int x=0;x<width;x++ )
                if ( iArray[x] != 255 )
                    iArray[x] = 0;
            br.setPixels(0,y,width,1,iArray);
        }
        return br;
    }
    /**
     * Clear pixels in the corresponding image that are marked "dark" 
     * @param wr the writable raster to clean
     */
    private void clearPixels( WritableRaster wr )
    {
        int[] iArray = new int[1];
        iArray[0] = 0;
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
            }
        }
    }
    /**
     * Is there a matching black pixel at these coordinates in rejectedRegions?
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
            {
                b.save( darkRegions, wr, b.firstBlackPixel );
            }
        }
    }
    /**
     * Is this blob near the cropRect's margins?
     * @param b the blob to test
     * @return true if it is
     */
    private boolean nearEdge( Blob b )
    {
        int edge = Math.round(cropRect.width/100.0f);
        int maxY = cropRect.y+cropRect.height;
        int minY = cropRect.y;
        int minX = cropRect.x;
        int maxX = cropRect.x+cropRect.width;
        return ( b.topLeft().y<=minY+edge
            || b.botRight().y>=maxY-edge
            || b.topLeft().x<=minX+edge
            || b.botRight().x>=maxX-edge );
    }
    /**
     * Is the blob dense or diffuse?
     * @param b the blob to test
     * @return true if it is compact within its limits
     */
    private boolean isDense( Blob b )
    {
        return (float)b.getBlackPixels()/(float)b.getTotalPixels() 
            > options.getFloat(Options.Keys.minBlackPC);
    }
    /**
     * Is this a small dot anywhere?
     * @param b the blob to test
     * @return true if it meets the criteria for speckles
     */
    private boolean isSpeckle( Blob b, WritableRaster wr )
    {
        return b.getHeight() !=0 && b.getHeight() <= speckleSize 
            && b.getWidth() != 0 && b.getWidth()<= speckleSize
            && b.hasWhiteStandoff(wr,speckleStandoff);
    }
    /**
     * Is this a small dot in the borders?
     * @param b the blob to test
     * @return true if it meets the criteria for speckles
     */
    private boolean isSpeckle( Blob b )
    {
        return b.getHeight() !=0 && b.getHeight() <= speckleSize 
            && b.getWidth() != 0 && b.getWidth()<= speckleSize;
    }
    /**
     * Is the blob very long or wide?
     * @param b the blob to test
     * @return true if it is odd-shaped
     */
    private boolean isOddShaped( Blob b )
    {
        float width = (float)b.getWidth();
        float height = (float)b.getHeight();
        float htWtRatio= (float)height/(float)width;
        float wtHtRatio = (float)width/(float)height;
        if ( wtHtRatio > options.getFloat(Options.Keys.oddShape) )
            return true;
        else if ( htWtRatio > options.getFloat(Options.Keys.oddShape) )
            return true;
        else
            return false;
    }
    /**
     * Is this blob just too damn big? If so. it can't be text.
     * @param b the blob in question
     * @return true if its a monster
     */
    private boolean isTooBig( Blob b )
    {
        int maxWidth = Math.round(src.getWidth()
            *options.getFloat(Options.Keys.maxFeatureSize));
        int maxHeight = Math.round(src.getHeight()
            *options.getFloat(Options.Keys.maxFeatureSize));
        boolean res = b.getHeight() > maxHeight || b.getWidth()>maxWidth;
        return res;
    }
    /**
     * Is a blob a black spot to be removed in the margin?
     * @param b the blob to test
     * @param wr the raster to test it in
     * @return true if this blob is suitable for removal
     */
    public boolean isInvalidInMargin( Blob b, WritableRaster wr )
    {
        if ( nearEdge(b) )
            return true;
        else if ( isDense(b) && isOddShaped(b) )
            return true;
        else if ( isSpeckle(b) )
            return true;
        else if ( isTooBig(b) )
            return true;
        else 
        {
            //System.out.println("w="+b.getWidth()+" h="+b.getHeight()
            //+" x="+b.topLeft().x+" y="+b.topLeft().y);
            if ( b.hasWhiteStandoff(wr,speckleStandoff) )
                return true;
            else
                return false;
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
        int yEnd = cropRect.height+cropRect.y;
        int xEnd = cropRect.width+cropRect.x;
        for ( int y=cropRect.y;y<yEnd;y++ )
        {
            loc.y = y;
            for ( int x=cropRect.x;x<xEnd;x++ )
            {
                loc.x = x;
                if ( border.area.contains(loc) )
                {
                    wr.getPixel(x,y,iArray);
                    if ( iArray[0] == 0 )
                    {
                        if ( !isDirty(loc) && !isRejected(loc) )
                        {
                            Blob b = new Blob(scratch,options,null);
                            b.expandArea( wr, loc );
                            //System.out.println("blob in margin: b.width="+b.getWidth()+" x="+x+" y="+y);
                            if ( isInvalidInMargin(b,wr) )
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
                else 
                {
                    wr.getPixel(x,y,iArray);
                    if ( iArray[0] == 0 )
                    {
                        if ( !isDirty(loc) )
                        {
                            Blob b = new Blob(scratch,options,null);
                            b.expandArea( wr, loc );
                            if ( (despeckleBody && isSpeckle(b,wr))
                                || (isTooBig(b)&&b.hasWhiteStandoff(wr,
                                    speckleStandoff)) )
                                b.save(darkRegions,wr,loc);
                        }
                    }
                }
            }
        }
        //writeDarkRegions();
        calcWhiteArea( wr );
        clearPixels( wr );        
    }
}
