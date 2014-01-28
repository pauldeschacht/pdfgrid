package org.pauldeschacht.pdfgrid;

import org.apache.pdfbox.pdmodel.font.PDFont;

public class WordPosition
{
    protected String _word;
    protected float _x1, _y1, _x2, _y2;
    protected int _lineNb;
    protected String _fontName;
    protected float _fontSize;
    protected PDFont _font;
    protected float _spaceWidth;
    
    public int _clusterRef[] = new int[4];
    /*
     * 0 RIGHT ALIGNED
     * 1 LEFT ALIGNED
     * 2 CENTER ALIGNED
     * 3 UNIFIED CLUSTER
     */
    
    public WordPosition() {
	_word = null;
        _lineNb = -1;
        
        for(int i=0;i<4;i++) {
            _clusterRef[i] = -1;
        }
    }
    
    public boolean isNumber() {
        for(int i=0; i<_word.length(); i++) {
            char c = _word.charAt(i);
            if (c < '0' && c > '9' && c != ',' && c != '.' && c != ' ') {
                return false;
            }
        }
        return true;
    }
    
    public void setWord(String word) {
	_word = word;
    }

    public String word() { return _word; }

    public float x1() { return _x1; }
    public float y1() { return _y1; }
    public float x2() { return _x2; }
    public float y2() { return _y2; }

    public void setRectangle(float x1, float y1, float x2, float y2 ) {
	_x1 = x1;
	_y1 = y1;
	_x2 = x2;
	_y2 = y2;  
    }
    
    public void trimSpaces() {
        while(_word.startsWith(" ") == true) {
            _word = _word.substring(1,_word.length());
            _x1 += _spaceWidth;
        }
        while(_word.endsWith(" ")==true) {
            _word = _word.substring(0,_word.length());
            _x2 -= _spaceWidth;
        }
    }
    
    public void setLineNb(int lineNb) {
        _lineNb = lineNb;
    }
    
    public int getLineNb() {
        return _lineNb;
    }

    public float height() { return Math.max(_y1,_y2) - Math.min(_y1,_y2); }
    
    public PDFont getPDFont() { return _font; }
    public void setPDFont(PDFont font) { _font = font; }
    
    public void setSpaceWidth(float spaceWidth) { _spaceWidth = spaceWidth; }
    public float getSpaceWidth() { return _spaceWidth; }
    
    
    public void fontName(String fontName) { _fontName = fontName; }
    public String fontName() { return _fontName; }
    public void fontSize(float fontSize) { _fontSize = fontSize; }
    public float fontSize() { return _fontSize; };
    
    public String toString() {
        return toString("\t");
    }
    public String toString(String sep) 
    {
	return Float.toString(_x1) + sep + 
                Float.toString(_x2) + sep + 
                Float.toString(_y1) + sep + 
                Float.toString(_y2) + sep + 
                _word;
    }
    
    public void merge(WordPosition other) {
        _word = _word + " " + other.word();
        _x1 = Math.min(_x1, other.x1());
        _x2 = Math.max(_x2, other.x2());
    }
}
