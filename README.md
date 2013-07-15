pdfgrid
=======

This project is a playground to automate some of the data extraction.

The goal is to extract 'tabular' data from pdf files.
A lot of public data is still hidden within PDF reports. Although tools such as PDF2XL exist, I want to create an automated, command line driven application which extracts tabular data from PDF files.
The application is based on Apache's PDFBox to extract glyphs (characters) and their position. Another possibility would be to used Mozilla's pdf.js to extract that information.

There is no extact method to define lines and tabular data, therefore this is an ongoing process in which I test several ideas/methods to detect tabular data.
Initial methods such as line detection work well, but not all tables have lines. I want to create an application with no/little requirements on the PDF data.

Current method is based on alignment detection (left, center and right) of several consecutive lines, combined with positional clustering. 
This method gives good results except whith aligned numbers and space as the thousand separator. 

1\_000

\_\_\_\_6

\_\_756

2\_345


In this case, the current method detects 2 different columns. Additional information is required to determine whether the 1 belongs to the number 1000.





