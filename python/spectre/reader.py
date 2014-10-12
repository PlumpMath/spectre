# AspectReader accepts an aspect path and returns an object
# that will iterate over the messages. The iterator returns
# a dictionary containing the message contents.
# AspectReader itself can also be called like a function
# to return an iterator over all messages.
#
# for msg in reader():
#     pass
#
# for msg in reader.all():
#     pass
#
# for tick in reader.by_tick():
#     for msg in tick:
#         pass
#

import struct
import logging
import inspect
from collections import defaultdict
from cStringIO import StringIO
from gzip import GzipFile
from urllib2 import urlopen
from boto.s3.connection import S3Connection

import courier
import mapper
import err
import proto.internal.fs_pb2 as fs_pb2
from util import read_varint

log = logging.getLogger(__name__)

MSG_TICK     = 0
MSG_STRINGS  = 1000
MAX_MSG_SIZE = 4096
MAX_STABLE_SIZE = 32*1024*1024


class AspectReader(object):

    def __init__(self, api_key, aspect_paths, replay_query):
        self._replays = None

        if isinstance(aspect_paths, str):
            self._aspect_paths = [aspect_paths]
        elif isinstance(aspect_paths, list):
            self._aspect_paths = aspect_paths
        else:
            raise RuntimeException("Invalid type for argument aspect_paths: {}".format(type(aspect_paths)))

        if isinstance(replay_query, list):
            self._replay_query = {'mid': ','.join(replay_query)}
        elif isinstance(replay_query, str):
            self._replay_query = {'mid': replay_query}
        elif isinstance(replay_query, int):
            self._replay_query = {'mid': replay_query}
        elif isinstance(replay_query, dict):
            self._replay_query = replay_query
        else:
            raise RuntimeException("Invalid type for argument replay_query: {}".format(type(replay_query)))

        # add api key to query
        self._replay_query['key'] = api_key


    @property
    def replay(self):
        return self.replays[0]

    @property
    def replays(self):
        if self._replays is None:
            self._replays = self._fetch_aspects()
        return self._replays

    
    def _fetch_aspects(self):
        # fetch and parse aspects and put them in to a list grouped by match id
        aspects = defaultdict(list)
        for aspect in self._aspect_paths:
            urls = courier.request_aspect_urls(aspect, self._replay_query)
            for mid,url in urls:
                psr = AspectParser(aspect, HttpRawReader(url))
                aspects[mid].append(psr)

        # load the aspects in to a Replay collection object
        return [Replay(mid,aspects) for (mid,aspects) in aspects.items()]


class AspectParser(object):

    def __init__(self, aspect_path, raw_reader):
        try:
            self.msg_cls = mapper.aspect_map[aspect_path]['cls']
            self.msg_id  = mapper.aspect_map[aspect_path]['id']
        except KeyError as e:
            raise err.UnknownAspectPath(aspect_path)

        self.raw = raw_reader

        self.aspect_path = aspect_path
        self.string_table = self.raw.read_string_table()
        self.tick = 0

        self.istream = self.raw.get_stream()
        try:
            self.replay_id = read_varint(self.istream)
        except Exception as e:
            raise err.MalformedAspect(e)

    def lookup_string(self, st_idx):
        return self.string_table[st_idx]

    def __call__(self):
        return self.all()

    def all(self):
        while self._has_next():
            yield self._read_msg()

    def by_tick(self):
        current_tick = self.tick
        msg_list = []

        while self._has_next(): # responsible for increasing self.tick
            next_msg = self._read_msg()

            if self.tick == current_tick:
                msg_list.append(next_msg)
            else:
                yield msg_list
                current_tick = self.tick
                msg_list = [next_msg]

    def _has_next(self):
        try:
            while True:
                pos = self.istream.tell()
                msg_type = read_varint(self.istream)

                if   msg_type == MSG_TICK:
                    self.tick = read_varint(self.istream)
                elif msg_type == MSG_STRINGS:
                    return False # at end of file
                elif msg_type == self.msg_id:
                    self.istream.seek(pos)
                    return True
                else:
                    raise MalformedAspect('Invalid msg_type:{}, expected [{},{},{}]'.format(msg_type, self.msg_id, MSG_TICK, MSG_STRINGS))

        except Exception as e:
            log.warning('Exception: {}'.format(e))
            return False

    def _read_msg(self):
        msg_type = read_varint(self.istream)
        if msg_type == MSG_TICK or msg_type == MSG_STRINGS:
            raise Exception('_read_msg called for INTERNAL msg_type:{}'.format(msg_type))

        msg_size = read_varint(self.istream)
        if msg_size <= MAX_MSG_SIZE:
            msg = self.msg_cls()
            msg.ParseFromString(self.istream.read(msg_size))
            return Msg(msg, self.lookup_string)


class HttpRawReader(object):

    def __init__(self, url):
        self._rawdata = None
        self._url = url
        try:
            self._replay_id = url.split('?')[0].split('/')[-1]
        except:
            self._replay_id  = '<unparsable>'


    def _load_aspect_data(self):
        if self._rawdata is None:
            log.debug('Downloading {} to memory'.format(self._replay_id))
            rsp = urlopen(self._url)
            rcode = rsp.getcode()
            if rcode == 200:
                self._rawdata = rsp.read()
            else:
                raise err.NetworkError('HTTP {}: {}\n{}'.format(rcode, rsp.read(), self._url))


    def get_stream(self):
        self._load_aspect_data()
        return GzipFile(fileobj=StringIO(self._rawdata))


    def read_string_table(self):
        self._load_aspect_data()

        # read last 4 bytes (string table offset)
        offset_val = struct.unpack_from('>L', self._rawdata, len(self._rawdata)-4)[0]
        log.debug('Offset value:{}'.format(offset_val))
        if offset_val > len(self._rawdata) or offset_val < 0:
            raise err.MalformedAspect('StringTable corrupted: offset invalid:{}'.format(offset_val))

        # grab section of memory containing the string table
        st_sz = offset_val-4
        start = len(self._rawdata)-offset_val
        end = start+st_sz-1
        if st_sz > MAX_STABLE_SIZE:
            raise err.MalformedAspect('StringTable size:{} > {}'.format(st_sz, MAX_STABLE_SIZE))
        log.debug('Reading StringTable data {}-{}'.format(start, end))
        st_obj = StringIO(self._rawdata[start:end+1])

        # read protobuf data from gzip'd buffer
        str_table = fs_pb2.StringTable()
        try:
            str_table.ParseFromString(GzipFile(fileobj=st_obj).read())
        except Exception as e:
            raise err.MalformedAspect('Error parsing StringTable: {}'.format(e))
        log.debug('strTable.length:{}'.format(len(str_table.value)))
        for i,_ in enumerate(str_table.value):
            log.debug('\t{}: {}'.format(i, str_table.value[i]))

        return str_table.value


class S3RawReader(object):

    def __init__(self, bucket_name, aspect_path, replay_id):
        self.bkt = S3Connection().get_bucket(bucket_name)
        self.key = self.bkt.get_key('{}/{}'.format(aspect_path, replay_id))
        if self.key is None:
            raise err.AspectInstanceNotFound('{}/{}'.format(aspect_path,replay_id))
        log.info('{}, Size: {}'.format(self.key.name, self.key.size))

        self.replay_id = None
        self.istream = None

    def get_stream(self):
        log.debug('Downloading {} to memory'.format(self.key.name))
        data = self.key.get_contents_as_string()
        log.debug('Download done')
        return GzipFile(fileobj=StringIO(data))

    def read_string_table(self):

        # download last 4 bytes (string table offset)
        offset_obj = self.key.get_contents_as_string(Range(self.key.size-4,self.key.size-1))
        log.debug('length:{}'.format(len(offset_obj)))
        log.debug('offset:{}'.format([hex(ord(c)) for c in offset_obj]))
        offset_val = struct.unpack('>L', offset_obj)[0]
        log.debug('Offset value:{}'.format(offset_val))
        if offset_val > self.key.size or offset_val < 0:
            raise err.MalformedAspect('StringTable corrupted: offset invalid:{}'.format(offset_val))

        # download gzip'd string table in to memory
        st_sz = offset_val-4
        start = self.key.size-offset_val
        end = start+st_sz-1
        if st_sz > MAX_STABLE_SIZE:
            raise err.MalformedAspect('StringTable size:{} > {}'.format(st_sz, MAX_STABLE_SIZE))
        log.debug('Reading StringTable data {}-{}'.format(start, end))
        st_obj = StringIO(self.key.get_contents_as_string(Range(start, end)))

        # read protobuf data from gzip'd buffer
        str_table = fs_pb2.StringTable()
        str_table.ParseFromString(GzipFile(fileobj=st_obj).read())
        log.debug('strTable.length:{}'.format(len(str_table.value)))
        for i,_ in enumerate(str_table.value):
            log.debug('\t{}: {}'.format(i, str_table.value[i]))

        return str_table.value


class Replay(object):
    """ Collection of aspects that are associated with a single match id """

    def __init__(self, matchid, aspect_list):
        self._psrlist = aspect_list
        self._psrdict = {p.aspect_path:p for p in aspect_list}

        self.match_id = matchid
        self.tick = 0


    def by_tick(self):
        # list of generators
        agens = [(psr.aspect_path,psr.by_tick()) for psr in self._psrlist] 

        # init starting state
        cur_vals  = {}
        for psr in self._psrlist:
            cur_vals[psr.aspect_path] = None
        next_vals = {}
        for asp,gen in agens:
            try:
                next_vals[asp] = gen.next()
            except StopIteration:
                next_vals[asp] = None
        agens = [(asp,gen) for (asp,gen) in agens if next_vals[asp] is not None]

        while len(agens) > 0:
            self.tick = min([p.tick for p in self._psrlist])
            for asp,gen in agens:
                if self._psrdict[asp].tick == self.tick:
                    cur_vals[asp] = next_vals[asp]
                    try:
                        next_vals[asp] = gen.next()
                    except StopIteration:
                        next_vals[asp] = None
            agens = [(asp,gen) for (asp,gen) in agens if next_vals[asp] is not None]
            yield cur_vals


def Range(start, end):
    return {'Range': 'bytes={}-{}'.format(start, end)}

def set_module_loglevel(lvl):
    logging.basicConfig()
    log.setLevel(lvl)


class Msg(object):
    """ Class to "wrap" pb message. 
        Resolves all string lookups on instantiation """
    def __init__(self, pb_msg, lookup):
        for mbr in inspect.getmembers(pb_msg, not inspect.ismethod and not inspect.isfunction):
            mname, mval = mbr
            if not mname.startswith('_'):
                setattr(self, mname, mval)
                if mname.endswith('_idx'):
                    setattr(self, mname.strip('_idx'), lookup(mval))
