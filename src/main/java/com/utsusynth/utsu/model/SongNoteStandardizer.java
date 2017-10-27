package com.utsusynth.utsu.model;

import com.google.common.base.Optional;
import com.utsusynth.utsu.model.voicebank.LyricConfig;
import com.utsusynth.utsu.model.voicebank.Voicebank;

/** Standardizes a song note and prepares it for rendering. */
public class SongNoteStandardizer {

	void standardize(SongNote note) {
		// Ensure note duration is a multiple of smallest quantization.
		// Enforce pitchbend size/location limits.
	}

	void standardizeInContext(
			Optional<SongNote> prev,
			SongNote note,
			Optional<SongNote> next,
			Voicebank voicebank) {
		double realPreutter = 0;
		double realDuration = note.getDuration();
		double realOverlap = 0;
		double autoStartPoint = 0;
		Optional<LyricConfig> config = voicebank.getLyricConfig(note.getLyric());
		if (config.isPresent()) {
			// Cap the preutterance at start of prev note or start of track.
			realPreutter = Math.min(config.get().getPreutterance(), note.getDelta());
			realOverlap = config.get().getOverlap();

			// Check correction factor.
			if (prev.isPresent()) {
				double maxLength = note.getDelta() - (prev.get().getDuration() / 2);
				if (realPreutter - realOverlap > maxLength) {
					double correctionFactor = maxLength / (realPreutter - realOverlap);
					double oldPreutter = realPreutter;
					realPreutter *= correctionFactor;
					realOverlap *= correctionFactor;
					autoStartPoint = oldPreutter - realPreutter;
				}
			}

			// TODO: Confirm envelope not bigger than note length.
			// Adjust the envelopes to match overlap.
			note.setFadeIn(Math.max(note.getFadeIn(), realOverlap));
			// TODO: Make this isTouching instead.
			// Case where there is an adjacent next node.
			if (next.isPresent() && note.getDuration() == note.getLength()) {
				note.setFadeOut(next.get().getFadeIn());
			} else {
				note.setFadeOut(35); // Default fade out.
			}

			realDuration = getAdjustedLength(voicebank, note, realPreutter, next);
		}

		// Set overlap.
		note.setRealPreutter(realPreutter);
		note.setRealDuration(realDuration);
		note.setAutoStartPoint(autoStartPoint);
	}

	// Find length of a note taking into account preutterance and overlap, but not tempo.
	private double getAdjustedLength(
			Voicebank voicebank,
			SongNote cur,
			double realPreutterance,
			Optional<SongNote> next) {
		// Increase length by this note's preutterance.
		double noteLength = cur.getDuration() + realPreutterance;

		// Decrease length by next note's preutterance.
		if (!next.isPresent()) {
			return noteLength;
		}

		Optional<LyricConfig> nextConfig = voicebank.getLyricConfig(next.get().getLyric());
		if (!nextConfig.isPresent()) {
			// Ignore next note if it has an invalid lyric.
			return noteLength;
		}

		// Expect next preutterance to be set.
		double nextPreutter = next.get().getRealPreutter();
		if (nextPreutter + cur.getDuration() < cur.getLength()) {
			// Ignore next note if it doesn't touch current note.
			return noteLength;
		}

		double encroachingPreutter = nextPreutter + cur.getDuration() - cur.getLength();
		noteLength -= encroachingPreutter;

		// Increase length by next note's overlap.
		double nextOverlap = Math.min(nextConfig.get().getOverlap(), next.get().getFadeIn());
		double nextBoundedOverlap = Math.max(0, Math.min(nextOverlap, next.get().getDuration()));
		noteLength += nextBoundedOverlap;

		return noteLength;
	}
}
