import sys

sys.path.insert(1, "../Contract/target/generated-sources/protobuf/python")
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
from NameServer import NameServer
import grpc


class NameServerServiceImpl(pb2_grpc.NameServerServicer):

    def __init__(self):
        self.server = NameServer()

    def register(self, request, context):
        print(request)

        try:
            self.server.register(request.service, request.qualifier, request.address)
        except ValueError as e:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(str(e))
            raise grpc.RpcError(e)

        response = pb2.RegisterResponse()
        print(response)
        return response

    def lookup(self, request, context):
        print(request)

        addr_list = self.server.lookup(request.service, request.qualifier)
        response = pb2.LookupResponse(address=addr_list)
        print(response)
        return response

    def delete(self, request, context):
        print(request)

        try:
            self.server.delete(request.service, request.address)
        except ValueError as e:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(str(e))
            raise grpc.RpcError(e)

        response = pb2.DeleteResponse()
        print(response)
        return response
