/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tilt.align;

/**
 * Represent a position in the edit graph
 */
 class Pos
 {
    /** the cost expended to get here */
    int score;
    /** from whence we came */
    Pos parent;
    /** the x-position (index into B) */
    int x;
    /** the y-position (index into A) */
    int y;
    /**
     * Manual constructor
     * @param x the Pos's x-value
     * @param y the Pos's y-value
     * @param score its score
     * @param parent its parent
     */
    Pos( int x, int y, int score, Pos parent )
    {
        this.x = x;
        this.y = y;
        this.parent = parent;
        this.score = score;
    }
    Pos()
    {
    }
}