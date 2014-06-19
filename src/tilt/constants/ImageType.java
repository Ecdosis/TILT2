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
    original,
    greyscale,
    twotone,
    cleaned;
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
            it= original;
        }
        return it;
    }
}
