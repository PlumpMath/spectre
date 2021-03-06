#!/usr/bin/python

import argparse

class AspectMapGenerator(object):
    def __init__(self, dst_path):
        self.dst_file = '{}/mapper.py'.format(dst_path)
        self.pkg_list = []
        self.msg_list = []

    def add_msg_type(self, cls_name, cls_id, path):
        cls_name = self.format_class(cls_name)
        pkg_pre = 'spectre.proto.aspect.' + path.split('/')[1].lower()
        pkg_name = pkg_pre + '.' + cls_name.split('.')[0]

        self.pkg_list.append(pkg_name)
        self.msg_list.append({'cls_name': cls_name, 'cls_id': cls_id, 'path': path})

    def save(self):
        with open(self.dst_file, 'w') as outfile:
            s  = '# Auto-generated by import_msgtypes.py\n'
            s += '# Do not edit\n\n'

            s += 'import sys\n'
            s += 'from os.path import abspath,dirname,join\n'
            s += '_cwd_ = abspath(dirname(__file__))\n'
            s += '_path_ = sys.path\n'
            s += 'sys.path.insert(0,join(_cwd_, \'proto\'))\n\n'

            for pkg in set(self.pkg_list): # no duplicates so use a set
                s += 'import {}_pb2 as {}\n'.format(pkg, pkg.split('.')[-1])
            s += '\n\n'

            s += 'sys.path = _path_\n\n'

            s += "aspect_map = {\n"
            for msg in self.msg_list:
                s += "    '{path}': {{'cls': {cls_name}, 'id': {cls_id} }},\n".format(**msg)
            s += "}"
            outfile.write(s)

    def format_class(self, cls_name):
        pre = cls_name.split('.')[0]
        new = ''+pre[0]
        for c in pre[1:]:
            if c.isupper():
                new += '_'+c.lower()
            else:
                new += c
        new = new.lower()

        post = cls_name.split('.')[1:]
        return new + '.' + '.'.join(post)


class AspectEnumGenerator(object):
    def __init__(self, dst_path):
        self.dst_file = '{}/aspect_enum.py'.format(dst_path)
        self.aspects = {}

    def add_msg_type(self, cls_name, cls_id, path):
        if len(path.strip()) == 0:
            return

        path_lst = self.lst_from_path(path)
        base = self.aspects
        for elm in path_lst:
            if elm not in base:
                base[elm] = {}
            last = base
            base = base[elm]
        last[elm] = path

    def save(self):
        with open(self.dst_file, 'w') as outfile:
            s  = '# Auto-generated by import_msgtypes.py\n'
            s += '# Do not edit\n\n'

            def traverse(s, pre, base, lvl):
                for k in base.keys():
                    nxt = base[k]

                    if lvl == 0:
                        seg = '{} = lambda:0\n'.format(k)
                    else:
                        if isinstance(nxt, str):
                            suff = " = '{}'".format(nxt)
                        else:
                            suff = ' = lambda:0'

                        seg = "{}.__dict__['{}']{}\n".format(pre, k, suff)
                    s += seg
                    if not isinstance(nxt, str):
                        s = traverse(s, seg.split('=')[0], nxt, lvl+1)
                return s
            s = traverse(s, '', self.aspects, 0)

            outfile.write(s)

    def lst_from_path(self, aspect_path):
        recs = aspect_path.split('/')[1:]
        recs = [''.join([c.capitalize() for c in s.split('_')]) for s in recs]
        recs[-1] = recs[-1].upper()
        return recs


def main(options):
    msgtypes_path = '{}/msgtypes.properties'.format(options.proto_path)
    print 'Importing {}'.format(msgtypes_path)

    map_gen = AspectMapGenerator(options.output_path)
    enum_gen = AspectEnumGenerator(options.output_path)

    with open(msgtypes_path) as infile:
        for line in (l.strip() for l in infile if l.strip() != ''):
            try:
                cls_name,_,cls_id,_,path = line.split()
                map_gen.add_msg_type(cls_name, cls_id, path)
                enum_gen.add_msg_type(cls_name, cls_id, path)
            except Exception as e:
                print 'Skipping "{}"\n{}'.format(line, e)

    map_gen.save()
    enum_gen.save()


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('proto_path')
    parser.add_argument('output_path')
    options = parser.parse_args()

    main(options)
