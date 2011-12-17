package fundfollowing;

public class CIK implements Comparable<CIK> {
	// Central Index Key used by SEC to identify individuals and companies
	private final String cik;

	public CIK(String c) {
		if (!isInteger(c) || c.length() != 10)
			throw new IllegalArgumentException();
		cik = c;
	}

	private boolean isInteger(String in) {
		try {
			Integer.parseInt(in);
		} catch (NumberFormatException ex) {
			return false;
		}
		return true;
	}

	public String getCIK() {
		return cik;
	}

	@Override
	public String toString() {
		return cik;
	}

	public int compareTo(CIK arg0) {
		return cik.compareTo(arg0.toString());
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof CIK) {
			CIK x = (CIK) arg0;
			return cik.equalsIgnoreCase(x.getCIK());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return cik.hashCode();
	}
}
