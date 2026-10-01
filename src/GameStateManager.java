public final class GameStateManager {
    public enum State {
        MAIN_MENU,
        PLAYING,
        GAME_OVER,
        VICTORY
    }

    private State currentState = State.MAIN_MENU;

    public State getCurrentState() {
        return currentState;
    }

    public void transitionTo(State nextState) {
        if (nextState == null) {
            throw new IllegalArgumentException("Game state cannot be null");
        }
        currentState = nextState;
    }

    public boolean isPlaying() {
        return currentState == State.PLAYING;
    }
}
