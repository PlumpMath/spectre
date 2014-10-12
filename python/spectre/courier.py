from __future__ import print_function

import sys
import json
from urllib import urlencode
from urllib2 import urlopen

BASE_URL = 'http://spectre.skadistats.org/api/v1/aspect'


def request_aspect_urls(aspect, query):
    url = '/'.join([BASE_URL, aspect.strip('/'), '?'+urlencode(query)])

    aspect_urls = []
    resp = urlopen(url)
    code = resp.getcode()
    if code == 200:
        try:
            body = resp.read()
            r = json.loads(body)
            aspect_urls = r['urls']
        except Exception as e:
            print("Error parsing response:", e, '\n', body, file=sys.stderr)
    elif code == 400:
        print("HTTP400:", resp.read(), file=sys.stderr)
    elif code == 503:
        print("HTTP503:", resp.read(), file=sys.stderr)

    # make pairs of (match_id, url) my extracting match id from url
    return [(int(url.split('?')[0].split('/')[-1]), url) for url in aspect_urls]
