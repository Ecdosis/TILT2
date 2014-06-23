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

package tilt.test.html;

/**
 * Represent a reference to a JQuery external copy
 * @author desmond
 */
public class JQueryRef extends Script
{
    /**
     * Create a reference for the header to an external jquery version
     * @param version the version of jQuery to use
     */
    public JQueryRef( String version )
    {
        super("");
        addAttribute("src","http://code.jquery.com/jquery-"+version+".js");
    }
}
