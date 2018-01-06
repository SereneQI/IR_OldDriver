#coding:utf-8
from sys import argv

def format(filename):
	file = open(filename,'r')
	formatline = ''
	print "running"
	while 1:
		line = file.readline()
		if not line:
			break
		if "https://m.sohu.com/a/" not in line:
			continue
		line = line.split('?')[0]
		if line[-1] == '\n':
			line = line[:-1]
		if line[-1] != '/':
			line = line + '/'
		print line
		formatline += line + '\n'
	file.close()

	fh = open(filename,'w')
	fh.write(formatline)
	fh.close()


format(argv[1])