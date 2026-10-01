import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;

public final class GameAudio implements AutoCloseable {
    private static final float SAMPLE_RATE = 22050f;

    private final Clip music;
    private final Clip attack;
    private final Clip hit;
    private final Clip heal;
    private final Clip reward;

    public GameAudio() throws LineUnavailableException {
        Clip loadedMusic = null;
        Clip loadedAttack = null;
        Clip loadedHit = null;
        Clip loadedHeal = null;
        Clip loadedReward = null;
        try {
            loadedMusic = createClip(createMusic());
            loadedAttack = createClip(createSwing());
            loadedHit = createClip(createHit());
            loadedHeal = createClip(createHeal());
            loadedReward = createClip(createReward());
        } catch (LineUnavailableException | IOException | IllegalArgumentException exception) {
            closeClip(loadedMusic);
            closeClip(loadedAttack);
            closeClip(loadedHit);
            closeClip(loadedHeal);
            closeClip(loadedReward);
            if (exception instanceof LineUnavailableException) {
                throw (LineUnavailableException) exception;
            }
            throw new IllegalStateException("Unable to initialize generated game audio", exception);
        }
        music = loadedMusic;
        attack = loadedAttack;
        hit = loadedHit;
        heal = loadedHeal;
        reward = loadedReward;
        music.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void playAttack() {
        play(attack);
    }

    public void playHit() {
        play(hit);
    }

    public void playHeal() {
        play(heal);
    }

    public void playReward() {
        play(reward);
    }

    private Clip createClip(byte[] samples) throws LineUnavailableException, IOException {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        Clip clip = AudioSystem.getClip();
        clip.open(format, samples, 0, samples.length);
        return clip;
    }

    private byte[] createMusic() {
        int noteLength = 33075;
        int[] notes = {220, 261, 330, 294, 196, 247, 294, 261,
                220, 261, 330, 392, 349, 294, 247, 220};
        byte[] data = new byte[noteLength * notes.length * 2];
        int output = 0;
        for (int note : notes) {
            for (int i = 0; i < noteLength; i++) {
                double time = i / (double) SAMPLE_RATE;
                double fadeIn = Math.min(1.0, time / 0.04);
                double fadeOut = Math.min(1.0, (noteLength - i) / (SAMPLE_RATE * 0.08));
                double envelope = Math.min(fadeIn, fadeOut) * 0.10;
                double sample = Math.sin(2.0 * Math.PI * note * time)
                        + 0.28 * Math.sin(2.0 * Math.PI * note * 2.0 * time);
                output = writeSample(data, output, sample * envelope);
            }
        }
        return data;
    }

    private byte[] createSwing() {
        int length = 4851;
        byte[] data = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            double progress = i / (double) length;
            double envelope = (1.0 - progress) * (0.6 + 0.4 * Math.sin(Math.PI * progress));
            double frequency = 850.0 - 650.0 * progress;
            double noise = Math.sin(2.0 * Math.PI * frequency * i / SAMPLE_RATE) * 0.65
                    + Math.sin(2.0 * Math.PI * 73.0 * i / SAMPLE_RATE) * 0.35;
            writeSample(data, i * 2, noise * envelope * 0.32);
        }
        return data;
    }

    private byte[] createHit() {
        int length = 6615;
        byte[] data = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            double time = i / (double) SAMPLE_RATE;
            double envelope = Math.exp(-time * 24.0);
            double sample = Math.sin(2.0 * Math.PI * (115.0 - 35.0 * time) * time);
            writeSample(data, i * 2, sample * envelope * 0.48);
        }
        return data;
    }

    private byte[] createHeal() {
        return createTones(new int[] {523, 659, 784}, 0.34, 0.22);
    }

    private byte[] createReward() {
        return createTones(new int[] {659, 831, 988}, 0.42, 0.24);
    }

    private byte[] createTones(int[] frequencies, double duration, double volume) {
        int noteLength = (int) (SAMPLE_RATE * duration);
        byte[] data = new byte[noteLength * frequencies.length * 2];
        int output = 0;
        for (int frequency : frequencies) {
            for (int i = 0; i < noteLength; i++) {
                double time = i / (double) SAMPLE_RATE;
                double envelope = Math.min(1.0, time / 0.012)
                        * Math.max(0.0, 1.0 - i / (double) noteLength);
                output = writeSample(data, output,
                        Math.sin(2.0 * Math.PI * frequency * time) * envelope * volume);
            }
        }
        return data;
    }

    private int writeSample(byte[] data, int offset, double sample) {
        int value = (int) (Math.max(-1.0, Math.min(1.0, sample)) * 32767.0);
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
        return offset + 2;
    }

    private void play(Clip clip) {
        if (clip == null || !clip.isOpen()) {
            return;
        }
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }

    private void closeClip(Clip clip) {
        if (clip != null) {
            clip.close();
        }
    }

    @Override
    public void close() {
        music.stop();
        closeClip(music);
        closeClip(attack);
        closeClip(hit);
        closeClip(heal);
        closeClip(reward);
    }
}
