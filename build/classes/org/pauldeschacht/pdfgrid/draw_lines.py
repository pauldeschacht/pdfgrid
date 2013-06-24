from Tkinter import *

master=Tk()
w = Canvas(master, width = 800, height = 600)
w.pack()

f = open("lines.txt")
lines = f.readlines()
for line in lines:
    if line.startswith("Line:"):
       items = line.split(':');
       coords = items[1].split(',');
       x1 = int(coords[0].strip())
       y1 = int(coords[1].strip())
       x2 = int(coords[2].strip())
       y2 = int(coords[3].strip())
       w.create_line(x1,y1,x2,y2)
       
