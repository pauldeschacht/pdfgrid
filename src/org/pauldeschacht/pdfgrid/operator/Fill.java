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

public class Fill extends OperatorProcessor
{
    private static final Log log = LogFactory.getLog(Fill.class);

    public void process(PDFOperator operator, List<COSBase> arguments) throws IOException
    {
	try
	{
	    PageGridDrawer drawer = (PageGridDrawer)context;
	    drawer.fill();
	}
	catch (Exception exception)
	{
	    log.warn(exception,exception);
	}
    }
}
