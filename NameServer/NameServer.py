class ServerEntry:
    def __init__(self, qualifier: str, address: str) -> None:
        self.qualifier: str = qualifier
        self.address: str = address


class ServiceEntry:
    def __init__(self, service, servers=[]) -> None:
        self.service: str = service
        self.servers: list[ServerEntry] = servers

    def add_server(self, server: ServerEntry) -> None:
        # check for duplicate address
        if any(map(lambda x: x.address == server.address, self.servers)):
            raise ValueError("Duplicate address")
        self.servers.append(server)

    def list_addresses(self, qualifier: str = "") -> list[str]:
        if qualifier == "":
            return [server.address for server in self.servers]
        sv_list = []
        for sv_entry in self.servers:
            if sv_entry.qualifier == qualifier:
                sv_list.append(sv_entry.address)

        return sv_list

    def remove_server(self, address: str):
        for server in self.servers:
            if server.address == address:
                self.servers.remove(server)
                return

        raise ValueError("Address not found")


class NameServer:
    def __init__(self) -> None:
        self.services: dict[str, ServiceEntry] = dict()

    def register(self, service: str, qualifier: str, address: str) -> None:
        if service not in self.services:
            self.services[service] = ServiceEntry(service, [])
        self.services[service].add_server(ServerEntry(qualifier, address))

    def lookup(self, service: str, qualifier: str = "") -> list[str]:
        if service not in self.services:
            return []
        service_entry = self.services[service]
        return service_entry.list_addresses(qualifier)


    def delete(self, service: str, address: str) -> None:
        if service not in self.services:
            raise ValueError("Service not found")
        service_entry = self.services[service]
        service_entry.remove_server(address)
