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
import tilt.image.filter.GaussianFilter;


/**
 * Blur a two-tone image so that lines can be recognised more reliably
 * @author desmond
 */
public class BlurImage 
{
    int blur;
    BufferedImage src;
    /**
     * Create a new Blur object 
     * @param src the source B&W image
     */
    public BlurImage( BufferedImage src, int blur )
    {
        this.src = src;
        this.blur = blur;
    }
    /**
     * Deploy a Gaussian blur
     * @return the blurred image
     */
    public BufferedImage blur()
    {
        WritableRaster wr = src.copyData(null);
        BufferedImage dst;
        if ( blur > 0 )
        {
            GaussianFilter gf = new GaussianFilter(blur);
            dst = new BufferedImage(src.getColorModel(), wr, false,null);
            gf.filter( src, dst );
        }
        else
            dst = src;
        return dst;
    }
}
