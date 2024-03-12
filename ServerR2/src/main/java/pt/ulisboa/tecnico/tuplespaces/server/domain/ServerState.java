package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import pt.ulisboa.tecnico.tuplespaces.server.ServerMain;
import pt.ulisboa.tecnico.tuplespaces.server.exceptions.MatchingTuplesLockException;

public class ServerState {

  private class Tuple {
    private final String value;
    private boolean locked = false;
    private int clientId;

    public Tuple(String value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Tuple tuple = (Tuple) obj;
      return this.value.equals(tuple.value);
    }

    @Override
    public String toString() {
      return this.value;
    }
  }

  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";
  private final List<Tuple> tuples;

  public ServerState() {
    this.tuples = new ArrayList<Tuple>();
  }

  public synchronized void put(String tuple) {
    tuple = tuple.trim();

    if (!isTupleValid(tuple)) {
      throw new IllegalArgumentException("Invalid tuple");
    }

    final String oldTuples = tuples.toString();
    tuples.add(new Tuple(tuple));
    ServerMain.debug(
        "put('%s'):\n\t\told tuples: %s\n\t\tnew tuples: %s\n", tuple, oldTuples, tuples);
    ServerMain.debug("@put: notifying all threads\n");
    notifyAll();
  }

  public synchronized String read(String pattern) throws InterruptedException {
    if (!isPatternValidRegex(pattern)) {
      throw new IllegalArgumentException("Invalid search pattern");
    }

    Tuple tuple;
    while ((tuple = getMatchingTuple(pattern)) == null) {
      ServerMain.debug("@read: couldn't find matching tuple with '%s', waiting...\n", pattern);
      this.wait();
    }
    return tuple.value;
  }

  public synchronized List<String> takePhase1(String pattern, int clientId)
      throws MatchingTuplesLockException {
    if (!isPatternValidRegex(pattern)) {
      throw new IllegalArgumentException("Invalid search pattern");
    }

    List<Tuple> matchingTuples = lockMatchingTuples(pattern, clientId);

    if (matchingTuples == null) {
      throw new MatchingTuplesLockException("Couldn't acquire locks");
    }

    ServerMain.debug(
        "@take: locked the following tuples for client %s: %s\n", clientId, matchingTuples);

    return matchingTuples.stream().map(tuple -> tuple.value).toList();
  }

  public synchronized void takePhase2(String chosenTuple, int clientId) {
    Tuple tuple = getMatchingTuple(chosenTuple);
    if (tuple == null || !tuple.locked || tuple.clientId != clientId) {
      // Shouldn't happen
      throw new IllegalArgumentException("Invalid tuple");
    }

    this.tuples.remove(tuple);

    releaseClientLocks(clientId);
  }

  public synchronized void releaseClientLocks(int clientId) {
    for (Tuple tuple : this.tuples) {
      if (tuple.locked && tuple.clientId == clientId) {
        tuple.locked = false;
      }
    }
  }

  public synchronized List<String> getTupleSpacesState() {
    return tuples.stream().map(tuple -> tuple.value).toList();
  }

  private Tuple getMatchingTuple(String pattern) {
    for (Tuple tuple : this.tuples) {
      if (tuple.value.matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  private List<Tuple> lockMatchingTuples(String pattern, int clientId) {
    List<Tuple> matchingTuples = new ArrayList<>();

    for (Tuple tuple : this.tuples) {
      if (tuple.value.matches(pattern)) {
        if (tuple.locked && tuple.clientId != clientId) {
          return null;
        }
        matchingTuples.add(tuple);
      }
    }

    for (Tuple tuple : matchingTuples) {
      tuple.locked = true;
      tuple.clientId = clientId;
    }

    return matchingTuples;
  }

  private boolean isTupleValid(String tuple) {
    return tuple.startsWith(BGN_TUPLE) && tuple.endsWith(END_TUPLE) && !tuple.contains(" ");
  }

  private boolean isPatternValidRegex(String pattern) {
    try {
      Pattern.compile(pattern);
      return true;
    } catch (PatternSyntaxException e) {
      return false;
    }
  }
}
