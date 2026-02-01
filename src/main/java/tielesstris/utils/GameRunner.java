/**
 * TLT - TieLessTris
 * Autori: Malacarne, Morelli, Tognetti
 * Sezione: 5CI
 */

package tielesstris.utils;

import tielesstris.GameSession;
import tielesstris.Player;
import tielesstris.Server;

public class GameRunner implements Runnable {
		private Server serv;
        private GameSession game;
        
        public GameRunner(GameSession game) {
            this.game = game;
            serv.log("GameRunner creato per partita");
        }
        
        @Override
        public void run() {
            Player playerX = game.getPlayerX();
            Player playerO = game.getPlayerO();
            
            if (playerX == null || playerO == null) {
            	serv.log("ERRORE: Uno dei giocatori è null in GameRunner");
                return;
            }
            
            serv.log("Partita in esecuzione: " + playerX.getUsername() + "(X) vs " + playerO.getUsername() + "(O)");
            
            // Avvia thread per ascoltare mosse
            Thread playerXListener = new Thread(new PlayerListener(playerX, game));
            Thread playerOListener = new Thread(new PlayerListener(playerO, game));
            
            playerXListener.setName("Listener-" + playerX.getUsername());
            playerOListener.setName("Listener-" + playerO.getUsername());
            
            playerXListener.start();
            playerOListener.start();
            
            serv.log("Thread listener avviati per entrambi i giocatori");
            
            try {
                playerXListener.join();
                serv.log("PlayerXListener terminato");
                playerOListener.join();
                serv.log("PlayerOListener terminato");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                serv.log("Thread partita interrotto");
            }
            
            synchronized (serv.activeGames) {
            	serv.activeGames.remove(game);
            }
            
            serv.log("Partita terminata tra " + playerX.getUsername() + " e " + playerO.getUsername());
        }
    }