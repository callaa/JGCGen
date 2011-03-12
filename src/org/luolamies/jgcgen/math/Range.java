package org.luolamies.jgcgen.math;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Range implements Iterable<Double> {
	private final double from, to, step;
	
	public Range(double from, double to, double step) {
		if(step==0)
			throw new IllegalArgumentException("Step must be nonzero!");
		this.from = from;
		this.to = to;
		step = Math.abs(step);
		if(from<to)
			this.step = step;
		else
			this.step = -step;
	}

	public Iterator<Double> iterator() {
		return new Iterator<Double>() {
			private double pos = from;
			public boolean hasNext() {
				return (step<0 && pos>=to) || (step>0 && pos<=to);
			}

			public Double next() {
				double p = pos;
				if((step<0 && p<to) || (step>0 && p>to))
					throw new NoSuchElementException();
				pos += step;
				return p;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
