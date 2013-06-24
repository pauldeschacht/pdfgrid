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

public class LineTo extends OperatorProcessor
{
    private static final Log log = LogFactory.getLog(LineTo.class);

    public void process(PDFOperator operator, List<COSBase> arguments) throws IOException
    {
	try
	{
	    PageGridDrawer drawer = (PageGridDrawer)context;
	    COSNumber x = (COSNumber)arguments.get( 0 );
	    COSNumber y = (COSNumber)arguments.get( 1 );

	    drawer.lineTo(x.doubleValue(),y.doubleValue());
	}
	catch (Exception exception)
	{
	    log.warn(exception,exception);
	}
    }
}
