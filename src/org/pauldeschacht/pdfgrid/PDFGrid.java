package org.pauldeschacht.pdfgrid;

import java.util.List;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;

public class PDFGrid {

    private PDFGrid() {
    }

    ;

    public static void main(String[] args) throws Exception {
        String filename = args[0];
        if (filename == null) {
            filename = "/Users/pauldeschacht/dev/pdfgrid/data/CAAC2012.pdf";
        }
        File file = new File(filename);
        if (file.isFile() && file.getName().endsWith(".pdf")) {
            parseFile(file.getAbsolutePath());
        } else {
            //process all PDF in the folder
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile() && files[i].getName().endsWith(".pdf")) {
                    parseFile(files[i].getAbsolutePath());
                }

            }
        }
    }

    protected static void parseFile(String pdfFile) throws Exception {
        String wpFilename = pdfFile + "-wordpositions.csv";
        File wpFile = new File(wpFilename);
        if (!wpFile.exists()) {
            wpFile.createNewFile();
        }
        FileWriter wpfw = new FileWriter(wpFile.getAbsoluteFile());
        BufferedWriter wpbw = new BufferedWriter(wpfw);


        String wpLineFilename = pdfFile + "-wordLinePositions.csv";
        File wpLineFile = new File(wpLineFilename);
        if (!wpLineFile.exists()) {
            wpLineFile.createNewFile();
        }
        FileWriter wpLinefw = new FileWriter(wpLineFile.getAbsoluteFile());
        BufferedWriter wpLinebw = new BufferedWriter(wpLinefw);

        System.out.println("Processing file " + pdfFile);
        PDDocument document = null;
        try {
            // extract the grid lines from the pdf
            document = PDDocument.load(pdfFile);

            // process page by page
            List pages = document.getDocumentCatalog().getAllPages();
            for (int pageNb = 1; pageNb <= pages.size(); pageNb++) {
                System.out.println("Page = " + pageNb);
                if (pageNb != 8) {
                        continue;
                }
                
                PDPage page = (PDPage)pages.get(pageNb-1);
                PDRectangle cropBox = page.findCropBox();
                float pageWidth = cropBox.getWidth();
                float pageHeight = cropBox.getHeight();
                
                //          for(int pageNb=3; pageNb<4; pageNb++) {
                //PDPage page = (PDPage)pages.get(pageNb);

                //PageGridDrawer gridDrawer = new PageGridDrawer();
                //gridDrawer.drawPage(page);

                //List<Grid> grids = gridDrawer.getGrids();
		/*
                 for(Grid grid: grids) {
                 Double[] xs = grid.xs();
                 System.out.print("\nx: ");
                 for(Double x: xs) {
                 System.out.print(Float.toString((float)x.floatValue()) + "," );
                 }
                 System.out.print("\ny: ");
                 Double[] ys = grid.ys();
                 for(Double y: ys) {
                 System.out.print(Float.toString((float)y.floatValue()) + "," );
                 }
                 System.out.print("\n");
                 }
                 */

                // extract the words and their positions from the page
                PDFWordPositionStripper wordPositionStripper = new PDFWordPositionStripper();
                wordPositionStripper.setStartPage(pageNb);
                wordPositionStripper.setEndPage(pageNb);
                wordPositionStripper.getText(document);
                List<WordPosition> words = wordPositionStripper.getWordPositions();

                
                // create csv file: page, x1, y1, x2, y2, word
                for (WordPosition word : words) {
                    wpbw.write(Integer.toString(pageNb) + ";");
                    wpbw.write(word.toString());
                    wpbw.write("\n");
                }

                // assign a line number to each word

                //sort on bottom line of each bounding box 
                //and assign a line number based on that bottom line
                Collections.sort(words, new WordPositionComparator());
                float LINE_MARGIN = 1.5f; //should be based on the height of the font
                float lineY = words.get(0).y1();
                int lineNb = 0;
                for (WordPosition word : words) {
                    float y = word.y1();
                    if (Math.abs(lineY - y) > LINE_MARGIN) {
                        lineNb++;
                        lineY = y;
                    }
                    //to gradually slide the line position: lineY = y;
                    word.setLineNb(lineNb);
                }

                int numLines = lineNb + 1;

                //sort the words according line number
                Collections.sort(words, new WordPositionLineComparator());


                //tryWithAlignment(words,numLines);

                //tryWithAlignmentClusters(words, numLines);
                
                //break;

                // build a map so that all the word in a single line can be accessed.
                // csv file: page, line, x1,y1,x2,y2, word
                Map<Integer, List<WordPosition>> lines = new HashMap<Integer, List<WordPosition>>();
                for (WordPosition word : words) {
                    wpLinebw.write(Integer.toString(pageNb) + ";");
                    wpLinebw.write(Integer.toString(word.getLineNb()) + ";");
                    wpLinebw.write(word.toString());
                    wpLinebw.write("\n");

                    Integer line = new Integer(word.getLineNb());
                    if (lines.containsKey(line) == true) {
                        lines.get(line).add(word);
                    } else {
                        lines.put(line, new ArrayList<WordPosition>());
                        lines.get(line).add(word);
                    }
                }
                wpLinebw.flush();

                
                //deal the space as a thousand separator
                
                for(Map.Entry<Integer,List<WordPosition>> kv: lines.entrySet()) {
                    Integer lineN = kv.getKey();
                    List<WordPosition> line = kv.getValue();
                    
                    int i=0;
                    while(i<line.size()-1) {
                        WordPosition w1 = line.get(i);
                        WordPosition w2 = line.get(i+1);
                        if(w1.isNumber() && w2.isNumber() && (w1.x2() + (1.5f * w1.getSpaceWidth()) > w2.x1())) {
                            //if both words are numbers and the space between the words is small (bit more than space width) --> merge the 2 words
                            w1.merge(w2);
                            line.remove(i+1);
                        }
                        else {
                            i++;
                        }
                    }
                }
                

                List<List<List<String>>> tables = tryWithSimilarityMatrix(lines,pageWidth,pageHeight,pdfFile,pageNb);
                for(List<List<String>> table : tables) {
                    for(List<String> row: table) {
                        for(String cell: row) {
                            System.out.print(cell);
                            System.out.print("\t|");
                        }
                        System.out.println();
                    }
                    System.out.println("------------------------------------");
                }
                
                
                
                //prepare the counter matrix
                int[][] counters = new int[numLines][numLines];
                for (int i = 0; i < numLines; i++) {
                    for (int j = 0; j < numLines; j++) {
                        counters[i][j] = -1;
                    }
                }

                for (int i = 0; i < numLines; i++) {

                    List<Span> spans = extractWhiteSpans(lines.get(i));
                    for (int j = i + 1; j < numLines; j++) {
                        float avgCharWidth = calcAverageCharWidth(lines.get(j));
                        spans = intersectingSpans(spans, extractWhiteSpans(lines.get(j)), avgCharWidth);
                        counters[i][j] = spans.size();

                        //System.out.println(i + "," + j + "," + spans.size() + " " + lines.get(i).get(0).word());
                        //System.out.println(i + "," + j + "," + spans.size());
                        if (spans.isEmpty()) {
                            break;
                        }
                    }

                }

                List<Range> ranges = findPerfectTable(counters, lines);
                for (Range range : ranges) {
                    int startRow = range.start();
                    int nb = range.num() + 1;

                    //recalculate the common spans
                    List<Span> spans = extractWhiteSpans(lines.get(startRow));
                    int startRowSpanSize = spans.size();

                    for (int i = startRow + 1; i < startRow + nb; i++) {
                        float avgCharWidth = calcAverageCharWidth(lines.get(i));
                        spans = intersectingSpans(spans, extractWhiteSpans(lines.get(i)), avgCharWidth);
                    }

                    if (startRowSpanSize < spans.size() - 2) {
                        //skip start row, probably a header
                        startRow++;
                    }

                    //System.out.println("TABLE start row" + startRow + ", identical #lines: " + nb + ", nb spans " + spans.size() + "  -------------------------");

                    for (int i = startRow; i < startRow + nb; i++) {
                        //printLineAccordingSpan(lines.get(i), spans);
                    }
                }
                /*
                 * what is a table ?
                 * A table is a number of consecutive lines. The consecutive lines have all the same spans in common.
                 * common spans = intersection of all the spans)
                 * The above calculates the number of 
                 *   0 1 2 3 4 5 ...
                 * 0 X a b
                 * 1 X X
                 * 2 X X X     c
                 * 3 X X X X
                 * 4 X X X X X
                 * 5 X X X X X ...
                 * ...
                 * a is the number of common spans between line 0 with line 1
                 * b is the number of common spans between line 0 with line 1 and line 2
                 * c is the number of common spans between line 2 with line 3, line 4 and line 5
                 * 
                 * for a given row, if the number increases: 3 --> 5 --> 7
                 *  -> column gets splits into sub columns (example header 3 spans, then subheader of 5 spans and then 7 columns)
                 * 
                 * for a given row, if the number decreases: 13 --> 12 --> 12 (columns are merged)
                 *  -> either end of table
                 *  -> either previous line in table did not really have 13 columns (wrong split)
                 */

                /*
                 for(int g=0; g<grids.size(); g++) {
			
                 //extract csv data
                 Grid grid = grids.get(g);
                 String[][] csv = grid.csv(words);
		    
                 //create csv file for this table
                 String csvFilename = pdfFile;
                 if(grids.size()>1) {
                 csvFilename = csvFilename + "-page-" + Integer.toString(pageNb) + "-table" + Integer.toString(g) + ".tsv";
                 }
                 else {
                 csvFilename = csvFilename + ".tsv";
                 }
                 File file = new File(csvFilename);
                 if(!file.exists()) {
                 file.createNewFile();
                 }
                 FileWriter fw = new FileWriter(file.getAbsoluteFile());
                 BufferedWriter bw = new BufferedWriter(fw);
		    
                 for(int row=0; row<csv.length; row++) {
                 for(int col=0; col<csv[row].length; col++) {
                 bw.write(csv[row][col]);
                 if(col<csv[row].length-1) {
                 bw.write("\t");
                 }
                 }
                 bw.write("\n");
                 }
                 bw.close();
                 }
                 */
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    //white space is the empty space between 2 consecutive words
    static List<Span> extractWhiteSpans(List<WordPosition> words) {

        List<Span> spans = new ArrayList<Span>();
        //Span span = new Span();
        //span.f1(0);
        float f1 = 0f;
        float f2 = 0f;
        for (int i = 0; i < words.size(); i++) {
            WordPosition w = words.get(i);
            f2 = w.x1();
            if (f2 > f1) {
                Span span = new Span(f1, f2);
                span.f2(f2);
                spans.add(span);
            }
            f1 = w.x2();
            //span = new Span();
            //span.f1(w.x2());
        }
        Span span = new Span(f1, Float.MAX_VALUE);
        //span.f2(Float.MAX_VALUE);
        spans.add(span);
        return spans;
    }

    static List<Span> intersectingSpans(List<Span> spans1, List<Span> spans2, float avgSpaceWidth) {
        int i = 0;
        int j = 0;

        List<Span> intersectingSpans = new ArrayList<Span>();
        while (i < spans1.size() && j < spans2.size()) {
            Span span1 = spans1.get(i);
            Span span2 = spans2.get(j);
            Span intersection = null;
            Span.POSITION relativePos = span1.overlap(span2);
            switch (relativePos) {
                case BEFORE: {
                    //span1 before span2 --> skip span1
                    i++;
                    break;
                }
                case BEFORE_OVERLAP: {
                    //span1 overlaps the first part span2
                    intersection = span1.intersect(span2);

                    i++;
                    break;
                }
                case ENCLOSED: {
                    //span1 is completely enclosed by span2
                    intersection = span1.intersect(span2);

                    i++;
                    break;
                }
                case SPANNED: {
                    //span1 is completely overspanning span2
                    intersection = span1.intersect(span2);

                    j++;
                    break;
                }
                case AFTER_OVERLAP: {
                    //span1 overlaps with the last part of span2
                    intersection = span1.intersect(span2);

                    j++;
                    break;
                }
                case AFTER: {
                    //span1 starts after span2
                    j++;
                    break;
                }
            }
            //if span is smaller than 3 times the width of space, ignore the span
            //and consider as one word
            //depends on width of the space character
            if (intersection != null && intersection.getWidth() >= 1 * avgSpaceWidth) {
                intersectingSpans.add(intersection);
            }
        }
        return intersectingSpans;
    }

    static void printSpans(List<Span> spans) {
        for (Span span : spans) {
            System.out.print("[" + span.f1() + ";" + span.f2() + "] ");
        }
        System.out.println();
    }

    static void printLineAccordingSpan(List<WordPosition> line, List<Span> spans) {
        int i = 0;
        int j = 0;
        while (i < line.size() && j < spans.size()) {
            WordPosition word = line.get(i);
            Span span = spans.get(j);

            if (word.x2() <= span.f1()) {
                System.out.print(word.word());
                System.out.print(" ");
                i++;
            } else {
                System.out.print("\t");
                j++;
            }
        }
        while (i < line.size()) {
            WordPosition word = line.get(i);
            System.out.print(word.word());
            System.out.print(" ");
            i++;
        }
        while (j < spans.size()) {
            System.out.print("\t");
            j++;
        }
        System.out.println();
    }
    
    static List<String> splitLineAccordingSpan(List<WordPosition> line, List<Span> spans) {
        List<String> strings = new ArrayList<String>();
        String currentString = "";
        int i = 0;
        int j = 0;
        while (i < line.size() && j < spans.size()) {
            WordPosition word = line.get(i);
            Span span = spans.get(j);

            if (word.x2() <= span.f1()) {
                //System.out.print(word.word());
                //System.out.print(" ");
                currentString += word.word();
                currentString += " ";
                i++;
            } else {
                strings.add(currentString);
                currentString = "";
                //System.out.print("\t");
                j++;
            }
        }
        if( i<line.size()) {
            while (i < line.size()) {
                WordPosition word = line.get(i);
                //System.out.print(word.word());
                //System.out.print(" ");
                currentString += word.word();
                currentString += " ";
                i++;
            }
            strings.add(currentString);
        }
        else {
            while (j < spans.size()) {
                //System.out.print("\t");
                currentString = "";
                strings.add(currentString);
                j++;
            }
        }
        //System.out.println();
        return strings;
    }
    

    static List<Range> findPerfectTable(int[][] counters, Map<Integer, List<WordPosition>> lines) {
        List<Range> perfectRanges = new ArrayList<Range>();
        //perfect table is defined as an sequence of lines with an identical number of spans
        int[] identicals = new int[counters.length];
        int[] values = new int[counters.length];

        //for each row in the counters matrix, get the sequence of identical counters
        int row = 0;
        while (row < counters.length - 1) {
            int col = row + 1; //only half of matrix populated
            int prevValue = counters[row][col];
            int num = 0;
            for (; col < counters[row].length; col++) {
                if (counters[row][col] != prevValue) {
                    break;
                } else {
                    num++;
                }
            }
            if (num > 2) {
                identicals[row] = num;
                values[row] = prevValue;
            } else {
                identicals[row] = 0;
                values[row] = 0;
            }
            row++;
        }
        row = 0;
        while (row < identicals.length) {
            int skip = identicals[row];
            int val = values[row];
            //System.out.println("[" + row + "] => " + skip);
            if (val > 0) {
                boolean found = false;
                //see if a bigger values follows within skip lines
                for (int r = row + 1; r <= row + skip; r++) {
                    if (identicals[r] > val) {
                        found = true;
                        break;
                    }
                }
                if (found == false) {
                    //nothing bigger found, add to range and skip the following lines
                    Range range = new Range(row, skip);
                    perfectRanges.add(range);
                    row += skip;
                } else {
                    row++;
                }
            } else {
                row++;
            }
        }

        return perfectRanges;
    }





    static void tryWithAlignment(List<WordPosition> words, int numLines) {

        List<WordPosition> leftAlignment = new ArrayList<WordPosition>();
        List<WordPosition> rightAlignment = new ArrayList<WordPosition>();
        List<WordPosition> centerAlignment = new ArrayList<WordPosition>();

        for (WordPosition word : words) {
            leftAlignment.add(word);
            rightAlignment.add(word);
            centerAlignment.add(word);
        }
        Collections.sort(leftAlignment, new WordPositionLeftAlignment());
        Collections.sort(rightAlignment, new WordPositionRightAlignment());
        Collections.sort(centerAlignment, new WordPositionCenterAlignment());

        //count the number of different x's and create a mapping from float(x) to the index that will contain list of lines
        float prevx = -1f;
        int counter = 0;
        Map<Float, Integer> leftFloatToIndex = new TreeMap<Float, Integer>();
        for (WordPosition leftAlignedWord : leftAlignment) {
            float coordinate = toBucket(leftAlignedWord.x1());
            if (coordinate != prevx) {
                leftFloatToIndex.put(coordinate, counter);
                counter++;
                prevx = coordinate;
            }
        }
        // floatToIndex maps an x coordinate to a index. 
        // the index points to a row in the matrix. 
        // a row in the matrix corresponds to a x coordinate
        // a column in the matrix corresponds to a line
        // each cell in the matrix contains either 1 or 0 and tells if that line has a word on the coordinate

        int[][] leftAlignedLines = new int[counter][numLines];
        for (int i = 0; i < leftAlignedLines.length; i++) {
            for (int j = 0; j < leftAlignedLines[i].length; j++) {
                leftAlignedLines[i][j] = 0;
            }
        }

        prevx = -1f;
        counter = 0;
        Map<Float, Integer> rightFloatToIndex = new TreeMap<Float, Integer>();
        for (WordPosition rightAlignedWord : rightAlignment) {
            float coordinate = toBucket(rightAlignedWord.x2());
            if (coordinate != prevx) {
                rightFloatToIndex.put(coordinate, counter);
                counter++;
                prevx = coordinate;
            }
        }

        int[][] rightAlignedLines = new int[counter][numLines];
        for (int i = 0; i < rightAlignedLines.length; i++) {
            for (int j = 0; j < rightAlignedLines[i].length; j++) {
                rightAlignedLines[i][j] = 0;
            }
        }
        for (WordPosition leftAlignedWord : leftAlignment) {
            int index = leftFloatToIndex.get(toBucket(leftAlignedWord.x1()));
            leftAlignedLines[index][leftAlignedWord.getLineNb()] = 1;
        }
        for (WordPosition rightAlignedWord : rightAlignment) {
            int index = rightFloatToIndex.get(toBucket(rightAlignedWord.x2()));
            rightAlignedLines[index][rightAlignedWord.getLineNb()] = 1;
        }

        for (Map.Entry<Float, Integer> kv : leftFloatToIndex.entrySet()) {
            float coordinate = kv.getKey();
            int index = kv.getValue();
            int num = 0;
            int[] lines = leftAlignedLines[index];
            int prevPresent = 0;
            for (int i = 0; i < numLines; i++) {
                if (prevPresent == lines[i]) {
                    num++;
                } else {
                    if (prevPresent == 1 && num > 3) {
                        //System.out.println("Coordinate " + coordinate + " has " + num + " left aligned lines ending with line " + i);
                    }
                    prevPresent = lines[i];
                    num = 0;
                }

            }
        }

        for (Map.Entry<Float, Integer> kv : rightFloatToIndex.entrySet()) {
            float coordinate = kv.getKey();
            int index = kv.getValue();
            int num = 0;
            int[] lines = rightAlignedLines[index];
            int prevPresent = 0;
            for (int i = 0; i < numLines; i++) {
                if (prevPresent == lines[i]) {
                    num++;
                } else {
                    if (prevPresent == 1 && num > 3) {
                        //System.out.println("Coordinate " + coordinate + " has " + num + " right aligned lines ending with line " + i);
                    }
                    prevPresent = lines[i];
                    num = 0;
                }
            }
        }
    }

    static void tryWithAlignmentClusters(List<WordPosition> words, int numLines) {

        List<WordCluster> rightClusters = buildRightAlignedClusters(words);
        List<WordCluster> mergedClusters = mergeClusters(rightClusters);
        int[][] rightMatrix = buildAlignmentMatrix(mergedClusters,numLines);
        int numClusters = rightMatrix.length;
        
  
        /*
         * search for a consective list of lines
         * 
         */
        for (int i = 0; i < numClusters; i++) {
            int prevPresent = 0;
            int num = 0;
            for (int j = 0; j < numLines; j++) {
                if (rightMatrix[i][j] == prevPresent) {
                    num++;
                } else {
                    if (prevPresent == 1 && num > 3) {
                        Span s = mergedClusters.get(i).getSpan();
                        //System.out.println("Cluster " + s.f1() + "-" + s.f2() + " has " + num + " words right aligned, ending on line " + j);
                    }
                    prevPresent = rightMatrix[i][j];
                    num = 0;
                }
            }
        }
    }

    static void writeWordClusters(String pdfFile, int page, List<WordCluster> clusters) {
        try {
            String wpWordClustersFilename = pdfFile + "-wordClusters.csv";
            File wpWordClusters = new File(wpWordClustersFilename);
            if (!wpWordClusters.exists()) {
                wpWordClusters.createNewFile();
            }
            FileWriter wpWordClustersfw = new FileWriter(wpWordClusters.getAbsoluteFile(),true);
            BufferedWriter wpWordClustersbw = new BufferedWriter(wpWordClustersfw);
            
            int clusterIndex = 0;
            for(WordCluster wc: clusters) {
                
                for(WordPosition wp:wc.getWords()) {
                    wpWordClustersbw.write(Integer.toString(page) + "\t");
                    wpWordClustersbw.write(Integer.toString(clusterIndex) + "\t");
                    wpWordClustersbw.write(Float.toString(wc.getSpan().f1()) + "\t");
                    wpWordClustersbw.write(Float.toString(wc.getSpan().f2()) + "\t");
                    wpWordClustersbw.write(wp.toString(" ") + ",");
                    wpWordClustersbw.write("\n");
                }
                clusterIndex++;


                
                        
            }
            wpWordClustersbw.flush();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static List<WordCluster> buildRightAlignedClusters(Map<Integer,List<WordPosition>> lines) {
        List<WordPosition> words = new ArrayList<WordPosition>();
        for(Map.Entry<Integer,List<WordPosition>> kv : lines.entrySet()) {
            for(WordPosition word: kv.getValue()) {
                words.add(word);
            }
        }
        return buildRightAlignedClusters(words);
    }
    static List<WordCluster> buildRightAlignedClusters(List<WordPosition> words) {
                //right alignment
        List<WordCluster> rightClusters = new ArrayList<WordCluster>();
        for (WordPosition word : words) {
            boolean bAdded = false;
            for (WordCluster c : rightClusters) {
                if (c.doesBelongToCluster(word.x2()) == true) {
                    c.addWord(word.x2(), word);
                    bAdded = true;
                    break;
                }
            }
            if (bAdded == false) {
                WordCluster c = new WordCluster();
                c.addWord(word.x2(), word);
                rightClusters.add(c);
            }
        }
        return rightClusters;
    }
    static List<WordCluster> buildLeftAlignedClusters(Map<Integer,List<WordPosition>> lines) {
        List<WordPosition> words = new ArrayList<WordPosition>();
        for(Map.Entry<Integer,List<WordPosition>> kv : lines.entrySet()) {
            for(WordPosition word: kv.getValue()) {
                words.add(word);
            }
        }
        List<WordCluster> leftClusters = new ArrayList<WordCluster>();
        for (WordPosition word : words) {
            boolean bAdded = false;
            for (WordCluster c : leftClusters) {
                if (c.doesBelongToCluster(word.x1()) == true) {
                    c.addWord(word.x1(), word);
                    bAdded = true;
                    break;
                }
            }
            if (bAdded == false) {
                WordCluster c = new WordCluster();
                c.addWord(word.x1(), word);
                leftClusters.add(c);
            }
        }
        return leftClusters;
        
    }
    
    /*
     * this method changes the rightClusters !
     */
    static List<WordCluster> mergeClusters(List<WordCluster> clusters) {
            
       Collections.sort(clusters, new Comparator<WordCluster>() {
            @Override
            public int compare(WordCluster c1, WordCluster c2) {
                float f1 = c1.getSpan().f1();
                float f2 = c2.getSpan().f1();
                if (f1 < f2) {
                    return -1;
                } else if (f1 > f2) {
                    return 1;
                }
                return 0;
            }
        });
        
        //merge the clusters
        int i = 0;
        while (i < clusters.size()-1) {

            WordCluster ci = clusters.get(i);
            if (ci != null) {
                int j = i + 1;
                while (j < clusters.size()) {
                    WordCluster cj = clusters.get(j);
                    if (cj != null) {
                        if (ci.overlap(cj) == true) {
                            ci.merge(cj);
                            clusters.set(j, null);
                            j++;
                            i=j;
                        } else {
                            //there is no overlap between ci and cj
                            //since clusters are ordered, we can stop here and take the next i
                            i = j;
                            break;
                        }
                    } else {
                        //cj is null, take next cj
                        j++;
                        i=j;
                    }
                }
                //finished processing all j
            } else {
                //ci is null, take next cluster
                i++;
            }
        }
        
        List<WordCluster> mergedClusters = new ArrayList<WordCluster>();
        for(WordCluster cluster: clusters) {
            if(cluster!=null) {
                mergedClusters.add(cluster);
            }
        }
        
        return mergedClusters;
    }
    
    static int[][] buildAlignmentMatrix(List<WordCluster> clusters, int numLines) {
        
        int numClusters = clusters.size();
        int[][] rightMatrix = new int[numClusters][numLines];
        for (int i = 0; i < numClusters; i++) {
            for (int j = 0; j < numLines; j++) {
                rightMatrix[i][j] = 0;
            }
        }
        /*
         *                      0 1 2 3 ...
         * center cluster i     a 0 0 c
         * center cluster i+1   x y z ...
         * 
         * The cell of (cluster i, line j) tells if the line j has a wordposition that is aligned on center of cluster x
         */

        for (int i = 0; i < numClusters; i++) {
            WordCluster c = clusters.get(i);
            List<WordPosition> clusteredWords = c.getWords();
            for (WordPosition clusteredWord : clusteredWords) {
                rightMatrix[i][clusteredWord.getLineNb()] = 1;
            }
        }

        return rightMatrix;
    }

    static float toBucket(float x) {
        float result = (float) (int) (Math.floor(x));
        //System.out.println("bucket[" + x + "]=" + result);
        return result;
    }
    
    
    static void wordPositionSetCluster(List<WordCluster> clusters, List<WordPosition> words, int clusterIndex) {
        for(int i=0;i<clusters.size();i++) {
            WordCluster cluster = clusters.get(i);
            for(WordPosition wp: cluster.getWords()) {
                wp._clusterRef[clusterIndex] = i;
            }
        }
        
    }
    static List<WordCluster> unifiedWordCluster(List<WordCluster> rightClusters1, List<WordCluster> leftClusters, List<WordPosition> words) {
    
        List<WordCluster>  unifiedCluster = new ArrayList<WordCluster>();
        // 1 --> right aligned
        // 2 --> left aligned
        for(WordPosition wp : words) {
            int lineNb = wp.getLineNb();
            WordCluster c1 = rightClusters1.get(wp._clusterRef[0]);
            WordCluster c2 = leftClusters.get(wp._clusterRef[1]);
            
            int aligned1 = c1.getAlignedLines(lineNb);
            int aligned2 = c2.getAlignedLines(lineNb);
            
            float x;
            if (aligned1 > aligned2 ) {
                x = wp.x2();
            }
            else {
                x = wp.x1();
            }
            
            boolean bAdded=false;
            for(WordCluster cluster:unifiedCluster) {
                if (cluster.doesBelongToCluster(x) == true) {
                    cluster.addWord(x, wp);
                    bAdded=true;
                    break;
                }
            }
            if(!bAdded) {
                WordCluster cluster = new WordCluster();
                cluster.addWord(x, wp);
                unifiedCluster.add(cluster);
            }
        }
        Collections.sort(unifiedCluster, new Comparator<WordCluster>() {
            @Override
            public int compare(WordCluster c1, WordCluster c2) {
                float f1 = c1.getSpan().f1();
                float f2 = c2.getSpan().f1();
                if (f1 < f2) {
                    return -1;
                } else if (f1 > f2) {
                    return 1;
                }
                return 0;
            }
        });
        wordPositionSetCluster(unifiedCluster,words,3);
        return unifiedCluster;
    }
        
    static List<List<List<String>>> tryWithSimilarityMatrix(Map<Integer, List<WordPosition>> lines,float pageWidth, float pageHeight, String pdfFile, int pageNb) {
            
        int numLines = lines.size();
        
        float[][] lineFeatures = new float[numLines-1][]; 
    
        List<WordCluster> rightClusters = buildRightAlignedClusters(lines);
        List<WordCluster> mergedRightClusters = mergeClusters(rightClusters);
        for(WordCluster cluster: mergedRightClusters) {
            cluster.calcAlignedLines();
        }
        
        List<WordCluster> leftClusters = buildLeftAlignedClusters(lines);
        List<WordCluster> mergedLeftClusters = mergeClusters(leftClusters);
        for(WordCluster cluster: mergedLeftClusters) {
            cluster.calcAlignedLines();
        }
        
        List<WordPosition> listOfWords = new ArrayList<WordPosition>();
        for(Map.Entry<Integer,List<WordPosition>> kv : lines.entrySet()) {
            for(WordPosition word: kv.getValue()) {
                listOfWords.add(word);
            }
        }
        wordPositionSetCluster(mergedRightClusters,listOfWords,0);
        wordPositionSetCluster(mergedLeftClusters,listOfWords,1);

        List<WordCluster> mergedClusters = unifiedWordCluster(mergedRightClusters, mergedLeftClusters,listOfWords);

        int[][] alignmentMatrix = buildAlignmentMatrix(mergedClusters,numLines);
        
        writeWordClusters(pdfFile,pageNb, mergedClusters);
        
        for(int i=0; i<numLines-1; i++) {
            List<WordPosition> words1 = lines.get(i);
            List<WordPosition> words2 = lines.get(i+1);
            
            lineFeatures[i] = similarityLines(words1, i, words2, i+1, alignmentMatrix,pageWidth);
            
            //System.out.println("lines " + i + " and " + (i+1) + " pixel increase " + lineFeatures[i][6]);
        }
        normalizeSimilarityMatrix(lineFeatures);
        
        float[] similarities = calcRelativeSimilarities(lineFeatures);
        
        boolean bFirstRow=true;
        
        List<List<List<WordPosition>>> tables = new ArrayList<List<List<WordPosition>>>();

        ArrayList<List<WordPosition>> currentTable = null;
        for(int i=0; i<numLines-1; i++) {
            float[] line = lineFeatures[i];
            /*
            System.out.print("Similarity line " + i + " and " + (i+1) + ": " + similarities[i] + " features: ");
            for(int feature=0; feature<line.length;feature++) {
                System.out.print(line[feature] + ", ");
            }
            System.out.println();
            */
            
            //TODO: once similar lines are detected --> extract x lines
            //currently only works with right aligned cluster
            
            //i and i+1 are similar and both have more than 3 aligned words ==> similar lines in table
            if(similarities[i] > 0.90 && getNumAlignedWords(alignmentMatrix, i, i+1) > 3) {
                if(bFirstRow==true) {
                    System.out.println(">>-------------");
                    currentTable = new ArrayList<List<WordPosition>>();
                    tables.add(currentTable);
                    //currentTable.add(lines.get(i));
                    currentTable.add(lines.get(i));
                    printAccordingCluster(lines.get(i), mergedClusters);
                    bFirstRow=false;
                }
                printAccordingCluster(lines.get(i+1), mergedClusters);
                    //copy line into the current table: cannot copy from List<WordPosition> to ArrayList<WordPosition>
                currentTable.add(lines.get(i+1));
                
            }
            else {
                System.out.println("<<-------------");
                System.out.println(similarities[i]);
                bFirstRow=true;
            }     
        }
        //remove tables with less than 2 rows and transformed fomr WordPosition to String
        List<List<List<String>>> stringifiedTables = new ArrayList<List<List<String>>>();
        
        List<Span> spans = new ArrayList<Span>();
        for (WordCluster cluster : mergedClusters) {
            Span span = cluster.getSpan();
            spans.add(span);
        }

        int i=0;
        while(i<tables.size()) {
            List<List<WordPosition>> table = tables.get(i);
            if(table.size()<2) {
                tables.remove(i);
            }
            else {
                List<List<String>> stringifiedTable = new ArrayList<List<String>>();

                for(List<WordPosition> words: table) {
                    List<String> strings =  splitLineAccordingSpan(words,spans);
                    stringifiedTable.add(strings);
                }
                
                stringifiedTables.add(stringifiedTable);
                i++;
            }
        }
        return stringifiedTables;
        //System.out.println("$$$$$$$$$$$$$$$");
    }
    
    static int getNumAlignedWords(int[][] alignmentMatrix, int lineNb1, int lineNb2) {
            //ALIGNMENT CLUSTER
        int alignment1 = 0;
        int alignment2 = 0;
        int equalAlignment = 0;
        for(int i=0; i<alignmentMatrix.length;i++) {
                alignment1 += alignmentMatrix[i][lineNb1];
                alignment2 += alignmentMatrix[i][lineNb2];
                if(alignmentMatrix[i][lineNb1] == alignmentMatrix[i][lineNb2]) {
                    equalAlignment++;
            }
                
        }
        return equalAlignment;
    }
    static void printAccordingCluster(List<WordPosition> words, List<WordCluster> clusters) {

        for(int i=0; i<clusters.size(); i++) {
            for(WordPosition word: words) {
                int clusterIndex = word._clusterRef[3];
                if (clusterIndex == i) {
                    System.out.print(word.word());
                }
            }
            System.out.print("\t|");
        }
        System.out.println("");
    }
    
    static void printAccordingRightAlignedCluster(List<WordPosition> words, List<WordCluster> clusters) {
//        System.out.print(words.get(0).getLineNb() + "\t");
//
//        List<Span> spans = new ArrayList<Span>();
//        for (WordCluster cluster : clusters) {
//            Span span = cluster.getSpan();
//            spans.add(span);
//        }
//        printLineAccordingSpan(words,spans);
               System.out.print(words.get(0).getLineNb() + "\t|");

        int i = 0;
        for (WordCluster cluster : clusters) {
                while(i<words.size() && words.get(i).x2() <= cluster.getSpan().f2()) {
                    System.out.print(words.get(i).word() + " ");
                    i++;
                }
                System.out.print("\t|");
            
        }
        System.out.println();
    }
    
    static float absIncrease(float f1, float f2) {
        return Math.abs((f1-f2)/f1);
    }
    
    static float[] similarityLines(List<WordPosition> line1, int lineNb1, List<WordPosition> line2, int lineNb2, int[][] alignmentMatrix, float pageWidth) {

        //NUMBER OF WORDS
        int numWords1 = line1.size();
        int numWords2 = line2.size();
        float numWordsIncrease = absIncrease(numWords1,numWords2);
        
        //NUMBER OF WHITE SPANS
        List<Span> spans1 = extractWhiteSpans(line1);
        List<Span> spans2 = extractWhiteSpans(line2);
        int numWhiteSpans1 = spans1.size();
        int numWhiteSpans2 = spans2.size();
        float numWhiteSpansIncrease = absIncrease(numWhiteSpans1, numWhiteSpans2);

        //NUMBER OF COMMON WHITE SPANS
        float avgCharWidth = calcAverageCharWidth(line1);
        List<Span> commonSpans = intersectingSpans(spans1,spans2,avgCharWidth);
        float commonSpan1Increase = absIncrease(spans1.size(), commonSpans.size());
        float commonSpan2Increase = absIncrease(spans2.size(), commonSpans.size());
        float commonSpanIncrease = Math.max(commonSpan1Increase, commonSpan2Increase);

        //FONT SIMILARITY
        float fontSimilarity = 0f;
        String mainFont1 = getMainFont(line1);
        String mainFont2 = getMainFont(line2);
        if(mainFont1.equals(mainFont2)==true) {
            fontSimilarity = 1.0f;
        }
             
        //ALIGNMENT CLUSTER
        int alignment1 = 0;
        int alignment2 = 0;
        int equalAlignment = 0;
        for(int i=0; i<alignmentMatrix.length;i++) {
                alignment1 += alignmentMatrix[i][lineNb1];
                alignment2 += alignmentMatrix[i][lineNb2];
                if(alignmentMatrix[i][lineNb1] == alignmentMatrix[i][lineNb2]) {
                    equalAlignment++;
            }
        }
        float alignmentIncrease = absIncrease(alignment1, alignment2);
        float equalAlignmentPercentage = (float)equalAlignment / (float)alignmentMatrix.length;
        
        //WORDS IN PIXELS
        float wordsInPixels1 = getWordsInPixels(line1);
        float wordsInPixels2 = getWordsInPixels(line2);
        float wordsInPixelsIncrease = absIncrease(wordsInPixels1, wordsInPixels2);
 
        float[] features = new float[7];
        features[0] = numWordsIncrease;
        features[1] = numWhiteSpansIncrease;
        features[2] = commonSpanIncrease;
        features[3] = fontSimilarity;
        features[4] = alignmentIncrease;
        features[5] = equalAlignmentPercentage;
        features[6] = wordsInPixelsIncrease;
        
        return features;
    }
    
    static void normalizeSimilarityMatrix(float[][] similarityMatrix) {
        float[] max = new float[similarityMatrix[0].length];
        for(int feature=0; feature<similarityMatrix[0].length;feature++) {
            max[feature] = 0;
        }
        
        for(int i=0; i<similarityMatrix.length; i++) {
            float[] line = similarityMatrix[i];
            for(int feature=0; feature<line.length;feature++) {
                max[feature] = Math.max(max[feature],line[feature]);
            }
        }
        
        for(int i=0; i<similarityMatrix.length; i++) {
            float[] line = similarityMatrix[i];
            for(int feature=0; feature<line.length;feature++) {
                line[feature] /= max[feature];
            }
        } 
        
        //some features are "smaller is better" --> reverse those and make everything "bigger is better"
        for(int i=0; i<similarityMatrix.length; i++) {
            float[] line = similarityMatrix[i];
            line[0] = 1.0f - line[0];  //numWordsIncrease
            line[1] = 1.0f - line[1];  //numSpansIncrease
            line[2] = 1.0f - line[2];   //numCommonSpans
            line[4] = 1.0f - line[4];   //alignmentIncrease
            line[6] = 1.0f - line[6];   //numWordsInPixelsIncrease
        }       
    }
    
    static float[] calcRelativeSimilarities(float[][] similarityMatrix) {
        float[] similarities = new float[similarityMatrix.length];
        for(int i=0; i<similarities.length;i++) {
            similarities[i] = 0f;
        }
        
        float[] weights = new float[similarityMatrix[0].length];
        weights[0] = 0.04f; //numWordsIncrease;
        weights[1] = 0.05f; //numWhiteSpansIncrease;
        weights[2] = 0.15f; //commonSpanIncrease;
        weights[3] = 0.01f; //fontSimilarity;
        weights[4] = 0.30f; //alignmentIncrease
        weights[5] = 0.45f; //equalAlignmentPercentage;
        weights[6] = 0f;    //wordsInPixelsIncrease;
        
        for(int i=0; i<similarityMatrix.length; i++) {
            float[] line = similarityMatrix[i];
            for(int feature=0; feature<line.length;feature++) {
                similarities[i] += line[feature] * weights[feature];
            }
        }
        
        return similarities;
    }
    
    static float calcAverageCharWidth(List<WordPosition> words) {
        float totalLen = 0;
        //float totalWidth = 0f;
        for (WordPosition word : words) {
            //totalLen += word.word().length();
            //totalWidth += (word.x2() - word.x1());
            totalLen += word.getSpaceWidth();
        }
        return totalLen /= words.size();
    }
    
    static float getWordsInPixels(List<WordPosition> words) {
        float totalPixels = 0f;
        for(WordPosition word: words) {
            totalPixels += word.x2() - word.x1();
        }
        return totalPixels;
    }
    
    public static String getMainFont(List<WordPosition> words) {
        Map<String,Integer> fontOccurences = new HashMap<String,Integer>();
        for(WordPosition word: words) {
            String font = word.fontName() + "_" + Float.toString(word.fontSize());
            if(fontOccurences.containsKey(font)==false) {
                fontOccurences.put(font, 1);
            }
            else {
                int num = fontOccurences.get(font);
                fontOccurences.put(font, num+1);
            }
        }
        String result="";
        int maxNum = 0;
        for(Map.Entry<String,Integer> kv: fontOccurences.entrySet()) {
            int num = kv.getValue();
            if(num>maxNum) {
                result = kv.getKey();
                maxNum=num;
            }
        }
        return result;
    } 

}
