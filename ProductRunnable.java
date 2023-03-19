import java.io.BufferedReader;
import java.io.IOException;

/* Thread de nivel 2 - se ocupa de un produs */
public class ProductRunnable implements Runnable {
	private final String orderID;
	private final BufferedReader productsReader;

	public ProductRunnable(String orderID, BufferedReader productsReader) {
		this.orderID = orderID;
		this.productsReader = productsReader;
	}

	@Override
	public void run() {

		/* Citeste linie cu linie din fisier pana cand gaseste un produs din
		comanda curenta */
		String line;
		while (true) {
			try {
				line = productsReader.readLine();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			String orderID = line.split(",")[0];
			if (orderID.equals(this.orderID)) {
				break;
			}
		}

		/* Scrie in fisierul de output faptul ca produsul a fost livrat */
		try {
			Tema2.productsWriter.write(line + ",shipped\n");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		/* Se actualizeaza numarul de task-uri ramase in pool */
		Tema2.inQueue2.decrementAndGet();

		/* Decrementeaza valoarea latch-ului pentru comanda curenta, pentru a
		semnifica faptul ca task-ul si-a terminat executia */
		Tema2.latchMap.get(orderID).countDown();
	}
}
