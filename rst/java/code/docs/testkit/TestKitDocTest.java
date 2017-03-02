/**
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package docs.testkit;

import static org.junit.Assert.*;

import akka.testkit.*;
import docs.AbstractJavaTest;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;

import akka.actor.ActorKilledException;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Kill;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.AbstractActor;
import scala.concurrent.Await;
import scala.concurrent.Future;
import akka.testkit.TestActor.AutoPilot;
import scala.concurrent.duration.Duration;

public class TestKitDocTest extends AbstractJavaTest {

  @ClassRule
  public static AkkaJUnitActorSystemResource actorSystemResource =
    new AkkaJUnitActorSystemResource("TestKitDocTest",
      ConfigFactory.parseString("akka.loggers = [akka.testkit.TestEventListener]"));

  private final ActorSystem system = actorSystemResource.getSystem();

  //#test-actor-ref
  static class MyActor extends AbstractActor {
    @Override
    public Receive createReceive() {
      return receiveBuilder()
        .matchEquals("say42", message -> {
          sender().tell(42, self());
        })
        .match(Exception.class, (Exception ex) -> {
          throw ex;
        })
        .build();
    }
    public boolean testMe() { return true; }
  }

  @Test
  public void demonstrateTestActorRef() {
    final Props props = Props.create(MyActor.class);
    final TestActorRef<MyActor> ref = TestActorRef.create(system, props, "testA");
    final MyActor actor = ref.underlyingActor();
    assertTrue(actor.testMe());
  }
  //#test-actor-ref

  @Test
  public void demonstrateAsk() throws Exception {
    //#test-behavior
    final Props props = Props.create(MyActor.class);
    final TestActorRef<MyActor> ref = TestActorRef.create(system, props, "testB");
    final Future<Object> future = akka.pattern.Patterns.ask(ref, "say42", 3000);
    assertTrue(future.isCompleted());
    assertEquals(42, Await.result(future, Duration.Zero()));
    //#test-behavior
  }

  @Test
  public void demonstrateExceptions() {
    //#test-expecting-exceptions
    final Props props = Props.create(MyActor.class);
    final TestActorRef<MyActor> ref = TestActorRef.create(system, props, "myActor");
    try {
      ref.receive(new Exception("expected"));
      Assert.fail("expected an exception to be thrown");
    } catch (Exception e) {
      assertEquals("expected", e.getMessage());
    }
    //#test-expecting-exceptions
  }

  @Test
  public void demonstrateWithin() {
    //#test-within
    new JavaTestKit(system) {{
      getRef().tell(42, ActorRef.noSender());
      new Within(Duration.Zero(), Duration.create(1, "second")) {
        // do not put code outside this method, will run afterwards
        public void run() {
          assertEquals((Integer) 42, expectMsgClass(Integer.class));
        }
      };
    }};
    //#test-within
  }

  @Test
  public void demonstrateExpectMsg() {
    //#test-expectmsg
    new JavaTestKit(system) {{
      getRef().tell(42, ActorRef.noSender());
      final String out = new ExpectMsg<String>("match hint") {
          // do not put code outside this method, will run afterwards
          protected String match(Object in) {
            if (in instanceof Integer) {
              return "match";
            } else {
              throw noMatch();
            }
          }
        }.get(); // this extracts the received message
      assertEquals("match", out);
    }};
    //#test-expectmsg
  }

  @Test
  public void demonstrateReceiveWhile() {
    //#test-receivewhile
    new JavaTestKit(system) {{
      getRef().tell(42, ActorRef.noSender());
      getRef().tell(43, ActorRef.noSender());
      getRef().tell("hello", ActorRef.noSender());
      final String[] out =
        new ReceiveWhile<String>(String.class, duration("1 second")) {
          // do not put code outside this method, will run afterwards
          protected String match(Object in) {
            if (in instanceof Integer) {
              return in.toString();
            } else {
              throw noMatch();
            }
          }
        }.get(); // this extracts the received messages
      assertArrayEquals(new String[] {"42", "43"}, out);
      expectMsgEquals("hello");
    }};
    //#test-receivewhile
    new JavaTestKit(system) {{
      //#test-receivewhile-full
      new ReceiveWhile<String>(     // type of array to be created must match ...
            String.class,           // ... this class which is needed to that end
            duration("100 millis"), // maximum collect time
            duration("50 millis"),  // maximum time between messages
            12                      // maximum number of messages to collect
            ) {
        //#match-elided
        protected String match(Object in) {
          throw noMatch();
        }
        //#match-elided
      };
      //#test-receivewhile-full
    }};
  }

  @Test
  public void demonstrateAwaitCond() {
    //#test-awaitCond
    new JavaTestKit(system) {{
      getRef().tell(42, ActorRef.noSender());
      new AwaitCond(
            duration("1 second"),  // maximum wait time
            duration("100 millis") // interval at which to check the condition
            ) {
        // do not put code outside this method, will run afterwards
        protected boolean cond() {
          // typically used to wait for something to start up
          return msgAvailable();
        }
      };
    }};
    //#test-awaitCond
  }

  @Test
  public void demonstrateAwaitAssert() {
    //#test-awaitAssert
    new JavaTestKit(system) {{
      getRef().tell(42, ActorRef.noSender());
      new AwaitAssert(
            duration("1 second"),  // maximum wait time
            duration("100 millis") // interval at which to check the condition
            ) {
        // do not put code outside this method, will run afterwards
        protected void check() {
          assertEquals(msgAvailable(), true);
        }
      };
    }};
    //#test-awaitAssert
  }

  @Test
  @SuppressWarnings({ "unchecked", "unused" }) // due to generic varargs
  public void demonstrateExpect() {
    new JavaTestKit(system) {{
      getRef().tell("hello", ActorRef.noSender());
      getRef().tell("hello", ActorRef.noSender());
      getRef().tell("hello", ActorRef.noSender());
      getRef().tell("world", ActorRef.noSender());
      getRef().tell(42, ActorRef.noSender());
      getRef().tell(42, ActorRef.noSender());
      //#test-expect
      final String hello = expectMsgEquals("hello");
      final Object   any = expectMsgAnyOf("hello", "world");
      final Object[] all = expectMsgAllOf("hello", "world");
      final int i        = expectMsgClass(Integer.class);
      final Number j     = expectMsgAnyClassOf(Integer.class, Long.class);
      expectNoMsg();
      //#test-expect
      getRef().tell("receveN-1", ActorRef.noSender());
      getRef().tell("receveN-2", ActorRef.noSender());
      //#test-expect
      final Object[] two = receiveN(2);
      //#test-expect
      assertEquals("hello", hello);
      assertEquals("hello", any);
      assertEquals(42, i);
      assertEquals(42, j);
      assertArrayEquals(new String[] {"hello", "world"}, all);
    }};
  }

  @Test
  public void demonstrateIgnoreMsg() {
    //#test-ignoreMsg
    new JavaTestKit(system) {{
      // ignore all Strings
      new IgnoreMsg() {
        protected boolean ignore(Object msg) {
          return msg instanceof String;
        }
      };
      getRef().tell("hello", ActorRef.noSender());
      getRef().tell(42, ActorRef.noSender());
      expectMsgEquals(42);
      // remove message filter
      ignoreNoMsg();
      getRef().tell("hello", ActorRef.noSender());
      expectMsgEquals("hello");
    }};
    //#test-ignoreMsg
  }

  @Test
  public void demonstrateDilated() {
    //#duration-dilation
    new JavaTestKit(system) {{
      final Duration original = duration("1 second");
      final Duration stretched = dilated(original);
      assertTrue("dilated", stretched.gteq(original));
    }};
    //#duration-dilation
  }

  @Test
  public void demonstrateProbe() {
    //#test-probe
    new JavaTestKit(system) {{
      // simple actor which just forwards messages
      class Forwarder extends AbstractActor {
        final ActorRef target;
        @SuppressWarnings("unused")
        public Forwarder(ActorRef target) {
          this.target = target;
        }
        @Override
        public Receive createReceive() {
          return receiveBuilder()
            .matchAny(message -> target.forward(message, getContext()))
            .build();
        }
      }
      
      // create a test probe
      final JavaTestKit probe = new JavaTestKit(system);

      // create a forwarder, injecting the probe’s testActor
      final Props props = Props.create(Forwarder.class, this, probe.getRef());
      final ActorRef forwarder = system.actorOf(props, "forwarder");

      // verify correct forwarding
      forwarder.tell(42, getRef());
      probe.expectMsgEquals(42);
      assertEquals(getRef(), probe.getLastSender());
    }};
    //#test-probe
  }

  @Test
  public void demonstrateTestProbeWithCustomName() {
    //#test-probe-with-custom-name
    new JavaTestKit(system) {{
      final TestProbe worker = new TestProbe(system, "worker");
      final TestProbe aggregator = new TestProbe(system, "aggregator");

      assertTrue(worker.ref().path().name().startsWith("worker"));
      assertTrue(aggregator.ref().path().name().startsWith("aggregator"));
    }};
    //#test-probe-with-custom-name
  }

  @Test
  public void demonstrateSpecialProbe() {
    //#test-special-probe
    new JavaTestKit(system) {{
      class MyProbe extends JavaTestKit {
        public MyProbe() {
          super(system);
        }
        public void assertHello() {
          expectMsgEquals("hello");
        }
      }

      final MyProbe probe = new MyProbe();
      probe.getRef().tell("hello", ActorRef.noSender());
      probe.assertHello();
    }};
    //#test-special-probe
  }

  @Test
  public void demonstrateWatch() {
    final ActorRef target = system.actorOf(Props.create(MyActor.class));
    //#test-probe-watch
    new JavaTestKit(system) {{
      final JavaTestKit probe = new JavaTestKit(system);
      probe.watch(target);
      target.tell(PoisonPill.getInstance(), ActorRef.noSender());
      final Terminated msg = probe.expectMsgClass(Terminated.class);
      assertEquals(msg.getActor(), target);
    }};
    //#test-probe-watch
  }

  @Test
  public void demonstrateReply() {
    //#test-probe-reply
    new JavaTestKit(system) {{
      final JavaTestKit probe = new JavaTestKit(system);
      probe.getRef().tell("hello", getRef());
      probe.expectMsgEquals("hello");
      probe.reply("world");
      expectMsgEquals("world");
      assertEquals(probe.getRef(), getLastSender());
    }};
    //#test-probe-reply
  }

  @Test
  public void demonstrateForward() {
    //#test-probe-forward
    new JavaTestKit(system) {{
      final JavaTestKit probe = new JavaTestKit(system);
      probe.getRef().tell("hello", getRef());
      probe.expectMsgEquals("hello");
      probe.forward(getRef());
      expectMsgEquals("hello");
      assertEquals(getRef(), getLastSender());
    }};
    //#test-probe-forward
  }

  @Test
  public void demonstrateWithinProbe() {
    try {
    //#test-within-probe
    new JavaTestKit(system) {{
      final JavaTestKit probe = new JavaTestKit(system);
      new Within(duration("1 second")) {
        public void run() {
          probe.expectMsgEquals("hello");
        }
      };
    }};
    //#test-within-probe
    } catch (AssertionError e) {
      // expected to fail
    }
  }

  @Test
  public void demonstrateAutoPilot() {
    //#test-auto-pilot
    new JavaTestKit(system) {{
      final JavaTestKit probe = new JavaTestKit(system);
      // install auto-pilot
      probe.setAutoPilot(new TestActor.AutoPilot() {
        public AutoPilot run(ActorRef sender, Object msg) {
          sender.tell(msg, ActorRef.noSender());
          return noAutoPilot();
        }
      });
      // first one is replied to directly ...
      probe.getRef().tell("hello", getRef());
      expectMsgEquals("hello");
      // ... but then the auto-pilot switched itself off
      probe.getRef().tell("world", getRef());
      expectNoMsg();
    }};
    //#test-auto-pilot
  }

  // only compilation
  public void demonstrateCTD() {
    //#calling-thread-dispatcher
    system.actorOf(
      Props.create(MyActor.class)
        .withDispatcher(CallingThreadDispatcher.Id()));
    //#calling-thread-dispatcher
  }

  @Test
  public void demonstrateEventFilter() {
    //#test-event-filter
    new JavaTestKit(system) {{
      assertEquals("TestKitDocTest", system.name());
      final ActorRef victim = system.actorOf(Props.empty(), "victim");

      final int result = new EventFilter<Integer>(ActorKilledException.class) {
        protected Integer run() {
          victim.tell(Kill.getInstance(), ActorRef.noSender());
          return 42;
        }
      }.from("akka://TestKitDocTest/user/victim").occurrences(1).exec();

      assertEquals(42, result);
    }};
    //#test-event-filter
  }

}
