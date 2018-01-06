import pickle 
import collections



urlQ = collections.deque()
with open('/Users/apple/Desktop/IR/data/aurl.txt') as f:
	for url in f:
		url = url[:-1]
		urlQ.append(url)

with open('/Users/apple/Desktop/IR/data/curl.txt') as f:
	for url in f:
		url = url[:-1]
		urlQ.append(url)

urlp = open('urlQ.pkl','wb')
pickle.dump(urlQ,urlp)
urlp.close()
