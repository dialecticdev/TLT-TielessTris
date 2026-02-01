/**
 * TLT - TieLessTris
 * Autori: Malacarne, Morelli, Tognetti
 * Sezione: 5CI
 */

package tielesstris.utils;

public class Move {
        public int row;
		public int col;
        public char symbol;
        public long timestamp;
        
        public Move(int r, int c, char s) {
            row = r; col = c; symbol = s;
            timestamp = System.currentTimeMillis();
        }
}