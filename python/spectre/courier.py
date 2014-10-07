from urllib import urlencode
from urllib2 import urlopen

BASE_URL = 'https://spectre.skadistats.org/api/v1'

def request_aspect_urls(aspects, query):
    construct_url = lambda aspect:'/'.join([BASE_URL, aspect, '?'+urlencode(query)])
    courier_urls = [construct_url(aspect) for aspect in aspects]
    print 'courier: {}'.format(courier_urls)

    for url in courier_urls:
        resp = urlopen(url)
        code = resp.getcode()
        if code == 503:
