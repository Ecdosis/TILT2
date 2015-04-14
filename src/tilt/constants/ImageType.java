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

package tilt.constants;

/**
 * Types of image requested
 * @author desmond
 */
public enum ImageType 
{
    /** before a picture can be viewed, it must be loaded */
    load,
    /** original image unchanged by tilt */
    original,
    /** original image reduced to 256 shades of grey */
    greyscale,
    /** 8-bit greyscale image reduced to black (0) and white (255) */
    twotone,
    /** same as twotone, which borders and large blobs removed */
    cleaned,
    /** thin pen-strokes lost during twotone conversion restored */
    reconstructed,
    /** cleaned image with lines draw over the top */
    baselines,
    /** reduced lines */
    reduced,
    /** original image with word shapes in alternating colours */
    words,
    /** original image and key to requesting linkage to text */
    link;
    /**
     * Convert a string into
     * @param str a string representation perhaps in upper or mixed case
     * @return the enum version of str
     */
    public static ImageType read( String str )
    {
        ImageType it;
        try
        {
            it = valueOf(str.toLowerCase());
        }
        catch ( Exception e )
        {
            it = load;
        }
        return it;
    }
    public String getMessage()
    {
        switch ( this )
        {
            case load:
                return "loading image";
            case original:
                return "converting to greyscale";
            case greyscale:
                return "converting to twotone";
            case twotone:
                return "cleaning borders";
            case cleaned:
                return "reconstructing fine lines";
            case reconstructed:
                return "identifying baselines";
            case baselines:
                return "removing duplicate baselines";
            case reduced:
                return "finding words";
            case words:
                return "linking word shapes to text";
            case link: default:
                return "done";
        }
    }
}
