#coding:utf-8
import sys,re
from selenium import webdriver
import time
from selenium.webdriver.common.desired_capabilities import DesiredCapabilities


def save(filename, contents, path = './', state = 'w'): # 保存内容入本文
	filename = path + filename
	fh = open(filename,state)
	fh.write(contents)
	fh.close();

def getPage(url):
	try:
		driver.get(url)
		return driver
	except:
		print "can not get ",url

def ChromeInitial():
	options=webdriver.ChromeOptions()
	prefs={
		'profile.default_content_setting_values': {
			'images': 2
		}
	} #设置不加载图片
	options.add_experimental_option('prefs',prefs)
	driver = webdriver.Chrome(chrome_options=options)
	js="document.documentElement.scrollTop=10000" #chrome用这个
	return  driver, js # chrome 浏览器，用于可视化debug

def PhantomJSInitial():
	dcap = dict(DesiredCapabilities.PHANTOMJS)
	dcap["phantomjs.page.settings.userAgent"] = ("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.221 Safari/537.36 SE 2.X MetaSr 1.0") #设置user-agent请求头
	dcap["phantomjs.page.settings.loadImages"] = False #禁止加载图片
	 
	driver = webdriver.PhantomJS(executable_path=r'/Users/apple/phantomjs-2.1.1-macosx/bin/phantomjs',desired_capabilities=dcap) 
	driver.set_page_load_timeout(20) #设置页面最长加载时间为20s
	js="document.body.scrollTop=10000" #phantomjs 用这个，玄学。
	return driver, js # phantomjs非可视化浏览器，用于提高爬取速度



def getaurls(content,js,aurls):

	content.implicitly_wait(4)#设置超时时间为1s
	for i in range(1,1000): #设置查找
		content.execute_script(js)
		# getmore = content.find_element_by_class_name('c-getmore')
		# if getmore.text == "没有更多啦!" :
		# 	break
	feed_list_area = content.find_elements_by_class_name('feed-list-area')
	for fla in feed_list_area:
		feed_list = fla.find_elements_by_css_selector("[class='feed-item right-mode']")
		for fl in feed_list:
			try:
				item = fl.find_element_by_tag_name("a")
				aurl = item.get_attribute('href')
				print aurl
				aurls += aurl + '\n'
			except:
				print 'element not found'
				pass
	save("aurl.txt",aurls,"./data/", 'a')

def getcurls(content):
	navMenu = content.find_elements_by_id('navMenu')
	curls = navMenu[0].find_elements_by_tag_name('a')
	for item in curls:
		curl = item.get_attribute('href')
		curl = curl.split('?')[0]
		print curl


def getsubcurls(content,url):
	try:
		layer_box = content.find_element_by_class_name('layer-box').find_elements_by_tag_name('a')
		for lb in layer_box:
			subcurl = lb.get_attribute('href')
			print subcurl
	except:
		try:
			site = content.find_element_by_class_name('site').find_elements_by_tag_name('a')
			for s in site:
				subcurl = s.get_attribute('href')
				print subcurl
		except:
			print url," can not find"
			pass
# def geturls(content):
# 	content = content.page_source

# 	soup = BeautifulSoup(content,'html.parser')

# 	aurls, curls = '',''
# 	for a in soup.find_all('a'):
# 		if a.has_attr('href'):
# 			if 'f=' in a['href'] and '/ch/' in a['href']:
# 				print a['href']
# 				curls += 'https://m.sohu.com' + a['href'] + '\n'
# 			elif 'f=' in a['href'] and '/a/' in a['href']:
# 				print a['href']
# 				aurls += 'https://m.sohu.com'+ a['href'] + '\n'

# 	save("aurl.txt",aurls,"./data/", 'a')
# 	# save("curl.txt",curls,"./data/",'a')


if __name__ == '__main__':
	reload(sys)
	sys.setdefaultencoding('utf-8')

	#driver, js = ChromeInitial()
	driver, js = PhantomJSInitial()
	aurls =''
	
	# url = 'https://m.sohu.com'
	# content = getPage(url)
	# getcurls(content)
	# driver.quit()

	with open('./data/curl.txt') as f:
		for url in f:
			#print url
			content = getPage(url)
			#getsubcurls(content,url)
			getaurls(content,js,aurls)
	driver.quit()


	url = 'https://v2.sohu.com/public-api/feed?scene=CHANNEL&sceneId=15&page=1&size=20'







