import java.io.*;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Tema2 {

	public static int threads;

	public static File ordersFile, productsFile, ordersOutFile, productsOutFile;

	public static BufferedReader ordersReader, productsReader;
	public static BufferedWriter ordersWriter, productsWriter;

	public static AtomicInteger inQueue1, inQueue2;
	public static ExecutorService tpe1, tpe2;

	/* O comanda trebuie sa astepte ca toate produsele acesteia sa fie procesate; astfel,
	fiecare comanda va avea un latch ce va avea ca scop asteptarea terminarii executiei
	tuturor task-urilor aferente produselor acestei comenzi; este folosit ConcurrentHashMap
	deoarece acesta este thread-safe */
	public static Map<String, CountDownLatch> latchMap = new ConcurrentHashMap<>();

	public static void main(String[] args) throws IOException, InterruptedException {

		String folder = args[0];
		threads = Integer.parseInt(args[1]);

		String ordersPath = folder + "/orders.txt";
		String productsPath = folder + "/order_products.txt";
		String ordersOutPath = "orders_out.txt";
		String productsOutPath = "order_products_out.txt";

		ordersFile = new File(ordersPath);
		productsFile = new File(productsPath);
		ordersOutFile = new File(ordersOutPath);
		productsOutFile = new File(productsOutPath);

		/* Pentru citire si scriere in fisier se vor folosi BufferedReader si BufferedWriter,
		deoarece acestea sunt thread-safe si pot fi partajate in mai multe thread-uri */

		ordersReader = new BufferedReader(new FileReader(ordersFile));
		productsReader = new BufferedReader(new FileReader(productsFile));
		ordersWriter = new BufferedWriter(new FileWriter(ordersOutFile));
		productsWriter = new BufferedWriter(new FileWriter(productsOutFile));

		/* Pool pentru thread-urile de nivel 1, cele care se ocupa de comenzi, care citesc
		din fisierul "orders.txt" */
		tpe1 = Executors.newFixedThreadPool(threads);
		inQueue1 = new AtomicInteger(0);

		/* Pool pentru thread-urile de nivel 2, cele care se ocupa de produse, care citesc
		din fisierul "order_products.txt" */
		tpe2 = Executors.newFixedThreadPool(threads);
		inQueue2 = new AtomicInteger(0);

		/* Se adauga initial in pool-ul pentru thread-urile de nivel 1 un numar de task-uri
		egal cu numarul de thread-uri */
		for (int i = 0; i < threads; i++) {
			inQueue1.incrementAndGet();
			tpe1.submit(new OrderRunnable());
		}

		/* Se asteapta terminarea executiei tuturor task-urilor din primul pool (deci,
		implicit, si din al doilea) */
		if (!tpe1.awaitTermination(10, TimeUnit.SECONDS)) {
			tpe1.shutdownNow();
		}

		productsWriter.close();
		ordersWriter.close();
		productsReader.close();
		ordersReader.close();
	}

}
