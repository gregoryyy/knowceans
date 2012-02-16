package org.knowceans.corpus;

import org.knowceans.util.Vectors;

/**
 * compute distances between random variables (or rather, their distributions)
 * 
 * @author gregor
 * 
 */
public class ProbDistance {

	public static final double eps = 1e-12;

	/**
	 * basis
	 */
	// static double log2 = Math.log(2);
	// ICML Workshop: same definition as metric PLSA
	static double log2 = 1;

	public static void main(String[] args) {
		double[] x = new double[] { 0.4, 0.1, 0.2, 0.2, 0.1 };
		double[] y = new double[] { 0.1, 0.2, 0.6, 0.0, 0.1 };
		double[][] pxy = new double[][] { x, y };

		System.out.println(String.format(
				"KL = %f, reverse KL = %f, JS = %f, H2 = %f",
				klDivergence(x, y), klDivergence(y, x), jsDivergence(x, y),
				hsqDistance(x, y)));

		pxy = posterior(pxy, new double[] { 0.5, 0.5 });
		System.out.println(Vectors.print(pxy));

	}

	/**
	 * compute the JS-Divergence between distributions x and y
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static double jsDivergence(double[] x, double[] y) {
		double[] m = Vectors.copy(x);
		Vectors.add(m, y);
		Vectors.mult(m, .5);
		double d = 0;
		d += klDivergence(x, m);
		d += klDivergence(y, m);
		return d * 0.5;
	}

	/**
	 * computes the KL-Divergence between distributions x and y, using the
	 * natural logarithm
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static double klDivergence(double[] x, double[] y) {
		double d = 0;
		for (int i = 0; i < x.length; i++) {
			d += x[i] * Math.log(x[i] / (y[i] + eps) + eps);
			// d += x[i] * Math.log(x[i] / (y[i]));
		}
		if (d < 0) {
			// compensate for regularisation
			d = 0;
		}
		return d;
	}

	/**
	 * computes the squared Hellinger distance between distributions x and y.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static double hsqDistance(double[] x, double[] y) {
		double d = 0;
		for (int i = 0; i < x.length; i++) {
			double u = Math.sqrt(x[i]) - Math.sqrt(y[i]);
			d += u * u;
		}
		return d * 0.5;
	}

	/**
	 * computes the posterior from the likelihood pxy and prior px.
	 * 
	 * @param pxy p(y|x)
	 * @param px p(x)
	 * @return p(x|y)
	 */
	public static double[][] posterior(double[][] pxy, double[] px) {
		double[][] pyx = new double[pxy[0].length][pxy.length];
		for (int y = 0; y < pyx.length; y++) {
			double den = 0;
			for (int x = 0; x < pxy.length; x++) {
				pyx[y][x] = pxy[x][y] * px[x];
				den += pyx[y][x];
			}
			for (int x = 0; x < pxy.length; x++) {
				pyx[y][x] /= den;
			}
		}
		return pyx;
	}

	/**
	 * computes the entropy of the joint distribution hxy
	 * 
	 * @param pxy
	 * @return H(x, y)
	 */
	public static double entropy(double[][] pxy) {
		double h = 0;
		for (int x = 0; x < pxy.length; x++) {
			h += entropy(pxy[x]);
		}
		return h;
	}

	/**
	 * entropy of the distribution
	 * 
	 * @param px
	 * @return H(x)
	 */
	public static double entropy(double[] px) {

		double h = 0;
		for (int x = 0; x < px.length; x++) {
			// 0 log 0 = 0
			if (px[x] == 0)
				continue;
			h -= px[x] * mylog(px[x]);
		}

		return h;
	}

	/**
	 * convert count statistics to probability
	 * 
	 * @param nc
	 * @return
	 */
	public static double[] count2prob(int[] nc) {
		double sum = Vectors.sum(nc);
		double[] p = new double[nc.length];
		for (int i = 0; i < nc.length; i++) {
			p[i] = nc[i] / sum;
		}
		return p;
	}

	/**
	 * represents the variation of information distance and contains the
	 * quantities related to its computation: mutual information, marginal and
	 * joint entropies and the normalised VI distance, VI(x||y)/H(x,y), which is
	 * in [0,1]
	 * 
	 * @author gregor
	 * 
	 */
	public static class ViDistance {
		/** VI(x||y) */
		public double vixy;
		/** H(x) */
		public double hx;
		/** H(y) */
		public double hy;
		/** I(x,y) */
		public double ixy;
		/** H(x,y) */
		public double hxy;
		/** VI(x||y) / H(x, y) in [0, 1] */
		public double vixynorm;

		@Override
		public String toString() {
			return String.format(
					"variation of information:\nVI = %f\nI = %f\nVI/H = %f ",
					vixy, ixy, vixynorm);
		}
	}

	/**
	 * "Meila-metric" for a priori and a posteriori relationships.
	 * <p>
	 * D(x, y) = H(x) + H(y) - 2 I(x, y) with entropy H(X) = - sum p(x) log p(x)
	 * and the KL divergence between the x,y considered independent and the
	 * actual joint distribution I(x, y) = KL( p(x,y) || p(x) p(y) )
	 * 
	 * @param px p(x)
	 * @param py p(y)
	 * @param pxy p(x, y)
	 * @param ViDistance representation, where the vi.vixy is the VI distance
	 *        proper and vi.vixynorm is the normalised version VI(x||y)/H(x, y)
	 */
	public static ViDistance viDistance(double[] px, double py[], double[][] pxy) {

		double hx = entropy(px);
		double hy = entropy(py);
		double hxy = entropy(pxy);

		// mutual information
		double ixy = hx + hy - hxy;

		// calculate distance

		ViDistance d = new ViDistance();
		d.vixy = hx + hy - 2 * ixy;
		d.hx = hx;
		d.hy = hy;
		d.ixy = ixy;
		d.hxy = hxy;
		d.vixynorm = d.vixy / hxy;

		return d;
	}

	/**
	 * calculate mutual info for the two clusterings if pjoint is known.
	 * 
	 * @param px p(x)
	 * @param py p(y)
	 * @param pxy p(x, y)
	 * @return I(x ; y) = H(x) + H(y) - H(x, y)
	 */
	public static double mutualInfo(double[] px, double[] py, double[][] pxy) {

		return entropy(px) + entropy(py) - entropy(pxy);
	}

	/**
	 * calculate joint probability for the two clusterings.
	 * 
	 * @param pmx p(x|m) distributions of x over several contexts m (e.g.,
	 *        topics
	 * @param pmy p(y|m) distributions of y over several contexts m (e.g.,
	 *        categories
	 * 
	 * @return pxy[x = cat][y = topic]
	 */
	public static double[][] pxy(double[][] pmx, double[][] pmy) {

		int X = pmx[0].length;
		int Y = pmy[0].length;
		double[][] pxy = new double[X][Y];
		double norm = 0;

		for (int m = 0; m < pxy.length; m++) {
			for (int x = 0; x < X; x++) {
				for (int y = 0; y < Y; y++) {
					double pmxy = pmx[m][x] * pmy[m][y];
					pxy[x][y] += pmxy;
					norm += pmxy;
				}
			}
		}
		// normalise
		norm = 1. / norm;
		for (int x = 0; x < X; x++) {
			Vectors.mult(pxy[x], norm);
		}

		return pxy;

	}

	public static double mylog(double arg) {
		return Math.log(arg) / log2;
	}
}
