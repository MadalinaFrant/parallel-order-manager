import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/* Thread de nivel 1 - se ocupa de o comanda */
public class OrderRunnable implements Runnable {

	public OrderRunnable() {
	}

	@Override
	public void run() {

		/* Citeste urmatoarea linie din fisier */
		String line;
		try {
			line = Tema2.ordersReader.readLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		/* Daca a fost citita o linie, deci fisierul nu s-a terminat, se vor
		executa operatiile corespunzatoare si se va adauga inca un task in
		pool-ul de nivel 1 */

		if (line != null) {

			String orderID = line.split(",")[0];
			int Nproducts = Integer.parseInt(line.split(",")[1]);

			/* Deschide fisierul de produse pentru fiecare comanda pentru a
			incepe citirea acestuia de la inceput */

			BufferedReader productsReader;
			try {
				productsReader = new BufferedReader(new FileReader(Tema2.productsFile));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}

			/* Initializeaza latch-ul aferent comenzii curente cu numarul de produse din aceasta */
			CountDownLatch latch = new CountDownLatch(Nproducts);
			Tema2.latchMap.put(orderID, latch);

			/* Deschide cate un task in al doilea pool pentru fiecare produs cautat */
			for (int i = 0; i < Nproducts; i++) {
				Tema2.inQueue2.incrementAndGet();
				Tema2.tpe2.submit(new ProductRunnable(orderID, productsReader));
			}

			/* Asteapta pana cand toate task-urile care se ocupa de produsele comenzii curente
			si-au incheiat executia */
			try {
				Tema2.latchMap.get(orderID).await();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			/* Se inchide fisierul din care s-a facut citirea */
			try {
				productsReader.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			/* Daca comanda are minim un produs, se va scrie in fisierul de output faptul ca
			aceasta a fost livrata */
			if (Nproducts != 0) {
				try {
					Tema2.ordersWriter.write(line + ",shipped\n");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			/* Se adauga un nou task in primul pool, de thread-uri de nivel 1 */
			Tema2.inQueue1.incrementAndGet();
			Tema2.tpe1.submit(new OrderRunnable());
		}

		/* Se actualizeaza numarul de task-uri ramase in pool, si in momentul in care
		acesta va deveni 0 se va opri pool-ul */
		int left = Tema2.inQueue1.decrementAndGet();
		if (left == 0) {
			/* Terminarea executiei tuturor task-urilor din primul pool implica si
			terminarea celor din al doilea pool, intrucat un task pentru o comanda
			asteapta finalizarea tuturor task-urilor produselor aferente acesteia */
			if (Tema2.inQueue2.get() == 0) {
				Tema2.tpe2.shutdown();
			}
			Tema2.tpe1.shutdown();
		}

	}
}
