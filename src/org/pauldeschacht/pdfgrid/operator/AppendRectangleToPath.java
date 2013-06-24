package org.pauldeschacht.pdfgrid.operator;

import java.util.List;
import java.io.IOException;

import org.pauldeschacht.pdfgrid.PageGridDrawer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.pdfbox.util.operator.OperatorProcessor;
import org.apache.pdfbox.util.PDFOperator;
import org.apache.pdfbox.util.operator.OperatorProcessor;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSNumber;

public class AppendRectangleToPath extends OperatorProcessor
{
    private static final Log log = LogFactory.getLog(AppendRectangleToPath.class);

    public void process(PDFOperator operator, List<COSBase> arguments) throws IOException
    {
	try
	{
	    PageGridDrawer drawer = (PageGridDrawer)context;

	    COSNumber x = (COSNumber)arguments.get( 0 );
	    COSNumber y = (COSNumber)arguments.get( 1 );
	    COSNumber w = (COSNumber)arguments.get( 2 );
	    COSNumber h = (COSNumber)arguments.get( 3 );
	    
	    double x1 = x.doubleValue();
	    double y1 = y.doubleValue();
	    // create a pair of coordinates for the transformation 
	    double x2 = w.doubleValue()+x1;
	    double y2 = h.doubleValue()+y1;

	    drawer.moveTo(x1,y1);
	    drawer.lineTo(x2,y1);
	    drawer.lineTo(x2,y2);
	    drawer.lineTo(x1,y2);
	    drawer.lineTo(x1,y1);
	    drawer.closepath();
	}
	catch (Exception exception)
	{
	    log.warn(exception,exception);
	}
    }
}
