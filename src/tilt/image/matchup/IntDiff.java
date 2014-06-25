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

package tilt.image.matchup;

/**
 * Compute the optimal alignment between two lists of integers
 * @author desmond
 */
public class IntDiff 
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        if ( args.length==2)
        {
            try
            {
                String[] arg1 = args[0].split(",");
                String[] arg2 = args[1].split(",");
                int[] list1 = new int[arg1.length];
                int[] list2 = new int[arg2.length];
                for ( int i=0;i<arg1.length;i++ )
                    list1[i] = Integer.parseInt(arg1[i]);
                for ( int i=0;i<arg2.length;i++ )
                    list2[i] = Integer.parseInt(arg2[i]);
                Matrix m = new Matrix( list1, list2 );
                System.out.println(m.toString());
                m.traceBack();
            }
            catch ( Exception e )
            {
                e.printStackTrace(System.out);
            }
        }
        else
            System.out.println("usage: java IntDiff <list1> <list2>");
        
    }
    
}
