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
  private final SequenceNumber sequenceNumber;
  private final List<PendingTake> pendingTakes;

  private class SequenceNumber {
    private int value = 1;

    public synchronized int getValue() {
      return this.value;
    }

    public synchronized void increment() {
      this.value++;
      this.notifyAll();
    }
  }

  private class PendingTake {
    private final String pattern;
    private String takenTuple = null;

    public PendingTake(String pattern) {
      this.pattern = pattern;
    }

    public synchronized boolean matches(String tuple) {
      return tuple.matches(this.pattern);
    }

    public synchronized void deliverTuple(String takenTuple) {
      this.takenTuple = takenTuple;
      this.notify();
    }

    public synchronized String waitForTuple() throws InterruptedException {
      if (this.takenTuple == null) {
        this.wait();
      }
      return this.takenTuple;
    }
  }

  public ServerState() {
    this.tuples = new ArrayList<String>();
    this.pendingTakes = new ArrayList<PendingTake>();
    this.sequenceNumber = new SequenceNumber();
  }

  public void put(String tuple, int sequenceNumber) throws InterruptedException {
    waitForTurn(sequenceNumber);

    tuple = tuple.trim();
    if (!isTupleValid(tuple)) {
      this.sequenceNumber.increment();
      throw new IllegalArgumentException("Invalid tuple");
    }

    synchronized (this.pendingTakes) {
      for (PendingTake pendingTake : this.pendingTakes) {
        if (pendingTake.matches(tuple)) {
          this.pendingTakes.remove(pendingTake);
          pendingTake.deliverTuple(tuple);

          this.sequenceNumber.increment();
          return;
        }
      }
    }

    synchronized (this.tuples) {
      final String oldTuples = this.tuples.toString();
      this.tuples.add(tuple);
      ServerMain.debug(
          "put('%s'):\n\t\told tuples: %s\n\t\tnew tuples: %s\n", tuple, oldTuples, this.tuples);
      ServerMain.debug("@put %d: notifying all threads on tuples\n", sequenceNumber);
      this.tuples.notifyAll();
    }

    this.sequenceNumber.increment();
  }

  public String read(String pattern) throws InterruptedException {
    if (!isPatternValidRegex(pattern)) {
      throw new IllegalArgumentException("Invalid search pattern");
    }

    synchronized (this.tuples) {
      String tuple;
      while ((tuple = getMatchingTuple(pattern)) == null) {
        ServerMain.debug(
            "@read: couldn't find matching tuple with '%s', waiting on tuples...\n", pattern);
        this.tuples.wait();
      }
      return tuple;
    }
  }

  public String take(String pattern, int sequenceNumber) throws InterruptedException {
    waitForTurn(sequenceNumber);

    if (!isPatternValidRegex(pattern)) {
      this.sequenceNumber.increment();
      throw new IllegalArgumentException("Invalid search pattern");
    }

    synchronized (this.tuples) {
      String takenTuple;
      if ((takenTuple = getMatchingTuple(pattern)) != null) {
        final String oldTuples = this.tuples.toString();
        this.tuples.remove(takenTuple);
        ServerMain.debug(
            "take('%s') -> removing '%s':\n\t\told tuples: %s\n\t\tnew tuples: %s\n",
            pattern, takenTuple, oldTuples, this.tuples);

        this.sequenceNumber.increment();
        return takenTuple;
      }
    }

    PendingTake pendingTake = new PendingTake(pattern);
    synchronized (this.pendingTakes) {
      this.pendingTakes.add(pendingTake);
    }

    this.sequenceNumber.increment();
    ServerMain.debug(
        "@take %d: couldn't find matching tuple with '%s', waiting...\n", sequenceNumber, pattern);
    String takenTuple = pendingTake.waitForTuple();
    ServerMain.debug("@take %d: removed tuple %s\n", sequenceNumber, takenTuple);

    return takenTuple;
  }

  public List<String> getTupleSpacesState() {
    synchronized (this.tuples) {
      return this.tuples.stream().toList();
    }
  }

  private void waitForTurn(int sequenceNumber) throws InterruptedException {
    int currentSequenceNumber;
    synchronized (this.sequenceNumber) {
      while ((currentSequenceNumber = this.sequenceNumber.getValue()) != sequenceNumber) {
        ServerMain.debug(
            "waiting for turn %d: current sequence number is %d\n",
            sequenceNumber, currentSequenceNumber);
        this.sequenceNumber.wait();
      }
    }
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
