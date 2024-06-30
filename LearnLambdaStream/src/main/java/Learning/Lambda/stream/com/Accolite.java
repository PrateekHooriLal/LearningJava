package Learning.Lambda.stream.com;

public class Accolite {

	public static void main(String[] args) {

		String data = "a,b,c,d e,f,g,h i,j,k,l m,n,o,p q,r,s,t x,y,z,g n,v,x,z,o";
		calculations(data.replaceAll(",", ""));

		// output:
		// [[a, e, i, m, q],
		// [b, f, j, n, r],
//		 [c, g, k, o, s]]		 
	}

	public static void calculations(String data) {

		String[] blockList = data.split(" ");

		for (int i = 0; i < blockList[i].length(); i++) {
			// System.out.print(blockList.length);

			for (int j = 0; j < blockList.length; j++) {
				// System.out.println("i=" + i + " j=" + j + " ");
				if (i <= blockList[j].length() - 1)
					System.out.print(blockList[j].charAt(i) + " ");
				else if (blockList[j].length() - 1 >= i)
					System.out.print(blockList[j].charAt(i) + " ");

			}
			System.out.println();
		}
	}

}
