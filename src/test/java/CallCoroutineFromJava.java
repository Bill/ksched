import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;

import java.time.LocalDateTime;
import java.util.Collection;

public class CallCoroutineFromJava {
  public static void main(String[] args) throws InterruptedException {

    final JavaCallableCoroutineScope jccs = new JavaCallableCoroutineScope();

    final Thread contentProducer = new Thread(() -> {
      while(true) {
        try {
          sleep(10);
        } catch (InterruptedException e) {
          return;
        }
        final String content = LocalDateTime.now().toString();
        DeferredKt.log2("sending content: " + content);
        jccs.submitStuff(content);
      }
    });

    final Thread snapshotter = new Thread(() -> {
      while(true) {
        try {
          sleep(100);
        } catch (InterruptedException e) {
          return;
        }
        DeferredKt.log2("sending snapshot request (synchronously)");
        final Collection<String> snapshot = jccs.snapshot();
        DeferredKt.log2("got snapshot (synchronously): " + snapshot);
      }
    });

    contentProducer.start();
    snapshotter.start();

    Thread.sleep(10_000);

    contentProducer.interrupt();
    snapshotter.interrupt();

    contentProducer.join();
    snapshotter.join();
  }
}
