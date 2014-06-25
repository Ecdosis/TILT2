/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt.image.matchup;

/**
 * Represent a path component. If the type changes e.g. from deleted
 * to inserted, we start a new path and join it to the previous one.
 * Author: desmond
 * 
 * Created on 9 March 2011, 7:43 AM
 * Modified for Java and integer arrays 24/6/2014
 */
public class Path 
{
    Path prev;
    Path next;
    Operation kind;
    int len;
    int index;
    int x;
    /**
     * Construct a path component whenever we make a move that is different
     * from the previous move. Otherwise we will extend the existing move.
     * @param index the path's original index
     * @param x the path's original x-position
     * @param operation the edit operation that got us here
     * @param len the length of the operation
     */
    public Path( int index, int x, Operation kind, int len )
    {
        this.index = index;
        this.x = x;
        this.kind = kind;
        this.len = len;
        this.prev = null;
    }
    /**
     * Print the path in human-readable form and string together adjacent
     * insertions and deletions.
     */
    void print()
    {
        Path p = this;
        Path start = null;
        while ( p != null )
        {
            Path oldp = p;
            p = p.prev;
            if ( p != null )
                p.next = oldp;
            else
                start = oldp;
        }
        // OK, so we've reversed the path
        p = start;
        while ( p != null )
        {
            switch ( p.kind )
            {
                case nothing:
                    break;
                case insertion:
                    System.out.println("inserted "+p.len+" chars at x="
                        +p.x+" y="+(p.x-index));
                    break;
                case deletion:
                    System.out.println("deleted "+p.len+" chars at x="
                        +p.x+" y="+(p.x-index));
                    break;
                case matched:
                    System.out.println("matched "+p.len+" chars at x="
                        +p.x+" y="+(p.x-index));
                    break;
                case exchange:
                    System.out.println("exchanged "+p.len+" chars at x="
                        +p.x+" y="+(p.x-index));
                    break;
            }
            p = p.next;
        }
    }    
}
