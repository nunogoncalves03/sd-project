import sys

sys.path.insert(1, "../Contract/target/generated-sources/protobuf/python")
import grpc
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
from concurrent import futures
from NameServerServiceImpl import NameServerServiceImpl
from Debug import Debug


# define the port
PORT = 5001

if __name__ == "__main__":
    try:
        for i in range(1, len(sys.argv)):
            if sys.argv[i] == "-debug":
                Debug.debug = True

        # create server
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
        # add service
        pb2_grpc.add_NameServerServicer_to_server(NameServerServiceImpl(), server)
        # listen on port
        server.add_insecure_port("[::]:" + str(PORT))
        # start server
        server.start()
        # print message
        Debug.log("Server listening on port " + str(PORT) + "\n")
        # print termination message
        Debug.log("Press CTRL+C to terminate\n")
        # wait for server to finish
        server.wait_for_termination()
    except KeyboardInterrupt:
        Debug.log("NameServer stopped\n")
        exit(0)
