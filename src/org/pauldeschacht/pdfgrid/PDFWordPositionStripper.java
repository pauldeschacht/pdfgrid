/**
 * This class extract the words from the PDF stream, much like the class org.apache.pdfbox.util.PDFTextStripper.
 * Some private members/functions of the PDFTextStripper are implemented in this class.
 *
 * The difference with PDFTextStripper is that this class keeps track of the position of each word (WordPosition)
 * Unlike the org.apache.pdfbox.example.util.PrintTextLocations, this class connects several characters into a single word. 
 */

package org.pauldeschacht.pdfgrid;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.PositionWrapper;
import org.apache.pdfbox.util.TextPosition;
import org.apache.pdfbox.util.TextNormalize;
import org.apache.pdfbox.util.TextPositionComparator;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

public class PDFWordPositionStripper extends PDFTextStripper
{
    List<WordPosition> _wordPositions;

   /**
     * The normalizer is used to remove text ligatures/presentation forms
     * and to correct the direction of right to left text, such as Arabic and Hebrew.
     */
    private TextNormalize normalize = null;
    private Map<PDFont,Map<Float,Float>> _fontMap;

    public PDFWordPositionStripper() throws IOException
    {
	super();
        this._fontMap = new HashMap<PDFont,Map<Float,Float>>();
	super.setSortByPosition(true);
	normalize = new TextNormalize(null);

	_wordPositions = new ArrayList<WordPosition>();
    }

    public PDFWordPositionStripper(String encoding) throws IOException
    {
	super(encoding);
        this._fontMap = new HashMap<PDFont,Map<Float,Float>>();
	super.setSortByPosition(true);
	normalize = new TextNormalize(encoding);

	_wordPositions = new ArrayList<WordPosition>();    
    }

    public List<WordPosition> getWordPositions() 
    {
	return _wordPositions;
    }
    
    public void processSinglePage(PDPage page) throws IOException
    {
	PDStream contentStream = page.getContents();
	if (contentStream != null) {
	    COSStream contents = contentStream.getStream();
	    processPage(page, contents);
	}

    }

    // copy from parent class 
    private static final float ENDOFLASTTEXTX_RESET_VALUE = -1;
    private static final float MAXYFORLINE_RESET_VALUE = -Float.MAX_VALUE;
    private static final float EXPECTEDSTARTOFNEXTWORDX_RESET_VALUE = -Float.MAX_VALUE;
    private static final float MAXHEIGHTFORLINE_RESET_VALUE = -1;
    private static final float MINYTOPFORLINE_RESET_VALUE = Float.MAX_VALUE;
    private static final float LASTWORDSPACING_RESET_VALUE = -1;

    /**
     * Slight modification of this function: call writeWordPositionLine instead of writeLines
     * The new function keeps the positions of each word.
     *
     * This will print the text of the processed page to "output".
     * It will estimate, based on the coordinates of the text, where
     * newlines and word spacings should be placed. The text will be
     * sorted only if that feature was enabled. 
     *
     * @throws IOException If there is an error writing the text.
     */
    protected void writePage() throws IOException
    {
        float maxYForLine = MAXYFORLINE_RESET_VALUE;
        float minYTopForLine = MINYTOPFORLINE_RESET_VALUE;
        float endOfLastTextX = ENDOFLASTTEXTX_RESET_VALUE;
        float lastWordSpacing = LASTWORDSPACING_RESET_VALUE;
        float maxHeightForLine = MAXHEIGHTFORLINE_RESET_VALUE;
        PositionWrapper lastPosition = null;
        PositionWrapper lastLineStartPosition = null;

        boolean startOfPage = true;//flag to indicate start of page
        boolean startOfArticle = true;
        if(charactersByArticle.size() > 0) 
        { 
	    //          writePageStart();
        }

        for( int i = 0; i < charactersByArticle.size(); i++)
        {
            List<TextPosition> textList = charactersByArticle.get( i );
            if( getSortByPosition() )
            {
                TextPositionComparator comparator = new TextPositionComparator();
                Collections.sort( textList, comparator );
            }

            Iterator<TextPosition> textIter = textList.iterator();

            /* Before we can display the text, we need to do some normalizing.
             * Arabic and Hebrew text is right to left and is typically stored
             * in its logical format, which means that the rightmost character is
             * stored first, followed by the second character from the right etc.
             * However, PDF stores the text in presentation form, which is left to
             * right.  We need to do some normalization to convert the PDF data to
             * the proper logical output format.
             *
             * Note that if we did not sort the text, then the output of reversing the
             * text is undefined and can sometimes produce worse output then not trying
             * to reverse the order.  Sorting should be done for these languages.
             * */

            /* First step is to determine if we have any right to left text, and
             * if so, is it dominant. */
            int ltrCnt = 0;
            int rtlCnt = 0;

            while( textIter.hasNext() )
            {
                TextPosition position = (TextPosition)textIter.next();
                String stringValue = position.getCharacter();
                for (int a = 0; a < stringValue.length(); a++)
                {
                    byte dir = Character.getDirectionality(stringValue.charAt(a));
                    if ((dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT ) ||
                            (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING) ||
                            (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE ))
                    {
                        ltrCnt++;
                    }
                    else if ((dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT ) ||
                            (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) ||
                            (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING) ||
                            (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE ))
                    {
                        rtlCnt++;
                    }
                }
            }

            // choose the dominant direction
            boolean isRtlDominant = rtlCnt > ltrCnt;

            startArticle(!isRtlDominant);
            startOfArticle = true;
            // we will later use this to skip reordering
            boolean hasRtl = rtlCnt > 0;

            /* Now cycle through to print the text.
             * We queue up a line at a time before we print so that we can convert
             * the line from presentation form to logical form (if needed). 
             */
            List<TextPosition> line = new ArrayList<TextPosition>();

            textIter = textList.iterator();    // start from the beginning again
            /* PDF files don't always store spaces. We will need to guess where we should add
             * spaces based on the distances between TextPositions. Historically, this was done
             * based on the size of the space character provided by the font. In general, this worked
             * but there were cases where it did not work. Calculating the average character width
             * and using that as a metric works better in some cases but fails in some cases where the
             * spacing worked. So we use both. NOTE: Adobe reader also fails on some of these examples.
             */
            //Keeps track of the previous average character width
            float previousAveCharWidth = -1;
            while( textIter.hasNext() )
            {
                float positionX = 0;
                float positionY = 0;
                float positionWidth = 0;
                float positionHeight = 0;

                TextPosition position = (TextPosition)textIter.next();
                PositionWrapper current = new PositionWrapper(position);
                String characterValue = position.getCharacter();

                //Resets the average character width when we see a change in font
                // or a change in the font size
                if(lastPosition != null && ((position.getFont() != lastPosition.getTextPosition().getFont())
                        || (position.getFontSize() != lastPosition.getTextPosition().getFontSize())))
                {
                    previousAveCharWidth = -1;
                }


                /* If we are sorting, then we need to use the text direction
                 * adjusted coordinates, because they were used in the sorting. */
                if (getSortByPosition())
                {
                    positionX = position.getXDirAdj();
                    positionY = position.getYDirAdj();
                    positionWidth = position.getWidthDirAdj();
                    positionHeight = position.getHeightDir();
                }
                else
                {
                    positionX = position.getX();
                    positionY = position.getY();
                    positionWidth = position.getWidth();
                    positionHeight = position.getHeight();
                }

                //The current amount of characters in a word
                int wordCharCount = position.getIndividualWidths().length;

                /* Estimate the expected width of the space based on the
                 * space character with some margin. */
                float wordSpacing = position.getWidthOfSpace();
                float deltaSpace = 0;
                if ((wordSpacing == 0) || (wordSpacing == Float.NaN))
                {
                    deltaSpace = Float.MAX_VALUE;
                }
                else
                {
                    if( lastWordSpacing < 0 )
                    {
                        deltaSpace = (wordSpacing * getSpacingTolerance());
                    }
                    else
                    {
                        deltaSpace = (((wordSpacing+lastWordSpacing)/2f)* getSpacingTolerance());
                    }
                }

                /* Estimate the expected width of the space based on the
                 * average character width with some margin. This calculation does not
                 * make a true average (average of averages) but we found that it gave the
                 * best results after numerous experiments. Based on experiments we also found that
                 * .3 worked well. */
                
                /*
                float averageCharWidth = -1;
                if(previousAveCharWidth < 0)
                {
                    averageCharWidth = (positionWidth/wordCharCount);
                }
                else
                {
                    averageCharWidth = (previousAveCharWidth + (positionWidth/wordCharCount))/2f;
                }

                float deltaCharWidth = (averageCharWidth * getAverageCharTolerance());
                * */
                
                float averageCharWidth = getSpaceWidthForFont(position.getFont(),position.getFontSize());
                float deltaCharWidth = (averageCharWidth); // * getAverageCharTolerance());
                
                //Compares the values obtained by the average method and the wordSpacing method and picks
                //the smaller number.
                float expectedStartOfNextWordX = EXPECTEDSTARTOFNEXTWORDX_RESET_VALUE;
                if(endOfLastTextX != ENDOFLASTTEXTX_RESET_VALUE)
                {
                    if(deltaCharWidth > deltaSpace)
                    {
                        expectedStartOfNextWordX = endOfLastTextX + deltaSpace;
                    }
                    else
                    {
                        expectedStartOfNextWordX = endOfLastTextX + deltaCharWidth;
                    }
                }

                if( lastPosition != null )
                {
                    if(startOfArticle)
                    {
                        lastPosition.setArticleStart();
                        startOfArticle = false;
                    }
                    // RDD - Here we determine whether this text object is on the current
                    // line.  We use the lastBaselineFontSize to handle the superscript
                    // case, and the size of the current font to handle the subscript case.
                    // Text must overlap with the last rendered baseline text by at least
                    // a small amount in order to be considered as being on the same line.

                    /* XXX BC: In theory, this check should really check if the next char is in full range
                     * seen in this line. This is what I tried to do with minYTopForLine, but this caused a lot
                     * of regression test failures.  So, I'm leaving it be for now. */
                    if(!overlap(positionY, positionHeight, maxYForLine, maxHeightForLine))
                    {
                        writeWordPositionLine(normalize(line,isRtlDominant,hasRtl));
                        line.clear();

                        lastLineStartPosition = 
                            handleLineSeparation(current, lastPosition, lastLineStartPosition, maxHeightForLine);

                        endOfLastTextX = ENDOFLASTTEXTX_RESET_VALUE;
                        expectedStartOfNextWordX = EXPECTEDSTARTOFNEXTWORDX_RESET_VALUE;
                        maxYForLine = MAXYFORLINE_RESET_VALUE;
                        maxHeightForLine = MAXHEIGHTFORLINE_RESET_VALUE;
                        minYTopForLine = MINYTOPFORLINE_RESET_VALUE;
                    }

                    //Test if our TextPosition starts after a new word would be expected to start.
                    if (expectedStartOfNextWordX != EXPECTEDSTARTOFNEXTWORDX_RESET_VALUE 
                            && expectedStartOfNextWordX < positionX &&
                            //only bother adding a space if the last character was not a space
                             lastPosition.getTextPosition().getCharacter() != null &&
                            !lastPosition.getTextPosition().getCharacter().endsWith( " " ) )
                    {
                        line.add(WordSeparator.getSeparator());
                    }
                }

                if (positionY >= maxYForLine)
                {
                    maxYForLine = positionY;
                }

                // RDD - endX is what PDF considers to be the x coordinate of the
                // end position of the text.  We use it in computing our metrics below.
                endOfLastTextX = positionX + positionWidth;

                // add it to the list
                if (characterValue != null)
                {
                    if(startOfPage && lastPosition==null)
                    {
			//                        writeParagraphStart();//not sure this is correct for RTL?
                    }
                    line.add(position);
                }
                maxHeightForLine = Math.max( maxHeightForLine, positionHeight );
                minYTopForLine = Math.min(minYTopForLine,positionY - positionHeight);
                lastPosition = current;
                if(startOfPage)
                {
                    lastPosition.setParagraphStart();
                    lastPosition.setLineStart();
                    lastLineStartPosition = lastPosition;
                    startOfPage=false;
                }
                lastWordSpacing = wordSpacing;
                previousAveCharWidth = averageCharWidth;
            }

            // print the final line
            if (line.size() > 0)
            {
		writeWordPositionLine(normalize(line,isRtlDominant,hasRtl));
		//                writeParagraphEnd();
            }

	    //            endArticle();
        }
	//        writePageEnd();
    }

    protected void writeWordPositionLine(List<String> words) 
    {
    }


    // rewrite private functions that are defined in parent class
    protected boolean overlap( float y1, float height1, float y2, float height2 )
    {
	return within( y1, y2, .1f) || (y2 <= y1 && y2 >= y1-height1) ||
	    (y1 <= y2 && y1 >= y2-height2);
    }
   
    /** This will determine of two floating point numbers are within a specified variance.
     *
     * @param first The first number to compare to.
     * @param second The second number to compare to.
     * @param variance The allowed variance.
     */
    protected boolean within( float first, float second, float variance )
    {
        return second < first + variance && second > first - variance;
    }

    protected WordPosition getPosition(TextPosition position) {
	float positionX = 0;
	float positionY = 0;
	float positionWidth = 0;
	float positionHeight = 0;

	if(position == null) {
	    WordPosition wp = new WordPosition();
	    wp.setRectangle(positionX, positionY, positionX + positionWidth, positionY - positionHeight);
	    return wp;
	}
	/* If we are sorting, then we need to use the text direction
	 * adjusted coordinates, because they were used in the sorting. */
	if (getSortByPosition()) {
	    positionX = position.getXDirAdj();
	    positionY = position.getYDirAdj();
	    positionWidth = position.getWidthDirAdj();
	    positionHeight = position.getHeightDir();
	}
	else {
	    positionX = position.getX();
	    positionY = position.getY();
	    positionWidth = position.getWidth();
	    positionHeight = position.getHeight();
	}
	WordPosition wp = new WordPosition();
	wp.setRectangle(positionX, positionY, positionX + positionWidth, positionY - positionHeight);
	return wp;
    }
 
    /**
     * Normalize the given list of TextPositions.
     * @param line list of TextPositions
     * @param isRtlDominant determines if rtl or ltl is dominant 
     * @param hasRtl determines if lines contains rtl formatted text(parts)
     * @return a list of strings, one string for every word
     */
    protected List<String> normalize(List<TextPosition> line, boolean isRtlDominant, boolean hasRtl)
    {
	TextPosition firstTextPosition = null;
	TextPosition lastTextPosition = null;

        LinkedList<String> normalized = new LinkedList<String>();
        StringBuilder lineBuilder = new StringBuilder();
        // concatenate the pieces of text in opposite order if RTL is dominant
        if (isRtlDominant)
        {
            int numberOfPositions = line.size();
            for(int i = numberOfPositions-1;i>=0;i--)
            {
                TextPosition text = line.get(i);
                if (text instanceof WordSeparator || text.getCharacter().endsWith(" ")==true) 
                {
		    String s = normalize.normalizePres(lineBuilder.toString());
                    normalized.add(s);

                    lineBuilder = new StringBuilder();

		    if (firstTextPosition != null) {
			WordPosition firstWordPosition = getPosition(firstTextPosition);
			WordPosition lastWordPosition = getPosition(lastTextPosition);
			WordPosition wordPosition = new WordPosition();
			wordPosition.setWord(s);
			wordPosition.setRectangle(firstWordPosition.x1(), firstWordPosition.y1(),
						  lastWordPosition.x2(), lastWordPosition.y2());
                    wordPosition.setSpaceWidth(getSpaceWidthForFont(firstTextPosition.getFont(), firstTextPosition.getFontSize()));
                        wordPosition.trimSpaces();
			_wordPositions.add(wordPosition);
		    }
		    firstTextPosition = null;
		    lastTextPosition = null;
                }
                else 
                {
		    //keep track of first and last character added
		    if (firstTextPosition == null) { firstTextPosition = text; }
		    lastTextPosition = text;
                    lineBuilder.append(text.getCharacter());
                }
            }
            if (lineBuilder.length() > 0) 
            {
		String s = normalize.normalizePres(lineBuilder.toString());
		normalized.add(s);
		
		if (firstTextPosition != null) {
		    WordPosition firstWordPosition = getPosition(firstTextPosition);
		    WordPosition lastWordPosition = getPosition(lastTextPosition);
		    WordPosition wordPosition = new WordPosition();
		    wordPosition.setWord(s);
		    wordPosition.setRectangle(firstWordPosition.x1(), firstWordPosition.y1(),
					      lastWordPosition.x2(), lastWordPosition.y2());
                    wordPosition.setSpaceWidth(getSpaceWidthForFont(firstTextPosition.getFont(), firstTextPosition.getFontSize()));
                        wordPosition.trimSpaces();
		    _wordPositions.add(wordPosition);
		}
		firstTextPosition = null;
		lastTextPosition = null;
            }
        }
        else
        {
            for(TextPosition text : line)
            {
                if (text instanceof WordSeparator || text.getCharacter().endsWith(" ")==true)
                {
		    String s = normalize.normalizePres(lineBuilder.toString());
                    normalized.add(s);
                    lineBuilder = new StringBuilder();

		    if (firstTextPosition != null) {
			WordPosition firstWordPosition = getPosition(firstTextPosition);
			WordPosition lastWordPosition = getPosition(lastTextPosition);
			WordPosition wordPosition = new WordPosition();
			wordPosition.setWord(s);
			wordPosition.setRectangle(firstWordPosition.x1(), firstWordPosition.y1(),
						  lastWordPosition.x2(), lastWordPosition.y2());
                                            wordPosition.fontName(firstTextPosition.getFont().getBaseFont());
                        wordPosition.fontSize(firstTextPosition.getFontSize());
                    wordPosition.setSpaceWidth(getSpaceWidthForFont(firstTextPosition.getFont(), firstTextPosition.getFontSize()));
                        wordPosition.trimSpaces();
			_wordPositions.add(wordPosition);
		    }
		    firstTextPosition = null;
		    lastTextPosition = null;
                }
                else 
                {
		    //keep track of first and last character added
		    if (firstTextPosition == null) { firstTextPosition = text; }
		    lastTextPosition = text;
                    lineBuilder.append(text.getCharacter());
                }
            }
            if (lineBuilder.length() > 0) 
            {
		String s = normalize.normalizePres(lineBuilder.toString());
		normalized.add(s);
		lineBuilder = new StringBuilder();
		
		if (firstTextPosition != null) {
		    WordPosition firstWordPosition = getPosition(firstTextPosition);
		    WordPosition lastWordPosition = getPosition(lastTextPosition);
		    WordPosition wordPosition = new WordPosition();
		    wordPosition.setWord(s);
		    wordPosition.setRectangle(firstWordPosition.x1(), firstWordPosition.y1(),
					      lastWordPosition.x2(), lastWordPosition.y2());
                    wordPosition.fontName(firstTextPosition.getFont().getBaseFont());
                    wordPosition.fontSize(firstTextPosition.getFontSize());
                    wordPosition.setPDFont(firstTextPosition.getFont());
                    wordPosition.setSpaceWidth(getSpaceWidthForFont(firstTextPosition.getFont(), firstTextPosition.getFontSize()));
                        wordPosition.trimSpaces();
		    _wordPositions.add(wordPosition);
		}
		firstTextPosition = null;
		lastTextPosition = null;
            }
        }
        return normalized;
    }

    /**
     * internal marker class.  Used as a place holder in
     * a line of TextPositions.
     * @author ME21969
     *
     */
    private static final class WordSeparator extends TextPosition
    {
        private static final WordSeparator separator = new WordSeparator();
        
        private WordSeparator()
        {
        }

        public static final WordSeparator getSeparator()
        {
            return separator;
        }

    }
    
    private float getSpaceWidthForFont(PDFont font, float fontSize) {
        
        if (_fontMap.containsKey(font)) {
            if(_fontMap.get(font).containsKey(fontSize)) {
                return _fontMap.get(font).get(fontSize);
            }
        }
        
        float width = 0f;
        try {
            PDDocument doc = new PDDocument();
            PDPage page = new PDPage();

            doc.addPage(page);
           // font = PDType1Font.HELVETICA_BOLD;

            PDPageContentStream content = new PDPageContentStream(doc, page);
            content.beginText();
            content.setFont(font, fontSize);
            content.moveTextPositionByAmount(0, 0);
            content.drawString(" ");

            content.endText();
            content.close();
            /*
                PDFWordPositionStripper wordPositionStripper = new PDFWordPositionStripper();
                wordPositionStripper.setStartPage(1);
                wordPositionStripper.setEndPage(1);
                wordPositionStripper.getText(doc);
                List<WordPosition> words = wordPositionStripper.getWordPositions();
            */
            
            SpaceWidthStripper stripper = new SpaceWidthStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            stripper.getText(doc);
            Vector<List<TextPosition>> chars = stripper.charactersByArticle();
            if(chars.size()>0) {
                if (chars.get(0).size()>0) {
                    TextPosition pos = chars.get(0).get(0);
                    if (pos.getCharacter().equals(" ")) {
                        width = pos.getWidth();
                        if (_fontMap.containsKey(font)==false) {
                            _fontMap.put(font, new HashMap<Float,Float>());
                        }
                        _fontMap.get(font).put(fontSize,width);
                    }
                }
            }
                  
            //doc.save("PDFWithText.pdf");
            doc.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        
        return width;
    }
}
