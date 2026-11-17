package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Tunes on note blocks for the scenes. A tune is a list of notes, each a midi number and a length in sixteenths (0 for
 * a rest), played from a start tick at so many ticks to a sixteenth.
 */
final class Band {
	static final SoundEvent HARP = SoundEvents.NOTE_BLOCK_HARP.value();
	static final SoundEvent FLUTE = SoundEvents.NOTE_BLOCK_FLUTE.value();
	static final SoundEvent TRUMPET = SoundEvents.NOTE_BLOCK_FLUTE.value();
	static final SoundEvent BASS = SoundEvents.NOTE_BLOCK_BASS.value();
	static final SoundEvent BELL = SoundEvents.NOTE_BLOCK_BELL.value();
	static final SoundEvent CHIME = SoundEvents.NOTE_BLOCK_CHIME.value();
	static final SoundEvent BIT = SoundEvents.NOTE_BLOCK_BIT.value();
	static final SoundEvent GUITAR = SoundEvents.NOTE_BLOCK_GUITAR.value();
	static final SoundEvent PLING = SoundEvents.NOTE_BLOCK_PLING.value();
	static final SoundEvent DIDGERIDOO = SoundEvents.NOTE_BLOCK_DIDGERIDOO.value();
	static final SoundEvent DRUM = SoundEvents.NOTE_BLOCK_BASEDRUM.value();
	static final SoundEvent SNARE = SoundEvents.NOTE_BLOCK_SNARE.value();
	static final SoundEvent HAT = SoundEvents.NOTE_BLOCK_HAT.value();

	private Band() {
	}

	/** The note that starts this many ticks into a tune, or -1 for none (or a rest). */
	static int noteAt(int[][] tune, int since, float step) {
		if (since < 0) {
			return -1;
		}
		int units = 0;
		for (int[] note : tune) {
			int at = Math.round(units * step);
			if (at == since) {
				return note[0] > 0 ? note[0] : -1;
			}
			if (at > since) {
				return -1;
			}
			units += note[1];
		}
		return -1;
	}

	/** How many ticks a tune lasts. */
	static int length(int[][] tune, float step) {
		int units = 0;
		for (int[] note : tune) {
			units += note[1];
		}
		return Math.round(units * step);
	}

	/** The pitch of a note block for a midi note: harp, flute, trumpet, bell and the like are at 1.0 on F#4 (66), the bass on F#2 (42). */
	static float pitch(int midi, int base) {
		return (float) Math.pow(2.0, (midi - base) / 12.0);
	}

	/** Plays the tune's note of this tick, if there is one, on each of the given instruments. Returns the midi note or -1. */
	static int play(ClientLevel level, Vec3 at, int[][] tune, int since, float step, float volume, SoundEvent... instruments) {
		int midi = noteAt(tune, since, step);
		if (midi > 0) {
			for (int i = 0; i < instruments.length; i++) {
				// The first instrument leads, the others are quieter.
				note(level, at, instruments[i], midi, i == 0 ? volume : volume * 0.5F);
			}
		}
		return midi;
	}

	/** One note of an instrument, the bass two octaves lower than the others. */
	static void note(ClientLevel level, Vec3 at, SoundEvent instrument, int midi, float volume) {
		boolean low = instrument == BASS || instrument == DIDGERIDOO;
		Scene.sound(level, at, instrument, SoundSource.RECORDS, volume, pitch(midi, low ? 42 : 66));
	}

	/** A chord: every note of it on the instrument. */
	static void chord(ClientLevel level, Vec3 at, SoundEvent instrument, int[] notes, float volume) {
		for (int midi : notes) {
			note(level, at, instrument, midi, volume);
		}
	}
}
