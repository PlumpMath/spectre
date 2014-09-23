require './reader'

class Spectre
    def initialize(bucket_name)
        @bucket_name = bucket_name
    end

    def new_reader(aspect_path, replay_id)
        return AspectReader.new(aspect_path, S3RawReader.new(@bucket_name, aspect_path, replay_id))
    end
end
