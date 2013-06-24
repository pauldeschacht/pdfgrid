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

public class WordPositionRightAlignment implements Comparator<WordPosition> {
    private final static float FLOAT_DELTA = 0.00001f;
    
    public int compare(WordPosition wp1, WordPosition wp2) {

        float dx = wp1.x2() - wp2.x2();
        if(Math.abs(dx) < FLOAT_DELTA) {
            //same line
            int dl = wp1.getLineNb() - wp2.getLineNb();
            if(dl == 0) {
                return 0; //should not happen
            }
            if (dl < 0) {
                return -1;
            }
            else {
                return 1;
            }
        }
        else {
            if (dx < 0) {
                return -1;
            }
            else {
                return 1;
            }
        }
    }
}
