package com.utsusynth.utsu.view.note.portamento;

import java.util.ArrayList;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.quantize.QuantizedNote;
import com.utsusynth.utsu.common.quantize.QuantizedPortamento;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.view.note.TrackNote;

public class TrackPortamentoFactory {
	private final CurveFactory curveFactory;

	@Inject
	public TrackPortamentoFactory(CurveFactory curveFactory) {
		this.curveFactory = curveFactory;
	}

	public TrackPortamento createPortamento(
			TrackNote note,
			QuantizedPortamento qPortamento,
			TrackPortamentoCallback callback) {
		QuantizedNote qNote = note.getQuantizedNote();
		int noteQuantSize = Quantizer.COL_WIDTH / qNote.getQuantization();
		int pitchQuantSize = Quantizer.COL_WIDTH / QuantizedPortamento.QUANTIZATION;
		double finalY = (note.getRow() + .5) * Quantizer.ROW_HEIGHT;

		int curX = (qNote.getStart() * noteQuantSize) + (qPortamento.getStart() * pitchQuantSize);
		double curY =
				(PitchUtils.pitchToRowNum(qPortamento.getPrevPitch()) + .5) * Quantizer.ROW_HEIGHT;

		ArrayList<Curve> pitchCurves = new ArrayList<>();
		for (int i = 0; i < qPortamento.getNumWidths(); i++) {
			int tempX = curX;
			curX += qPortamento.getWidth(i) * pitchQuantSize;
			double tempY = curY;
			if (i == qPortamento.getNumWidths() - 1) {
				curY = finalY;
			} else {
				curY = finalY - (qPortamento.getShift(i) / 10) * Quantizer.ROW_HEIGHT;
			}
			String type = qPortamento.getCurve(i);
			pitchCurves.add(curveFactory.createCurve(tempX, tempY, curX, curY, type));
		}
		return new TrackPortamento(pitchCurves, callback, curveFactory);
	}
}
