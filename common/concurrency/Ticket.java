package concurrency;

import java.util.concurrent.atomic.AtomicInteger;

//como lo usamos para casos en los que es practicamente imposible que llegue al MAX_INTEGER, 
//no realizamos el modulo para evitar el overflow
public class Ticket {
	volatile AtomicInteger turn;
	volatile int next;

	public Ticket() {
		turn = new AtomicInteger(0);
		next = 0;
	}

	public void getLock() {
		int my_turn = turn.getAndIncrement();
		while (next != my_turn)
			;
	}

	public void releaseLock() {
		next++;
	}

}