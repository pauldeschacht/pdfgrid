/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pauldeschacht.pdfgrid;

/**
 *
 * @author pauldeschacht
 */
public class Span {
    protected float _f1, _f2;
    public enum POSITION { BEFORE, BEFORE_OVERLAP, ENCLOSED, SPANNED, AFTER_OVERLAP, AFTER };
    public Span() {
        _f1 = Float.MIN_VALUE;
        _f2 = Float.MIN_VALUE;
    }
    public Span(float f1, float f2) {
        _f1 = f1;
        _f2 = f2;
        if (_f1 >= f2) {
            System.out.println("ERROR WRONG SPAN");
        }
    }
    
    float f1() { return _f1; }
    void f1(float f) { _f1 = f; }
    
    float f2() { return _f2; }
    void f2(float f) { _f2 = f; }
    
    float getWidth() { return _f2 - _f1; }
    /*
     */
    POSITION overlap(Span other) {
        if(this._f2 <= other.f1()) {
            return POSITION.BEFORE;
        }
        else if(this._f1 >= other.f2()) {
            return POSITION.AFTER;
        }
        else if(this._f1 <= other.f1()) {
            if(this._f2 > other.f2()) {
                return POSITION.SPANNED;
            }
            else {
                return POSITION.BEFORE_OVERLAP;
            }
        }
        else {
            //this._f1 > other.f1
            if(this._f2 <= other.f2()) {
                return POSITION.ENCLOSED;
            }
            else {
                return POSITION.AFTER_OVERLAP;
            }
        }
    }
    /*
     * requirement: this and other span must overlap
     */
    Span intersect(Span other) {
        return new Span(Math.max(_f1, other.f1()), Math.min(_f2, other.f2()));
    }
}
