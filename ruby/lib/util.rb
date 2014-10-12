VI_MAX_BYTES = 5
VI_SHIFT = 7
VI_MASK = (1 << 32) - 1

def read_varint(io)
    size = 0
    value = 0
    shift = 0

    loop do
        byte = io.getbyte

        if (byte == nil)
            raise EOFError, "end-of-file encountered while decoding varint"
        end

        size += 1
        value |= (byte & 0x7f) << shift
        shift += VI_SHIFT

        if (byte & 0x80) == 0
            return value & VI_MASK
        end

        if (shift >= VI_SHIFT * VI_MAX_BYTES)
            raise RuntimeError, "too many bytes when decoding varint"
        end
    end
end