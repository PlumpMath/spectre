from spectre.reader import AspectReader, S3RawReader


class Spectre(object):

    def __init__(self, bucket_name):
        self.bucket_name = bucket_name

    def new_reader(self, aspect_path, replay_id):
        return AspectReader( aspect_path, S3RawReader(self.bucket_name, aspect_path, replay_id) )
