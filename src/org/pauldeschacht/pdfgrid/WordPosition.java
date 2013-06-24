package org.pauldeschacht.pdfgrid;

public class WordPosition
{
    protected String _word;
    protected float _x1, _y1, _x2, _y2;
    protected int _lineNb;
    protected String _fontName;
    protected float _fontSize;
    
    public WordPosition() {
	_word = null;
        _lineNb = -1;
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
    
    public void setLineNb(int lineNb) {
        _lineNb = lineNb;
    }
    
    public int getLineNb() {
        return _lineNb;
    }

    public float height() { return Math.max(_y1,_y2) - Math.min(_y1,_y2); }
    
    
    public void fontName(String fontName) { _fontName = fontName; }
    public String fontName() { return _fontName; }
    public void fontSize(float fontSize) { _fontSize = fontSize; }
    public float fontSize() { return _fontSize; };
    
    public String toString() 
    {
	return Float.toString(_x1) + "\t" + 
                Float.toString(_y1) + "\t" + 
                Float.toString(_x2) + "\t" + 
                Float.toString(_y2) + "\t" + 
                _word;
    }
    
}
