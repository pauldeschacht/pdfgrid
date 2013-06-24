/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pauldeschacht.pdfgrid;

/**
 *
 * @author pauldeschacht
 */
public class Range {
    protected int _start;
    protected int _num;
    public Range() {
        _start = -1;
        _num = -1;
    }
    
    public Range(int s, int n) {
        _start = s;
        _num = n;
    }
    
    public int start() { return _start; }
    public int num()   { return _num; }
}
