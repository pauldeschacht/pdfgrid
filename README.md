pdfgrid
=======

This project is a playground to automate some of the data extraction.

The goal is to extract 'tabular' data from pdf files.
A lot of public data is still hidden within PDF reports. Although tools such as PDF2XL exist, I want to create an automated, command line driven application which extracts tabular data from PDF files.
The application is based on Apache's PDFBox to extract glyphs (characters) and their position.

There is no extact method to define lines and tabular data, therefore this is an ongoing process in which I test several ideas/methods to detect tabular data.
Initial methods such as line detection work well, but not all tables have lines. I want to create an application with no/little requirements on the PDF data.

Current method is based on alignment detection (left, center and right) of several consecutive lines, combined with positional clustering. 
This method gives good results except when the thousand separator is a space. 

1 000

    6
    
  756
  
2 345


In this case, the current method detects 2 columns (with empty values in the first column). 


