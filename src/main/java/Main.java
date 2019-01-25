class Main {
	public static void main(String[] args) {
		while (true) {
			Game game = new Game();
			boolean playAgain = game.playGame();
			game.dispose();
			if (!playAgain) {
				break;
			}
		}
	}
}
