require 'zlib'
require 'aws-sdk'

require_relative './util'
require_relative './proto/internal/fs.pb'
require_relative './mapper'

MSG_TICK     = 0
MSG_STRINGS  = 1000
MAX_MSG_SIZE = 4096
MAX_STABLE_SIZE = 32*1024*1024

class AspectReader
	def initialize(aspect_path, raw_reader)
		unless AspectMapper::MAPPER.include?(aspect_path)
			raise KeyError, 'no aspect mapping for #{aspect_path}'
		end
		@msg_cls = AspectMapper::MAPPER[aspect_path]['cls']
		@msg_id = AspectMapper::MAPPER[aspect_path]['id']
		@raw = raw_reader
		@aspect_path = aspect_path
		@tick = 0
		@istream = @raw.get_stream
		@replay_id = read_varint(@istream)
	end

	def each_msg(&block)
		while _has_next?
			msg = _read_msg
			if block_given?
				block.call msg
			else
				yield msg
			end
		end
	end

	def each_tick(&block)
		current_tick = @tick
		msg_list = Array.new

		while _has_next?
			next_msg = _read_msg

			if @tick == current_tick
				msg_list.push(next_msg)
			else
				if block_given?
					block.call msg_list
				else
					yield msg_list
				end
				current_tick = @tick
				msg_list = Array.new
			end
		end
	end

	def _has_next?()
		loop do
			pos = @istream.tell
			msg_type = read_varint(@istream)

			if msg_type == MSG_TICK
				@tick = read_varint(@istream)
			elsif msg_type == MSG_STRINGS
				return false
			elsif msg_type == @msg_id
				@istream.rewind
				@istream.read(pos)
				return true
			else
				raise RuntimeError, "Invalid msg_type:#{msg_type}, expected [#{@msg_id}, #{MSG_TICK}, #{MSG_STRINGS}]"
			end
		end
	end

	def _read_msg()
		msg_type = read_varint(@istream)
		if (msg_type == MSG_TICK) || (msg_type == MSG_STRINGS)
			raise RuntimeError, "_read_msg called for INTERNAL msg_type:#{msg_type}"
		end

		msg_size = read_varint(@istream)
		unless msg_size < MAX_MSG_SIZE
			raise RuntimeError, "_read_msg found a message of size #{msg_size} (max should be #{MAX_MSG_SIZE})"
		end
		msg = @msg_cls.new()
		msg.parse_from_string(@istream.read(msg_size))
		msg
	end
end

class S3RawReader
	def initialize(bucket_name, aspect_path, replay_id)
		@bkt = AWS::S3.new.buckets[bucket_name]
		# Ruby version of AWS::S3 does not like the initial forward-slash
		key_string = "#{aspect_path}/#{replay_id}".sub(/^\//, '')
		@key = @bkt.objects[key_string]
		unless @key.exists?
			raise KeyError, "aspect instance #{key_string} not found"
		end
	end

	def get_stream
		return Zlib::GzipReader.new(StringIO.new(@key.read))
	end

	def read_string_table
	end
end
