/**
 * TLT - TieLessTris
 * Autori: Malacarne, Morelli, Tognetti
 * Sezione: 5CI
 */

package tielesstris.utils;

import java.io.IOException;

import tielesstris.GameSession;
import tielesstris.Player;
import tielesstris.Server;

public class PlayerListener implements Runnable {
        private Server serv;
		private Player player;
        private GameSession game;
        
        public PlayerListener(Player player, GameSession game) {
            this.player = player;
            this.game = game;
            serv.log("PlayerListener creato per " + player.getUsername());
        }
        
        @Override
        public void run() {
            String username = player.getUsername();
            serv.log("PlayerListener avviato per " + username);
            
            try {
                while (player.isConnected() && !game.isGameOver()) {
                    String message = player.receive();
                    if (message == null) {
                    	serv.log(username + " disconnesso (ricevuto null)");
                        break;
                    }
                    
                    serv.log("Ricevuto da " + username + ": \"" + message + "\"");
                    
                    if (message.startsWith("MOVE ")) {
                        String[] parts = message.split(" ");
                        if (parts.length == 3) {
                            try {
                                int row = Integer.parseInt(parts[1]);
                                int col = Integer.parseInt(parts[2]);
                                
                                serv.log(username + " tenta mossa in (" + row + "," + col + ")");
                                
                                // Processa la mossa
                                String result = game.processMove(player, row, col);
                                serv.log("Risultato mossa per " + username + ": " + result);
                                
                                // Invia il risultato SOLO al giocatore che ha mosso
                                if (!result.startsWith("ERROR")) {
                                    player.send(result);
                                    serv.log("Risultato inviato a " + username + ": " + result);
                                    
                                    // Se c'è un vincitore, invia a tutti
                                    if (result.startsWith("WINNER")) {
                                        broadcastWinner(game, result);
                                    }
                                } else {
                                    player.send(result);
                                }
                                
                            } catch (NumberFormatException e) {
                                String errorMsg = "ERROR Invalid move format";
                                player.send(errorMsg);
                                serv.log("Formato mossa non valido da " + username + ": " + message);
                            } catch (Exception e) {
                                String errorMsg = "ERROR " + e.getMessage();
                                player.send(errorMsg);
                                serv.log("Errore processando mossa: " + e.getMessage());
                            }
                        } else {
                            String errorMsg = "ERROR Invalid MOVE command format";
                            player.send(errorMsg);
                        }
                    } else if (message.equals("QUIT")) {
                    	serv.log(username + " ha inviato QUIT");
                        game.playerDisconnected(player);
                        break;
                    } else if (message.equals("BOARD?")) {
                        // Comando per richiedere lo stato attuale della board
                        game.broadcastBoard();
                    } else if (message.equals("PING")) {
                        player.send("PONG");
                    } else {
                    	serv.log("Comando non riconosciuto da " + username + ": " + message);
                        player.send("ERROR Unknown command");
                    }
                }
            } catch (IOException e) {
            	serv.log("IOException per " + username + ": " + e.getMessage());
                game.playerDisconnected(player);
            }
            
            serv.log("PlayerListener terminato per " + username);
        }
        
        private void broadcastWinner(GameSession game, String winResult) {
            Player playerX = game.getPlayerX();
            Player playerO = game.getPlayerO();
            
            if (playerX != null && playerX.isConnected()) {
                playerX.send(winResult);
                playerX.send("GAME_OVER");
                serv.log("Inviato WINNER e GAME_OVER a " + playerX.getUsername());
            }
            
            if (playerO != null && playerO.isConnected()) {
                playerO.send(winResult);
                playerO.send("GAME_OVER");
                serv.log("Inviato WINNER e GAME_OVER a " + playerO.getUsername());
            }
        }
    }