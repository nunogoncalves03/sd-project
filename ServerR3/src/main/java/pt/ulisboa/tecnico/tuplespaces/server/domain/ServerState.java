package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import pt.ulisboa.tecnico.tuplespaces.server.ServerMain;

public class ServerState {

  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";
  private final List<String> tuples;

  public ServerState() {
    this.tuples = new ArrayList<String>();
  }

  public synchronized void put(String tuple) {
    tuple = tuple.trim();

    if (!isTupleValid(tuple)) {
      throw new IllegalArgumentException("Invalid tuple");
    }

    final String oldTuples = this.tuples.toString();
    this.tuples.add(tuple);
    ServerMain.debug(
        "put('%s'):\n\t\told tuples: %s\n\t\tnew tuples: %s\n", tuple, oldTuples, this.tuples);
    ServerMain.debug("@put: notifying all threads\n");
    notifyAll();
  }

  public synchronized String read(String pattern) throws InterruptedException {
    if (!isPatternValidRegex(pattern)) {
      throw new IllegalArgumentException("Invalid search pattern");
    }

    String tuple;
    while ((tuple = getMatchingTuple(pattern)) == null) {
      ServerMain.debug("@read: couldn't find matching tuple with '%s', waiting...\n", pattern);
      this.wait();
    }
    return tuple;
  }

  public synchronized String take(String pattern) throws InterruptedException {
    if (!isPatternValidRegex(pattern)) {
      throw new IllegalArgumentException("Invalid search pattern");
    }

    String toRemove;
    while ((toRemove = getMatchingTuple(pattern)) == null) {
      ServerMain.debug("@take: couldn't find matching tuple with '%s', waiting...\n", pattern);
      this.wait();
    }

    final String oldTuples = this.tuples.toString();
    this.tuples.remove(toRemove);
    ServerMain.debug(
            "take('%s') -> removing '%s':\n\t\told tuples: %s\n\t\tnew tuples: %s\n",
            pattern, toRemove, oldTuples, this.tuples);

    return toRemove;
  }

  public synchronized List<String> getTupleSpacesState() {
    return tuples.stream().toList();
  }

  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return null;
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
