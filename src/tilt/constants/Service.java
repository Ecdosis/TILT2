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
 * Names of services used in urls
 * @author desmond
 */
public class Service 
{
    /** test java interface */
    public static final String TEST = "test";
    /** high-level tilt service name */
    public static final String TILT = "tilt";
    /** sub-service within tilt returns gGeoJson representations of an image+text */
    public static final String GEOJSON = "geojson";
    /** sub-service that returns a particular type of image */
    public static final String IMAGE = "image";
    /** the tilt gui editor */
    public static final String EDITOR = "editor";
}
