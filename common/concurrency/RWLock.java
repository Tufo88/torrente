package concurrency;

import java.util.concurrent.Semaphore;

public class RWLock {
	// m actua como mutex
	Semaphore r, w, m;

	int dr = 0, dw = 0, nr = 0, nw = 0;

	public RWLock() {
		r = new Semaphore(0);
		w = new Semaphore(0);
		m = new Semaphore(1);
	}

	// realmente, si ocurriese una interrupcion, habria que rehacer el lock, porque
	// no sabemos en que estado se habrá quedado.
	// tampoco es razonable para esta implementacion tener en cuenta todos los
	// sitios donde
	// puede interrumpirse.
	// conlusion: se nos lia si ocurre una interrupcion, pero no es algo de facil
	// solucion
	public void requestWrite() throws InterruptedException {
		m.acquire();

		if (nr > 0 || nw > 0) {
			dw = dw + 1;
			m.release();
			w.acquire();
		}
		nw = nw + 1;
		m.release();
	}

	public void releaseWrite() throws InterruptedException {
		m.acquire();
		nw = nw - 1;
		if (dr > 0) {
			dr = dr - 1;
			r.release();
		} else if (dw > 0) {
			dw = dw - 1;
			w.release();
		} else
			m.release();
	}

	public void requestRead() throws InterruptedException {
		m.acquire();
		if (nw > 0) {
			dr = dr + 1;
			m.release();
			r.acquire();
		}
		nr = nr + 1;
		if (dr > 0) {
			dr = dr - 1;
			r.release();
		} else
			m.release();
	}

	public void releaseRead() throws InterruptedException {
		m.acquire();
		nr = nr - 1;
		if (nr == 0 && dw > 0) {
			dw = dw - 1;
			w.release();
		} else
			m.release();

	}

}
