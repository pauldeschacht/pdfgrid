/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pauldeschacht.pdfgrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


/**
 *
 * @author pauldeschacht
 */
public class WordCluster {

    private Map<Integer,Integer> _lineNbToAlignedLines = new HashMap<Integer,Integer>();

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
        HALF_DELTA = Math.min(HALF_DELTA, word.getSpaceWidth() / 2);
        HALF_DELTA = word.getSpaceWidth();
        //System.out.println(HALF_DELTA + " vs" + word.getSpaceWidth() * 2);
        if (HALF_DELTA < 0.1) {
            HALF_DELTA = 4;
        }
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
    
    public int getAlignedLines(int lineNb) {
        int result = -1;
        Integer i = _lineNbToAlignedLines.get(lineNb);
        if (i!=null) {
            return i.intValue();
        }
        return result;
    }
    public void calcAlignedLines() {
        //sort the words according lineNb
       Collections.sort(_words, new Comparator<WordPosition>() {
            @Override
            public int compare(WordPosition word1, WordPosition word2) {
                int lineNb1 = word1.getLineNb();
                int lineNb2 = word2.getLineNb();
                if (lineNb1 < lineNb2) {
                    return -1;
                } else if (lineNb1 > lineNb2) {
                    return 1;
                }
                return 0;
            }
        });
        
        int count = 0;
        int currLineNb = -1;
        int prevLineNb = 0;
        for(WordPosition word: getWords()) {
            currLineNb = word.getLineNb();
            if(currLineNb != prevLineNb) {
                if(currLineNb == prevLineNb + 1) {
                    count++;
                    prevLineNb = currLineNb;
                }
                else {
                    for(int i=prevLineNb-count+1; i<=prevLineNb; i++) {
                        _lineNbToAlignedLines.put(prevLineNb,count);
                    }
                    count = 1;
                }
            }
            else {
                //_lineToSequence.put(prevLineNb,count);
            }
        }
        if(prevLineNb-count+1 > 0) {
            for(int i=prevLineNb-count+1; i<=prevLineNb; i++) {
                _lineNbToAlignedLines.put(prevLineNb,count);
            }   
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
