package com.mobicrave.eventtracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class UserEventIndex {
  private IdList[] index;

  private UserEventIndex(IdList[] index) {
    this.index = index;
  }

  public void enumerateEventIds(long userId, long firstStepEventId, long maxLastEventId,
      Callback callback) {
    IdList.Iterator eventIdIterator = index[(int) userId].subList(firstStepEventId, maxLastEventId);
    while (eventIdIterator.hasNext()) {
      if (!callback.onEventId(eventIdIterator.next())) {
        return;
      }
    }
  }

  public void addEvent(long eventId, long userId) {
    // TODO: userId needs to be < 4B
    index[(int) userId].add(eventId);
  }

  public void addUser(long userId) {
    // TODO: userId needs to be < 4B
    if (userId >= index.length) {
      synchronized (this) {
        if (userId >= index.length) {
          IdList[] newIndex = new IdList[index.length * 2];
          System.arraycopy(index, 0, newIndex, 0, index.length);
          index = newIndex;
        }
      }
    }
    index[(int) userId] = IdList.build();
  }

  public void close(String directory) {
    new File(directory).mkdirs();
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getSerializationFile(directory)))) {
      oos.writeObject(index);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getSerializationFile(String directory) {
    return directory + "/user_event_index.ser";
  }

  public static UserEventIndex build(String directory) {
    File file = new File(getSerializationFile(directory));
    if (file.exists()) {
      try {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        IdList[] index = (IdList[]) ois.readObject();
        return new UserEventIndex(index);
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return new UserEventIndex(new IdList[1024]);
  }

  public static interface Callback {
    public boolean onEventId(long eventId);
  }
}