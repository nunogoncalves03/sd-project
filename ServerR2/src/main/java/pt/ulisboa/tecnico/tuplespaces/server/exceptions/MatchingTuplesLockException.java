package pt.ulisboa.tecnico.tuplespaces.server.exceptions;

public class MatchingTuplesLockException extends Exception {
  public MatchingTuplesLockException() {
    super();
  }

  public MatchingTuplesLockException(String message) {
    super("LOCK_FAILED: " + message);
  }
}
