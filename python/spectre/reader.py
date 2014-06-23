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

class Reader(object):

    def __init__(self, aspect_path):
        self.aspect_path = aspect_path
        self.decoder = self.find_aspect_decoder()

    def __call__(self):
        return all()

    def all(self):
        pass

    def by_tick(self):
        pass
