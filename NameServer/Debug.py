class Debug:
    debug = False

    @staticmethod
    def log(msg):
        if Debug.debug:
            print(f"[DEBUG] {msg}", end="")
