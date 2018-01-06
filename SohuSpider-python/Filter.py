# coding:utf-8
from sys import argv

def clean(filename):#用户 url 去重
# 用于对文件中的用户名去重
# :para filename 存储所有用户名的文件
	file = open(filename,"r")
	urls = file.read()
	file.close()
	urls = urls.split('\n')
	print len(urls)
	urls = list(set(urls))
	print len(urls)

	newurl = ''
	for url in urls:
		newurl += url + '\n'
	fh = open(filename,'w') #写
	fh.write(newurl)
	fh.close();

clean(argv[1])
