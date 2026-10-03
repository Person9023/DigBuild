package com.abmstudios.DigBuild;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusicManager {

    private final List<Music> tracks = new ArrayList<>();

    private int currentTrack = -1;

    private float volume = 0.5f;

    public MusicManager() {

        tracks.add(Gdx.audio.newMusic(
            Gdx.files.internal("music/music1.mp3")
        ));

        tracks.add(Gdx.audio.newMusic(
            Gdx.files.internal("music/music2.mp3")
        ));

        tracks.add(Gdx.audio.newMusic(
            Gdx.files.internal("music/music3.mp3")
        ));

        tracks.add(Gdx.audio.newMusic(
            Gdx.files.internal("music/music4.mp3")
        ));

        for (Music music : tracks) {
            music.setVolume(volume);

            music.setOnCompletionListener(completedMusic -> {
                playNextTrack();
            });
        }

        playRandomTrack();
    }

    private void playRandomTrack() {

        if (tracks.isEmpty()) {
            return;
        }

        int nextTrack;

        do {
            nextTrack = (int)(Math.random() * tracks.size());
        } while (tracks.size() > 1 && nextTrack == currentTrack);

        currentTrack = nextTrack;

        Music music = tracks.get(currentTrack);

        music.setPosition(0f);
        music.play();
    }

    private void playNextTrack() {

        if (tracks.isEmpty()) {
            return;
        }

        int nextTrack;

        do {
            nextTrack = (int)(Math.random() * tracks.size());
        } while (tracks.size() > 1 && nextTrack == currentTrack);

        currentTrack = nextTrack;

        Music music = tracks.get(currentTrack);

        music.setPosition(0f);
        music.play();
    }

    public void setVolume(float volume) {

        this.volume = Math.max(0f, Math.min(1f, volume));

        for (Music music : tracks) {
            music.setVolume(this.volume);
        }
    }

    public void dispose() {

        for (Music music : tracks) {
            music.dispose();
        }

        tracks.clear();
    }
}
