package jdocs.stream.operators.sourceorflow;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.time.Duration;
import java.util.stream.Stream;
import docs.stream.operators.sourceorflow.ThrottleCommon.Frame;

/**
 *
 */
public class Throttle {

    public static void main(String[] args) {
        ActorSystem actorSystem = ActorSystem.create("25fps-throttled-stream");
        Materializer mat = Materializer.matFromSystem(actorSystem);

        Source<Frame, NotUsed> frameSource =
                Source
                        .fromIterator(() -> Stream.iterate(0 , i -> i+1).iterator())
                        .map(i -> new Frame(i.intValue())) ;

        // #throttle
        int framesPerSecond = 24;

        Source<Frame, NotUsed> videoThrottling = frameSource
                .throttle(
                        framesPerSecond,
                        Duration.ofSeconds(1),
                        framesPerSecond * 30,
                        ThrottleMode.shaping()
                );
        // serialize `Frame` and send over the network.
        // #throttle

        videoThrottling
                .map(f -> f.i())
                .to(Sink.foreach(System.out::println))
                .run(mat);

    }


}
