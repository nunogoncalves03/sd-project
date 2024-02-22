package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;

public class ServerState {

  private List<String> tuples;

  public ServerState() {
    this.tuples = new ArrayList<String>();

  }

  public synchronized void put(String tuple) {
    tuples.add(tuple);
    notifyAll();
  }

  public synchronized String read(String pattern) throws InterruptedException {
    String tuple = getMatchingTuple(pattern);
    while (tuple == null){
      this.wait();
      tuple = getMatchingTuple(pattern);
    }
    return tuple;
  }

  public synchronized String take (String pattern) throws InterruptedException {
    String toRemove = getMatchingTuple(pattern);
    while (toRemove == null){
      this.wait();
      toRemove = getMatchingTuple(pattern);
    }
    this.tuples.remove(toRemove);
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
