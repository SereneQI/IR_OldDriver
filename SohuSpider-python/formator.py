# coding:utf-8
import sys,re,os
import json
import pickle

reload(sys)
sys.setdefaultencoding('utf-8')

def loadfile(filename):
	filename = '/Users/apple/Desktop/IR/data/' + filename
	with open(filename) as f:
		data = json.load(f)
	return data	

def storefile(filename,data):
	filename = '/Users/apple/Desktop/IR/data/' + filename
	with open(filename,'w') as f:
		f.write(json.dumps(data, encoding='utf-8', ensure_ascii=False))


def format():
	i = 0
	filelist = os.listdir('/Users/apple/Desktop/IR/data')
	for filename in filelist:
		if filename not in fileFilter:
			fileFilter.add(filename)
			data = loadfile(filename)
			if data['cmtlist'] == '':
				data['cmtlist'] = []
				storefile(filename,data)
				print i,' ',filename,' updated with cmts: ',data['cmtnum']
			i += 1
		else:
			print 'already examed'

if __name__ == '__main__':
	
	if os.path.exists('formatFilter.pkl') == True:	
		pklfile = open('formatFilter.pkl','rb')
		fileFilter = pickle.load(pklfile)
	else:
		fileFilter = set()

	try:
		format()
	except Exception,e: #当发生中断时，保存变量
		print '遇到异常',e
		filefp = open('formatFilter.pkl','wb')
		pickle.dump(fileFilter,filefp)
		filefp.close()
		print '成功退出'





