/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pauldeschacht.pdfgrid;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pauldeschacht
 */
public class WordCluster {
    
    public static float HALF_DELTA = 4;

    protected float _start = Float.MAX_VALUE;
    protected float _end = Float.MIN_VALUE;
    
    protected List<WordPosition> _words = new ArrayList<WordPosition>();
    
    public boolean doesBelongToCluster(float coordinate) {
    
        if (coordinate< _start) {
            return false;
        }
        if (coordinate > _end) {
            return false;
        }
        return true;
    }
    
    void addWord(float coordinate, WordPosition word) {
        _words.add(word);
        _start = Math.min(coordinate-HALF_DELTA, _start);
        _end = Math.max(coordinate+HALF_DELTA, _end);
    }
    
    public List<WordPosition> getWords() {
        return _words;
    }
    
    public Span getSpan() {
        return new Span(_start,_end);
    }
    
    public boolean overlap(WordCluster other) {
        Span.POSITION pos = getSpan().overlap(other.getSpan());
        if (pos == Span.POSITION.BEFORE || pos == Span.POSITION.AFTER) {
            return false;
        }
        return true;
    }
    
    public void merge(WordCluster other) {
        _start = Math.min(_start, other.getSpan().f1());
        _end = Math.max(_end, other.getSpan().f2());
        for(WordPosition word: other.getWords()) {
            _words.add(word);
        }
    }
    
    /*
    public WordCluster(WordCluster c) {
        _start = c.getSpan().f1();
        _end = c.getSpan().f2();
        for(WordPosition word: c.getWords()) {
            _words.add(word);
        }
    }
    
    public WordCluster(WordCluster c1, WordCluster c2) {
        _start = Math.min(c1.getSpan().f1(), c2.getSpan().f1());
        _end = Math.max(c1.getSpan().f2(), c2.getSpan().f2());
        for(WordPosition word: c1.getWords()) {
            _words.add(word);
        }
        for(WordPosition word: c2.getWords()) {
            _words.add(word);
        }
    }
    * */
}
