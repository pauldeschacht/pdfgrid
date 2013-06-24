package org.pauldeschacht.pdfgrid;

public class Line 
{
    public static double LINE_MARGIN = 2;

    public double _x1, _y1, _x2, _y2;

    public Line(double x1, double y1, double x2, double y2) 
    {
	_x1 = x1;
	_y1 = y1;
	_x2 = x2;
	_y2 = y2;
    }
    public boolean isVertical() 
    {
	return (java.lang.Math.abs(_y2 - _y1) < Line.LINE_MARGIN);
    }
    public boolean isHorizontal()
    {
	return (java.lang.Math.abs(_x2 - _x1) < Line.LINE_MARGIN);
    }

    public String toString() 
    {
	return 
	    "" + 
	    Float.toString((float)_x1) + 
	    "," + 
	    Float.toString((float)_y1) + 
	    "," + 
	    Float.toString((float)_x2) + 
	    "," + 
	    Float.toString((float)_y2);
    }

};
