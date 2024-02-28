import sys

sys.path.insert(1, "../Contract/target/generated-sources/protobuf/python")
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
from NameServer import NameServer
import grpc
from Debug import Debug


class NameServerServiceImpl(pb2_grpc.NameServerServicer):

    def __init__(self):
        self.server = NameServer()

    def register(self, request, context):
        Debug.log(f"request @register: {request}")

        try:
            self.server.register(request.service, request.qualifier, request.address)
        except ValueError as e:
            Debug.log(f"Error @register: {e}\n")
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(str(e))
            raise grpc.RpcError(e)

        response = pb2.RegisterResponse()
        Debug.log(f"response @register: empty\n")
        return response

    def lookup(self, request, context):
        Debug.log(f"request @lookup: {request}")

        addr_list = self.server.lookup(request.service, request.qualifier)
        response = pb2.LookupResponse(address=addr_list)
        Debug.log(f"response @lookup: {response if len(addr_list) != 0 else 'empty\n'}")
        return response

    def delete(self, request, context):
        Debug.log(f"request @delete: {request}")

        try:
            self.server.delete(request.service, request.address)
        except ValueError as e:
            Debug.log(f"Error @delete: {e}\n")
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(str(e))
            raise grpc.RpcError(e)

        response = pb2.DeleteResponse()
        Debug.log(f"response @delete: empty\n")
        return response
