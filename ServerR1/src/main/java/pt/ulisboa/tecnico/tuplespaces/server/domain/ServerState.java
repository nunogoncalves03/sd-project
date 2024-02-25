package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.server.ServerMain;

public class ServerState {

  private final List<String> tuples;

  public ServerState() {
    this.tuples = new ArrayList<String>();
  }

  public synchronized void put(String tuple) {
    final String oldTuples = tuples.toString();
    tuples.add(tuple);
    ServerMain.debug(
        "put('%s'):\n\t\told tuples: %s\n\t\tnew tuples: %s\n", tuple, oldTuples, tuples);
    ServerMain.debug("@put: notifying all threads\n");
    notifyAll();
  }

  public synchronized String read(String pattern) throws InterruptedException {
    String tuple;
    while ((tuple = getMatchingTuple(pattern)) == null) {
      ServerMain.debug("@read: couldn't find matching tuple with '%s', waiting...\n", pattern);
      this.wait();
    }
    return tuple;
  }

  public synchronized String take(String pattern) throws InterruptedException {
    String toRemove;
    while ((toRemove = getMatchingTuple(pattern)) == null) {
      ServerMain.debug("@take: couldn't find matching tuple with '%s', waiting...\n", pattern);
      this.wait();
    }

    final String oldTuples = tuples.toString();
    this.tuples.remove(toRemove);
    ServerMain.debug(
        "take('%s') -> removing '%s':\n\t\told tuples: %s\n\t\tnew tuples: %s\n",
        pattern, toRemove, oldTuples, tuples);

    return toRemove;
  }

  public synchronized List<String> getTupleSpacesState() {
    return tuples;
  }

  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }
}
