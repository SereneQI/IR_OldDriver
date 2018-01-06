# coding:utf-8
import sys,re,os
from selenium import webdriver
from selenium.webdriver.common.desired_capabilities import DesiredCapabilities
import json
import time
import signal
import pickle

reload(sys)
sys.setdefaultencoding('utf-8')

def getPage(url):
	try:
		driver.get(url)
		return driver
	except:
		print "can not get ",url
		return None

def ChromeInitial():
	options=webdriver.ChromeOptions()
	prefs={
		'profile.default_content_setting_values': {
			'images': 2
		}
	} #设置不加载图片
	options.add_experimental_option('prefs',prefs)
	driver = webdriver.Chrome(chrome_options=options)
	driver.set_page_load_timeout(10) #设置页面最长加载时间为5s
	driver.set_script_timeout(10) # 设置5s脚本超时
	js="document.documentElement.scrollTop=10000" #chrome用这个
	return  driver, js # chrome 浏览器，用于可视化debug

def loadfile(filename):
	filename = '/Users/apple/Desktop/IR/data/' + filename
	with open(filename) as f:
		data = json.load(f)
	return data	

def storefile(filename,data):
	filename = '/Users/apple/Desktop/IR/data/' + filename
	with open(filename,'w') as f:
		f.write(json.dumps(data, encoding='utf-8', ensure_ascii=False))


def sig_handler(signum,frame):
	global is_sigint
	is_sigint = True
	print '捕捉到终止信号',

def quit():
	print '退出程序'
	try:
		filefp = open('fileFilter.pkl','wb')
		pickle.dump(fileFilter,filefp)
		filefp.close()
		print '退出成功'
	except Exception, e:
		print e

def getcmts(url,js,cmtnumOld,cmtlistOld):
	cmturl = 'http://m.sohu.com/cm/' + url.split('/')[-1]
	content = getPage(cmturl)
	try:
		cmtnum = int(content.find_element_by_class_name('c-comments-num').text)
	except:
		cmtnum = 0

	if cmtnum == 0 :
		return cmtnum,[]
	if cmtnum == cmtnumOld :
		return cmtnumOld,cmtlistOld
	else:
		for i in range(1,100): #设置查找
			content.execute_script(js)
			try:
				getmore = content.find_element_by_class_name('c-getmore')
				if getmore.text == "没有更多啦!" :
					break
			except:
				continue

		cmt_all = []
		try:
			cmtitems = content.find_elements_by_class_name('c-comment-item')
			for item in cmtitems:
				cmter = item.find_element_by_class_name('c-username').text
				cmt = item.find_elements_by_class_name('c-discuss')[-1].text #这里只保留最后一个，因为嵌套评论中该用户的评论在最后
				cmttime = item.find_element_by_class_name('c-item-date').text
				comment = {'cmter':cmter, 'cmt': cmt, 'cmttime':cmttime}
				cmt_all.append(comment)
			return cmtnum,cmt_all
		except:
			print "can not find comment"
			return 0,[]


def spider():
	i = 0
	filelist = os.listdir('/Users/apple/Desktop/IR/data')
	for filename in filelist:
		if is_sigint:
			quit()
			break
		if filename not in fileFilter:
			fileFilter.add(filename)
			data = loadfile(filename)
			url = data['newsurl']
			cmtnumOld = data['cmtnum']
			cmtlistOld = data['cmtlist']
			data['cmtnum'],data['cmtlist'] = getcmts(url,js,cmtnumOld,cmtlistOld)
			storefile(filename,data)
			print i,' ',filename,' updated with cmts: ',data['cmtnum']
		else:
			print 'file already update'
		i += 1

if __name__ == '__main__':
	if os.path.exists('fileFilter.pkl') == True:	
		pklfile = open('fileFilter.pkl','rb')
		fileFilter = pickle.load(pklfile)
	else:
		fileFilter = set()

	signal.signal(signal.SIGINT, sig_handler)
	signal.signal(signal.SIGTERM,sig_handler)
	is_sigint=False

	try:
		driver, js = ChromeInitial()
		spider()
	except Exception,e: #当发生中断时，保存变量
		print '遇到异常',e
		quit()
	driver.quit()



