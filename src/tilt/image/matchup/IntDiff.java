/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
