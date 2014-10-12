class MalformedAspect(Exception):
    def __init__(self, msg): 
        super(MalformedAspect, self).__init__(msg)

class UnknownAspectPath(Exception):
    def __init__(self, msg): 
        super(UnknownAspectPath, self).__init__(msg)

class AspectInstanceNotFound(Exception):
    def __init__(self, msg): 
        super(AspectInstanceNotFound, self).__init__(msg)

class NetworkError(Exception):
    def __init__(self, msg): 
        super(NetworkError, self).__init__(msg)
