package org.pauldeschacht.pdfgrid;

import java.awt.Point;
import java.util.List;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Grid 
{
    private static final Log LOG = LogFactory.getLog(Grid.class);

    protected double _minx, _miny, _maxx, _maxy;
    protected boolean _isDefined = false;
    protected Double[] _xs = null;
    protected Double[] _ys = null;
    
    public static double COLLAPSE_X = (double)3.0;
    public static double COLLAPSE_Y = (double)3.0;

    public Grid()
    {
	_isDefined=false;
	_xs = null;
	_ys = null;
    }

    /**
     * list of sorted x values that are part of the grid (vertical lines)
     **/    
    public Double[] xs() {
	return _xs;
    }
    /**
     * list of sorted y values that are part of the grid (horizontal lines)
     **/
    public Double[] ys() {
	return _ys;
    }

    /**
     * The input array is a sorted list of values. 
     * The function removes consecutive values that are close to each other (difference less than epsilon)
     * PDF tends to generate a lot of lines (sometimes to blacken borders), that do no contribute to the grid
     **/
    protected Double[] collapse(Double[] d, double epsilon) {
	ArrayList<Double> result = new ArrayList<Double>();
	for(int i=0; i<d.length-1; i++) {
	    double d1 = d[i].doubleValue();
	    result.add(new Double(d1));
	    int j=i+1;
	    for(; j<d.length;j++) {
		double d2 = d[j].doubleValue();
		if(java.lang.Math.abs(d1-d2) > epsilon) {
		    break;
		}
	    }
	    i=j-1;
	}
	return result.toArray(new Double[0]);
    }

    /**
     * The input is a list of WordPositions. Based on the position of the word, this function determines the row and column in the grid. 
     * The resulting matrix is [row,column] based. Each cell in the matrix contains the string that is the concatenation of words that are inside that cell.
     **/
    public String[][] csv(List<WordPosition> words) {

	String[][] csv = new String[_ys.length][_xs.length];

	for(int row=0; row<_ys.length; row++) {
	    for(int col=0; col<_xs.length; col++) {
		csv[row][col] = "";
	    }
	}
	for(WordPosition word: words) {
	    Point index = findGridIndex(word);
	    if(index!=null) {
		int row = (int)index.getY();
		int col = (int)index.getX();
		if (row>=0 && row<_ys.length && col>=0 && col<_xs.length) {
		    csv[row][col] = csv[row][col] + word.word() + " ";
		}
	    }
	    else {
		//		LOG.warn("Word " + word.word() + " is not inside the grid");
	    }
	}
	return csv;
    }

    public String[][] csv2(List<WordPosition> words) {

	String[][] csv = new String[_xs.length][_ys.length];

	for(int row=0; row<_xs.length; row++) {
	    for(int col=0; col<_ys.length; col++) {
		csv[row][col] = "";
	    }
	}
	for(WordPosition word: words) {
	    Point index = findGridIndex(word);
	    if(index!=null) {
		int row = (int)index.getX();
		int col = (int)index.getY();
		if (row>=0 && row<_xs.length && col>=0 && col<_ys.length) {
		    csv[row][col] = csv[row][col] + word.word()+ " ";
		}
	    }
	    else {
		//		LOG.warn("Word " + word.word() + " is not inside the grid");
	    }
	}
	return csv;
    }
    /**
     * Naive algo that searches the cell in which the word appears.
     * The result is a Point that contains the column and the row of the cell
     * TODO: use bounding box of the grid to quickly eliminate 
     **/
    public Point findGridIndex(WordPosition word) {
	float wx1 = word.x1();
	float wy1 = word.y1();
	float wx2 = word.x2();
	float wy2 = word.y2();
	
	int i;
	boolean found = false;
	for(i=0; i<_xs.length-1; i++) {
	    Double x1 = _xs[i];
	    Double x2 = _xs[i+1];
	    if(x1.floatValue()<= wx1 && x2.floatValue() >= wx2) {
		found=true;
		break;
	    }
	}
	if(found==false) {
	    return null;
	}
	int j;
	found=false;
	for(j=0; j<_ys.length-1; j++) {
	    Double y1 = _ys[j];
	    Double y2 = _ys[j+1];
	    if(y1.floatValue()<= wy1 && y2.floatValue() >= wy2) {
		found=true;
		break;
	    }
	}
	if(found==false) {
	    return null;
	}
	return new Point(i,j);
    }

    /**
     * Requirement: the line is either horizontal, either vertical.
     *
     * This function returns true if the input line overlaps with the existing grid.
     * If the line overlaps, then the x or y is added to the gridx/gridy. The gridx/gridy contains only the x/y values of the overlapping vertical/horizontal lines.
     *
     * If the grid is empty, then the input line will be taken a first line to build the grid.
     **/
    private boolean overlap(Line line,SortedSet<Double> gridx, SortedSet<Double> gridy) 
    {
	if(_isDefined == false) {
	    _minx = java.lang.Math.min(line._x1, line._x2);
	    _miny = java.lang.Math.min(line._y1, line._y2);
	    _maxx = java.lang.Math.max(line._x1, line._x2);
	    _maxy = java.lang.Math.max(line._y1, line._y2);
	    _isDefined = true;
	    return true;
	}
	else {
	    double l_minx = java.lang.Math.min(line._x1, line._x2);
	    double l_maxx = java.lang.Math.max(line._x1, line._x2);
	    if (l_minx <= _maxx && l_maxx >= _minx){
		double l_miny = java.lang.Math.min(line._y1, line._y2);
		double l_maxy = java.lang.Math.max(line._y1, line._y2);
		if (l_miny <= _maxy && l_maxy >= _miny) {
		    _minx = java.lang.Math.min(_minx, l_minx);
		    _miny = java.lang.Math.min(_miny, l_miny);
		    _maxx = java.lang.Math.max(_maxx, l_maxx);
		    _maxy = java.lang.Math.max(_maxy, l_maxy);
		    
		    if (line.isHorizontal()==true) { 
			//equal x
			gridx.add(line._x1);
		    }
		    else if (line.isVertical()==true) {
			//equal y
			gridy.add(line._y1);
		    }
		    return true;
		}
	    }
	}
	return false;
    }
    
    /**
     * Naive way to find all the overlapping lines (This could be done more efficiently by presorting the lines..., not sure if the added complexity is worth the effort)
     * The first line of the list is added to the grid, the rest of the lines are only added if there is an overlap with the lines in the grid. This is done until there are no more overlapping lines.
     * This process is repeated until all the lines are part of a grid.
     * The list of non overlapping lines are returned (this non-overlapping lines will be used to define a different grid).
     * 
    **/
    public List<Line> overlapping(List<Line> lines) 
    {
	SortedSet<Double> gridx = new TreeSet<Double>(); 
	SortedSet<Double> gridy = new TreeSet<Double>();

	List<Line> currentLines = lines;
	List<Line> nonProcessedLines = new ArrayList<Line>();
	
	boolean reprocess = true;
	while(reprocess == true)  {
	    reprocess = false;
	    for(Line line : currentLines) {
		if (overlap(line,gridx,gridy) == false) {
		    nonProcessedLines.add(line);
		}
		else {
		    reprocess = true;
		}
	    }
	    if (reprocess == true) {
		currentLines = nonProcessedLines;
		nonProcessedLines = new ArrayList<Line>();
	    }
	}

	Double[] xs = gridx.toArray(new Double[0]);
	Double[] ys = gridy.toArray(new Double[0]);

	_xs = collapse(xs, (double)Grid.COLLAPSE_X);
	_ys = collapse(ys, (double)Grid.COLLAPSE_Y);

	return nonProcessedLines;
    }
};
