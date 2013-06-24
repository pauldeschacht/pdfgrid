/*
 * This class is a modified copy of the org.apache.pdfbox.util.PageDrawer
 * There is a separate resource file for the PDF operators and PageDrawer has a hardcoded link to it's own resource file
 */

package org.pauldeschacht.pdfgrid;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Area;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import java.awt.Image;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDMatrix;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.AxialShadingPaint;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingResources;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType3;
import org.apache.pdfbox.pdmodel.graphics.shading.RadialShadingPaint;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.text.PDTextState;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.PDFStreamEngine;
import org.apache.pdfbox.util.ResourceLoader;
import org.apache.pdfbox.util.TextPosition;
import org.apache.pdfbox.cos.COSName;


/**
 */
public class PageGridDrawer extends PDFStreamEngine
{
    /**
     * Log instance.
     */
    private static final Log LOG = LogFactory.getLog(PageGridDrawer.class);

    private Graphics2D graphics;
    
    /**
     * clipping winding rule used for the clipping path.
     */
    private int clippingWindingRule = -1;

    /**
     * Size of the page.
     */
    protected Dimension pageSize;
    /**
     * Current page to be rendered.
     */
    protected PDPage page;

    private GeneralPath linePath = new GeneralPath();

    /**
     * specific for finding the grids
     */
    protected boolean _closedPath;
    protected List<Line> _currentPath;
    protected List<Line> _path;
    protected double _startx, _starty, _currentx, _currenty;
    protected AffineTransform _pageAffineTransform;

    /**
     * Default constructor, loads properties from file.
     *
     * @throws IOException If there is an error loading properties from the file.
     */
    public PageGridDrawer() throws IOException
    {
	super( ResourceLoader.loadProperties("org/pauldeschacht/pdfgrid/resources/GridDrawer.properties", true ) );
	_closedPath = false;
	_currentPath = new ArrayList<Line>();
	_path = new ArrayList<Line>();
	_pageAffineTransform = null;
    }

    /**
     * This will draw the page to the requested context.
     *
     * @param p The page to draw.
     * @param pageDimension The size of the page to draw.
     *
     * @throws IOException If there is an IO error while drawing the page.
     */
    public void drawPage(PDPage p) throws IOException
    {
        page = p;

	PDRectangle cropBox = page.findCropBox();
	float widthPt = cropBox.getWidth();
	float heightPt = cropBox.getHeight();
	//        float scaling = resolution / (float)DEFAULT_USER_SPACE_UNIT_DPI;
	float scaling = (float)1.0;
	int widthPx = Math.round(widthPt * scaling);
	int heightPx = Math.round(heightPt * scaling);
	//TODO The following reduces accuracy. It should really be a Dimension2D.Float.
	pageSize = new Dimension( (int)widthPt, (int)heightPt );

	setPageAffineTransform(page);
        if ( page.getContents() != null) 
        {
            PDResources resources = page.findResources();
            processStream( page, resources, page.getContents().getStream() );
        }
        List<PDAnnotation> annotations = page.getAnnotations();
        for( int i=0; i<annotations.size(); i++ )
        {
            PDAnnotation annot = (PDAnnotation)annotations.get( i );
            PDRectangle rect = annot.getRectangle();
            String appearanceName = annot.getAppearanceStream();
            PDAppearanceDictionary appearDictionary = annot.getAppearance();
            if( appearDictionary != null )
            {
                if( appearanceName == null )
                {
                    appearanceName = "default";
                }
                Map<String, PDAppearanceStream> appearanceMap = appearDictionary.getNormalAppearance();
                if (appearanceMap != null) 
                { 
                    PDAppearanceStream appearance = 
                        (PDAppearanceStream)appearanceMap.get( appearanceName ); 
                    if( appearance != null ) 
                    { 
                        Point2D point = new Point2D.Float(rect.getLowerLeftX(), rect.getLowerLeftY());
                        Matrix matrix = appearance.getMatrix();
                        if (matrix != null) 
                        {
                            // transform the rectangle using the given matrix 
                            AffineTransform at = matrix.createAffineTransform();
                            at.transform(point, point);
                        }
			//                        g.translate( (int)point.getX(), -(int)point.getY() );
                        processSubStream( page, appearance.getResources(), appearance.getStream() ); 
			//                        g.translate( -(int)point.getX(), (int)point.getY() ); 
                    }
                }
            }
        }

    }

    /**
     * Get the page that is currently being drawn.
     *
     * @return The page that is being drawn.
     */
    public PDPage getPage()
    {
        return page;
    }

    /**
     * Get the size of the page that is currently being drawn.
     *
     * @return The size of the page that is being drawn.
     */
    public Dimension getPageSize()
    {
        return pageSize;
    }

    /**
     * Fix the y coordinate.
     *
     * @param y The y coordinate.
     * @return The updated y coordinate.
     */
    public double fixY( double y )
    {
        return pageSize.getHeight() - y;
    }

    protected void setPageAffineTransform(PDPage page) 
    {
        PDRectangle cropBox = page.findCropBox();
        float widthPt = cropBox.getWidth();
        float heightPt = cropBox.getHeight();
	//        float scaling = resolution / (float)DEFAULT_USER_SPACE_UNIT_DPI;
	float scaling = (float)1.0;
        int widthPx = Math.round(widthPt * scaling);
        int heightPx = Math.round(heightPt * scaling);
        //TODO The following reduces accuracy. It should really be a Dimension2D.Float.
        Dimension pageDimension = new Dimension( (int)widthPt, (int)heightPt );
	//BufferedImage retval = null;
        int rotationAngle = page.findRotation();
        // normalize the rotation angle
        if (rotationAngle < 0)
        {
            rotationAngle += 360;
        }
        else if (rotationAngle >= 360)
        {
            rotationAngle -= 360;
        }
        // swap width and height
        if (rotationAngle == 90 || rotationAngle == 270)
        {
	    //retval = new BufferedImage( heightPx, widthPx, BufferedImage.TYPE_BYTE_GRAY );
        }
        else
        {
            //retval = new BufferedImage( widthPx, heightPx, BufferedImage.TYPE_BYTE_GRAY );
        }
        //Graphics2D graphics = (Graphics2D)retval.getGraphics();
	//        graphics.setBackground( 0 /*TRANSPARENT_WHITE*/ );
        // graphics.clearRect( 0, 0, retval.getWidth(), retval.getHeight() );
	_pageAffineTransform = new AffineTransform();
        if (rotationAngle != 0)
        {
            int translateX = 0;
            int translateY = 0;
            switch(rotationAngle) 
            {
                case 90:
                    translateX = heightPx; //retval.getWidth();
                    break;
                case 270:
                    translateY = widthPx; //retval.getHeight();
                    break;
                case 180:
                    translateX = widthPx; // retval.getWidth();
                    translateY = heightPx; //retval.getHeight();
                    break;
                default:
                    break;
            }
            //graphics.translate(translateX,translateY);
            //graphics.rotate((float)Math.toRadians(rotationAngle));
	    _pageAffineTransform.translate(translateX,translateY);
	    _pageAffineTransform.rotate((float)Math.toRadians(rotationAngle));
        }
	//        graphics.scale( scaling, scaling );
	_pageAffineTransform.scale(scaling,scaling);
	//        PageDrawer drawer = new PageDrawer();
        //drawer.drawPage( graphics, this, pageDimension );

	//	_pageAffineTransform = graphics.getTransform();
    }

    //This code generalizes the code Jim Lynch wrote for AppendRectangleToPath
    /**
     * use the current transformation matrix to transform a single point.
     * @param x x-coordinate of the point to be transform
     * @param y y-coordinate of the point to be transform
     * @return the transformed coordinates as Point2D.Double
     */
    public java.awt.geom.Point2D.Double transformedPoint(double x, double y)
    {
        double[] position = {x,y}; 
        getGraphicsState().getCurrentTransformationMatrix().createAffineTransform().transform(position, 0, position, 0, 1);
        position[1] = fixY(position[1]);

	_pageAffineTransform.transform(position,0, position, 0, 1);
        return new Point2D.Double(position[0],position[1]);
    }

    /**
     * Impementation of the move operator. Move resets the current path
     **/
    public void moveTo(double x, double y)
    {
	_closedPath = false;
	Point2D pos = transformedPoint(x,y);
	_startx = pos.getX();
	_starty = pos.getY();
	_currentx = pos.getX();
	_currenty = pos.getY();
	_currentPath.clear();
    }

    /**
     * Implementation of the line operator. Adds a line segment to the current path
     **/
    public void lineTo(double x, double y)
    {
	Point2D pos = transformedPoint(x,y);
	double tx = pos.getX();
	double ty = pos.getY();
	
	Line line = new Line(_currentx, _currenty, tx, ty);

	System.out.println("+Line: " + line.toString());
	_currentPath.add(line);
	_currentx = tx;
	_currenty = ty;
    }

    /**
     * Implementation of the close path operator. Adds a line segment to the current path 
     **/
    public void closepath() 
    {
	Line line = new Line(_currentx, _currenty, _startx, _starty);
	_currentPath.add(line);
	_currentx = _startx;
	_currenty = _starty;
	_closedPath = true;
    }
    /**
     * Implementaion of the end path operator. See closepath
     **/
    public void endpath()
    {
	 closepath();
	//stroke();
	for(Line line : _currentPath) {
	    System.out.println("EndPath: " + line.toString());
	}
	_currentPath.clear();
	_closedPath=false;
    }

    /**
     * Implementation of the NonZeroFillRule. This operator actually make the current path 'real'.
     * For the grid extraction, only the vertical and horizontal lines are required.
     **/
    public void stroke() throws IOException
    {
	if (_closedPath == true) {
	    Paint strokingPaint = getGraphicsState().getStrokingColor().getJavaColor();
	    if ( strokingPaint == null ) {
		strokingPaint = getGraphicsState().getStrokingColor().getPaint(pageSize.height);
	    }
	    if ( strokingPaint == null ) {
		LOG.info("ColorSpace "+getGraphicsState().getStrokingColor().getColorSpace().getName() +" doesn't provide a stroking color, using white instead!");
		strokingPaint = Color.WHITE;
	    }
	    if (strokingPaint != Color.WHITE) {
		for(Line line : _currentPath) {
		    if(line.isVertical() || line.isHorizontal()) {
			_path.add(line);
		    }
		}
	    }
	}
	_currentPath.clear();
    }

    public void fill() throws IOException {
	if (_closedPath == true) {
	    Paint nonStrokingPaint = getGraphicsState().getNonStrokingColor().getJavaColor();
	    if ( nonStrokingPaint == null ) {
		nonStrokingPaint = getGraphicsState().getNonStrokingColor().getPaint(pageSize.height);
	    }
	    if ( nonStrokingPaint == null ) {
		LOG.info("ColorSpace "+getGraphicsState().getNonStrokingColor().getColorSpace().getName() +" doesn't provide a non-stroking color, using white instead!");
		nonStrokingPaint = Color.WHITE;
	    }
	    if(nonStrokingPaint != Color.WHITE) {
		for(Line line : _currentPath) {
		    if(line.isVertical() || line.isHorizontal()) {
			_path.add(line);
		    }
		}
	    }
	}
	_currentPath.clear();
    }

    /**
     * Once the vertical and horizontal lines are extracted from the PDF stream, it is possible to determine the different grids.
     **/
    public List<Grid> getGrids() {
	List<Grid> grids = new ArrayList<Grid>();
	List<Line> currentLines = _path;
	while(currentLines.size()>1) {
	    Grid grid = new Grid();
	    grids.add(grid);
	    List<Line> nonOverlappingLines = grid.overlapping(currentLines);
	    	    
	    Double[] xs = grid.xs();
	    Double[] ys = grid.ys();
	    
	    if (xs.length>3 && ys.length>3) {
		Double minx = xs[0];
		Double maxx = xs[xs.length-1];
		
		Double miny = ys[0];
		Double maxy = ys[ys.length-1];
		
		for(int i=0; i<xs.length; i++) {
		    System.out.println("Line: " + xs[i].toString() + "," + miny.toString() + "," + xs[i].toString() + "," + maxy.toString());
		}
		for(int i=0; i<ys.length; i++) {
		    System.out.println("Line: " + minx.toString() + "," + ys[i].toString() +  "," + maxx.toString() + "," + ys[i].toString());
		}
	    }

	    currentLines = nonOverlappingLines;
	}
	return grids;
    }
}
