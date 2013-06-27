/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pauldeschacht.pdfgrid;

import java.io.IOException;
import java.util.List;
import java.util.Vector;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;

/**
 *
 * @author pdeschacht
 */
public class SpaceWidthStripper extends PDFTextStripper{
    public SpaceWidthStripper() throws IOException{
        super();
    }
    
    public  Vector<List<TextPosition>> charactersByArticle() {
        return this.charactersByArticle;
    }
}
