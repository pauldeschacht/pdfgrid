/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pauldeschacht.pdfgrid;

import java.util.Comparator;

/**
 *
 * @author pauldeschacht
 */

public class WordPositionComparator implements Comparator<WordPosition> {
    private final static float FLOAT_DELTA = 0.00001f;
    
    public int compare(WordPosition wp1, WordPosition wp2) {

        float dy = wp1.y1() - wp2.y1();
        if(Math.abs(dy) < FLOAT_DELTA) {
            //same line
            float dx = wp1.x1() - wp2.x1();
            if(Math.abs(dx) < FLOAT_DELTA) {
                return 0; //should not happen
            }
            if (dx < 0) {
                return -1;
            }
            else {
                return 1;
            }
        }
        else {
            if (dy < 0) {
                return -1;
            }
            else {
                return 1;
            }
        }
    }
}
