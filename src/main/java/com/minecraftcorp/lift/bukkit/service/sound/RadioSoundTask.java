package com.minecraftcorp.lift.bukkit.service.sound;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.entity.Entity;

import com.minecraftcorp.lift.bukkit.model.BukkitElevator;
import com.xxmicloxx.NoteBlockAPI.model.FadeType;
import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.Fade;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;

public class RadioSoundTask extends SoundTask {

	private static final Fade RADIO_FADE = new Fade(FadeType.LINEAR, 20);
	protected static Song[] songs;
	private final RadioSongPlayer radio;

	public RadioSoundTask(BukkitElevator elevator) {
		super(elevator);
		radio = new RadioSongPlayer(new Playlist(songs));
		radio.setVolume((byte) volume);
		radio.setRandom(true);
		startRadio(elevator.getPassengers());
	}

	@Override
	public void run() {
		List<UUID> uuidsInElevator = Stream.concat(elevator.getPassengers().stream(), elevator.getFreezers().stream())
				.map(Entity::getUniqueId)
				.collect(Collectors.toList());
		radio.getPlayerUUIDs()
				.stream()
				.filter(uuid -> !uuidsInElevator.contains(uuid))
				.forEach(radio::removePlayer);
	}

	@Override
	public void cancel() throws IllegalStateException {
		plugin.logDebug("Cancelled SoundTask");
		stopRadio(elevator.getPassengers());
		super.cancel();
	}

	private void startRadio(Set<Entity> entities) {
		filterPlayers(entities).forEach(radio::addPlayer);
		radio.setPlaying(true, RADIO_FADE);
		int randomIndex = ThreadLocalRandom.current()
				.nextInt(radio.getPlaylist().getSongList().size());

		radio.playSong(randomIndex);
		plugin.logDebug("Playing " + radio.getSong().getPath().getName());
	}

	private void stopRadio(Set<Entity> entities) {
		filterPlayers(entities).forEach(radio::removePlayer);
		radio.setPlaying(false, RADIO_FADE);
	}

	public static void reload() {
		songs = plugin.getMusicFiles()
				.stream()
				.map(NBSDecoder::parse)
				.toArray(Song[]::new);
	}
}
