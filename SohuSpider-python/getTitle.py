# coding:utf-8
import sys,re,os
import json
import pickle

reload(sys)
sys.setdefaultencoding('utf-8')

def loadfile(filename):
	# filename = '/Users/apple/Desktop/IR/data/' + filename
	filename = filepath + filename
	with open(filename) as f:
		data = json.load(f)
	return data	

def storefile(filename,data):
	# filename = '/Users/apple/Desktop/IR/data/' + filename
	filename = filepath + filename
	with open(filename,'w') as f:
		f.write(json.dumps(data, encoding='utf-8', ensure_ascii=False))
def save(filename,data):
	with open(filename,'wa') as f:
		f.write(data)

def getTitle():
	i = 0;
	title = ''
	#filelist = os.listdir('/Users/apple/Desktop/IR/data')
	filelist = os.listdir(filepath)
	for filename in filelist:
		if filename not in fileFilter:
			fileFilter.add(filename)
			data = loadfile(filename)
			title += data['title'] + '\n'
			storefile(filename,data)
			print i,' ',filename,' updated'# with cmts: ',data['cmtnum'],' type:',type(data['cmtnum'])
		else:
			print 'already changed'
		i += 1
	save("AllTitles.txt",title)

if __name__ == '__main__':

	global filepath#, title
	filepath = sys.argv[1]
	print filepath

	if os.path.exists('formatFilter.pkl') == True:	
		pklfile = open('formatFilter.pkl','rb')
		fileFilter = pickle.load(pklfile)
	else:
		fileFilter = set()

	try:
		getTitle()
	except Exception,e: #当发生中断时，保存变量
		print '遇到异常',e
		filefp = open('formatFilter.pkl','wb')
		pickle.dump(fileFilter,filefp)
		filefp.close()

		titlefp = open('alltitle.pkl','wb')
		pickle.dump(title,titlefp)
		titlefp.close()
		print len(title)
		#save("AllTitles.txt",title)
		print '成功退出'





