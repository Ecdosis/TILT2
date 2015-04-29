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
package tilt.image.geometry;

import java.util.ArrayList;

/**
 * Test data and routines for testing polygon intersection, union
 * @author desmond
 */
public class Test {
    private static int failed;
    private static int[][] lists = {
        {70,40,50,70,10,50,40,10,70,40},
        {40,30,30,40,10,30,10,10,30,10,40,30},
        {10,20,70,20,70,60,10,60,10,20},
        {30,70,30,10,50,10,50,70,30,70},
        {10,40,30,10,50,40,30,70,10,40},
        {30,40,60,10,80,40,60,70,30,40}
    };
    private static int[][] results = {
        {10,30,10,10,30,10,34,18,40,10,70,40,50,70,10,50,21,35,10,30},
        {34,18,40,30,30,40,21,35,34,18},
        {10,20,30,20,30,10,50,10,50,20,70,20,70,60,50,60,50,70,30,70,30,60,10,60,10,20},
        {30,20,50,20,50,60,30,60,30,20},
        {30,10,42,28,60,10,80,40,60,70,42,52,30,70,10,40,30,10},
        {42,28,50,40,42,52,30,40,42,28}
    };
    private static Polygon[] polys;
    private static void initPolys()
    {
        polys = new Polygon[lists.length];
        for ( int j=0;j<lists.length;j++ )
        {
            polys[j] = new Polygon();
            for ( int i=1;i<lists[j].length;i+=2 )
                polys[j].addPoint(lists[j][i-1],lists[j][i]);
            polys[j].toPoints();
        }
    }
    static {
        initPolys();
    }
    private static boolean checkPoly( Polygon pg, int[] result )
    {
        boolean res = true;
        for ( int i=0;i<pg.npoints;i++ )
            if ( result[i*2] != pg.xpoints[i] || result[i*2+1] != pg.ypoints[i] )
            {
                System.out.println("incorrect point: expected ["+result[i*2]
                    +","+result[i*2+1]+"] but found ["+pg.xpoints[i]+","
                    +pg.ypoints[i]+"]");
                    res = false;
                    break;
            }
        return res;
    }
    public static void runTests()
    {
        failed = 0;
        for ( int i=0;i<polys.length;i++ )
        {
            Polygon poly;
            if ( i % 2 == 0 )
                poly = polys[i].getUnion(polys[i+1]);
            else
                poly = polys[i-1].getIntersection(polys[i]);
            printPolygonPoints(poly);
            if ( !checkPoly(poly,results[i]) )
                failed++;
        }
        System.out.println("Failed "+failed+" tests. Passed "
            +(results.length-failed)+" tests.");
    }
    private static void printPolygonPoints( Polygon poly )
    {
        ArrayList<Point> list = poly.toPoints();
        for ( int i=0;i<list.size();i++ )
            System.out.print(list.get(i).toString());
        System.out.println("");
    }
    
}