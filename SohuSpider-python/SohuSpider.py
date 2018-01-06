#coding:utf-8
import sys,re,os
from selenium import webdriver
from selenium.webdriver.common.desired_capabilities import DesiredCapabilities
import json
import collections
import pickle
import time
import signal

reload(sys)
sys.setdefaultencoding('utf-8')

def save(filename, contents, path = './'): # 保存内容入本文
	filename = path + filename
	fh = open(filename,'w')
	fh.write(contents)
	fh.close();

def getPage(url):
	try:
		driver.get(url)
		return driver
	except:
		print "can not get ",url
		return None
def savePage(content,url):
	content = content.page_source
	newsid = re.findall(r"\d+_\d+",url)[0]
	filename = newsid+"_cmt.txt"
	save(filename,content)

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

def PhantomJSInitial():
	dcap = dict(DesiredCapabilities.PHANTOMJS)
	dcap["phantomjs.page.settings.userAgent"] = ("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.221 Safari/537.36 SE 2.X MetaSr 1.0") #设置user-agent请求头
	dcap["phantomjs.page.settings.loadImages"] = False #禁止加载图片
	 
	driver = webdriver.PhantomJS(executable_path=r'/Users/apple/phantomjs-2.1.1-macosx/bin/phantomjs',desired_capabilities=dcap) 
	driver.set_page_load_timeout(5) #设置页面最长加载时间为5s
	driver.set_script_timeout(5) # 设置5s脚本超时
	js="document.body.scrollTop=10000" #phantomjs 用这个，玄学。
	return driver, js # phantomjs非可视化浏览器，用于提高爬取速度

def extract(i, url):
	pat = "(.*/a/[0-9]+_[0-9]+.*)"
	if re.search(pat,url): #表示该url是新闻网页
		newsid = re.findall(r"(\d+_\d+)",url)[0]
		if os.path.exists('../../data/'+ newsid + '.json') == True:
			print newsid,' 文件已存在，跳过'
			urlFilter.add(url)
			return i
		content = getPage(url)
		if content == None:
			return i
		try:
			title =content.find_element_by_class_name('title-info').text
			date = content.find_element_by_class_name('time').text
			dispcont = content.find_element_by_class_name('display-content').text
			cmtnum = content.find_element_by_class_name('c-comments-num').text
			author =content.find_element_by_class_name('name').text
			newstype = content.find_element_by_class_name('back-sub').text

			date ='{}-{} {}'.format('2017',date.split(' ')[0],date.split(' ')[1])# 格式化时间
			timearray = time.strptime(date, "%Y-%m-%d %H:%M")
			timestamp = int(time.mktime(timearray))
			urlFilter.add(url) # 表明该网页抓取成功，放入到Filter中
			newsdata ={'newsid':newsid, 'newsurl':url, 'author':author, 'title':title, 'type':newstype,'ntime':timestamp, 'content':dispcont,'ori':"搜狐新闻", 'cmtnum':"", 'cmtlist':""}
			newsjson = json.dumps(newsdata, encoding='utf-8', ensure_ascii=False)
			filename = newsid + ".json"
			save(filename,newsjson, '../../data/')
			print i, ' 爬取 ',url,' 成功,',

			tagas = content.find_elements_by_tag_name("a")
			pat2 = "(.*/a/[0-9]+_[0-9]+.*)|(.*/n/[0-9]+.*)"
			addnum = 0
			for a in tagas:
				try:
					urla = a.get_attribute('href')
					find = re.search(pat2,urla)
					if find:
						urla = find.group()
						if "m.sohu.com" not in urla:
							urla= "http://m.sohu.com" + urla
						urlQ.append(urla)
						addnum += 1
				except:
					continue
			print '加入',addnum,'条url'
			return i+1
		except :
			print "抓取不到内容"
			return i
	else:
		return i

def spider():
	i = 0
	while urlQ:
		if is_sigint:
			quit()
			break
		url = urlQ.popleft()
		if url not in urlFilter: # 如果用户已经被抓取过，跳过
			i = extract(i,url)
		else:
			print '该url已经爬过，跳过'
	print 'url 队列为0，停止爬取'


def sig_handler(signum,frame):
	global is_sigint
	is_sigint = True
	print '捕捉到终止信号',

def quit():
	print '退出程序'
	try:
		urlp = open('urlQ.pkl','wb')
		urlfp = open('urlFilter.pkl','wb')
		pickle.dump(urlQ,urlp)
		pickle.dump(urlFilter,urlfp)
		urlp.close()
		urlfp.close()
		print '退出成功'
	except Exception, e:
		print e


if __name__ == '__main__':
	
	if os.path.exists('urlQ.pkl') == True:
		pklfile = open('urlQ.pkl','rb')
		urlQ = pickle.load(pklfile)
		print '程序启动，len(urls) = ', len(urlQ)
	else:
		print 'urls 不存在，请重试'
		# urlQ = collections.deque()
		# #导航页面
		# url = "https://m.sohu.com/c/395/?_once_=000025_zhitongche_daohang_v3" 
		# urlQ.append(url)
	if os.path.exists('urlFilter.pkl') == True:	
		pklfile = open('urlFilter.pkl','rb')
		urlFilter = pickle.load(pklfile)
	else:
		urlFilter = set()

	signal.signal(signal.SIGINT, sig_handler)
	signal.signal(signal.SIGTERM,sig_handler)
	is_sigint=False

	try:
		driver, js = ChromeInitial()
		# driver, js = PhantomJSInitial()
		spider()
	except : #当发生中断时，保存变量
		print '遇到异常',
		quit()
	driver.quit()
	
	'''
	i = 0
	with open('./data/aurl.txt') as f:
		for url in f:
			url = url[:-1]

			if not 'https' in url:
				continue 
			newsid = re.findall(r"\d+_\d+",url)[0]
			if os.path.exists('./data/'+ newsid+ '.json') == True: # 如果用户已经被抓取过，跳过
				print "webpage", newsid ,"already exists"
				continue
			content = getPage(url)
			extract(content, js, newsid, url)
			i += 1; print i,"page done"
	'''
	# driver.quit()










