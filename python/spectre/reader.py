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
import types
from cStringIO import StringIO
from gzip import GzipFile
from boto.s3.connection import S3Connection

import spectre.mapper as mapper
import spectre.err as err
import spectre.proto.internal.fs_pb2 as fs_pb2
from spectre.util import read_varint

log = logging.getLogger(__name__)

MSG_TICK     = 0
MSG_STRINGS  = 1000
MAX_MSG_SIZE = 4096
MAX_STABLE_SIZE = 32*1024*1024


class AspectReader(object):

    def __init__(self, api_key, aspect_paths, replay_query):
        self._replays = None
        self._api_key = api_key

        if isinstance(aspect_paths, list):
            self._aspect_paths = aspect_paths
        elif isinstance(aspect_paths, str):
            self._aspect_paths = list(aspect_paths)
        else:
            raise RuntimeException("Invalid type for argument aspect_paths: {}".format(type(aspect_paths)))

        if isinstance(replay_query, list):
            self._replay_query = {'mid': ','.join(replay_query)}
        elif isinstance(replay_query, string):
            self._replay_query = {'mid': replay_query}
        elif isinstance(replay_query, int):
            self._replay_query = {'mid': replay_query}
        elif isinstance(replay_query, dict):
            self._replay_query = replay_query
        else:
            raise RuntimeException("Invalid type for argument replay_query: {}".format(type(replay_query)))


    @property
    def replay(self):
        return self.replays[0]

    @property
    def replays(self):
        if self._replays is None:
            self._replays = self._fetch_aspects()
        return self._replays

    
    def _fetch_aspects(self):
        urls = 


class OldAspectReader(object):

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
        return all()

    def all(self):
        while self._has_next():
            yield self._read_msg()

    def by_tick(self):
        current_tick = self.tick
        msg_list = []

        while self._has_next():
            next_msg = self._read_msg()

            if self.tick == current_tick:
                msg_list.append(next_msg)
            else:
                yield msg_list
                current_tick = self.tick
                msg_list = []

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
            return msg


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


def Range(start, end):
    return {'Range': 'bytes={}-{}'.format(start, end)}

def set_module_loglevel(lvl):
    logging.basicConfig()
    log.setLevel(lvl)
