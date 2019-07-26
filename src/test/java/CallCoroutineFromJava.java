import static java.lang.Thread.sleep;

import java.time.LocalDateTime;
import java.util.Collection;

public class CallCoroutineFromJava {
  public static void main(String[] args) throws InterruptedException {

    final JavaCallableCoroutineScope jccs = new JavaCallableCoroutineScope();

    final Thread contentProducer = new Thread(() -> {
      try {
        while (true) {
          sleep(10);
          final String content = LocalDateTime.now().toString();
          DeferredKt.log2("sending content: " + content);
          jccs.submitStuff(content);
        }
      } catch (InterruptedException e) {
      }
    });

    final Thread snapshotter = new Thread(() -> {
      try {
        while (true) {
          sleep(100);
          DeferredKt.log2("sending snapshot request (synchronously)");
          final Collection<String> snapshot = jccs.snapshot();
          DeferredKt.log2("got snapshot (synchronously): " + snapshot);
        }
      } catch (InterruptedException e) {
      }
    });

    contentProducer.setDaemon(true);
    snapshotter.setDaemon(true);

    contentProducer.start();
    snapshotter.start();

    Thread.sleep(10_000);
  }
}
