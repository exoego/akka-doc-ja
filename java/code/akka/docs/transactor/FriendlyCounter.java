/**
 * Copyright (C) 2009-2011 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.docs.transactor;

//#class
import akka.actor.*;
import akka.transactor.*;
import java.util.Set;
import scala.concurrent.stm.Ref;
import static scala.concurrent.stm.JavaAPI.*;

public class FriendlyCounter extends UntypedTransactor {
    Ref.View<Integer> count = newRef(0);

    @Override public Set<SendTo> coordinate(Object message) {
        if (message instanceof Increment) {
            Increment increment = (Increment) message;
            if (increment.hasFriend())
                return include(increment.getFriend(), new Increment());
        }
        return nobody();
    }

    public void atomically(Object message) {
        if (message instanceof Increment) {
            increment(count, 1);
        }
    }

    @Override public boolean normally(Object message) {
        if ("GetCount".equals(message)) {
            getSender().tell(count.get());
            return true;
        } else return false;
    }
}
//#class
