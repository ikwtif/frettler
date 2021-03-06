/*
 * Copyright (C) 2020 Philip Whiles
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package me.flotsam.frettler.view;

import static java.lang.System.out;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.flotsam.frettler.engine.Chord;
import me.flotsam.frettler.engine.Note;
import me.flotsam.frettler.engine.Scale;
import me.flotsam.frettler.engine.ScaleInterval;
import me.flotsam.frettler.engine.ScaleNote;
import me.flotsam.frettler.instrument.Fret;
import me.flotsam.frettler.instrument.FrettedInstrument;

/**
 * Shows a chord fingering diagram, a chord arpeggio or a scale, using a vertical representation,
 * similar to standard guitar chord diagrams.
 * 
 * @author philwhiles
 *
 */
public class VerticalView {

  private Options defaultOptions = new Options(false, true);
  private static final List<Integer> inlays = Arrays.asList(1, 3, 5, 7, 9, 12, 15, 17, 19, 21, 23);
  private FrettedInstrument instrument;

  public VerticalView(FrettedInstrument instrument) {
    this.instrument = instrument;
  }


  public void showChord(Chord chord) {
    showChord(chord, defaultOptions);
  }

  public void showChord(Chord chord, Options options) {
    List<ChordFret> chordFrets = new ArrayList<>();

    out.println();
    out.println(StringUtils.center(chord.getTitle(), 30));
    out.println(StringUtils.center("(" + instrument.getLabel() + " ["
        + instrument.getStringNotes().stream().map(Note::name).collect(Collectors.joining(","))
        + "])", 30));
    out.println();

    List<List<Fret>> strings = instrument.getFretsByString();

    Fret tonic = null;
    List<List<ChordFret>> byString = new ArrayList<>();

    // work thru the notes in the chord and find candidates on each string
    for (ScaleNote chordScaleNote : chord.getChordNotes()) {
      int stringNum = 0;
      
      // go through all strings looking for the chord note candidate in each
      for (List<Fret> stringFrets : strings) {
        List<ChordFret> stringsCandidates = null;
        // first chord note thru we won;t have created the strings candidates list
        if (stringNum >= byString.size()) {
          stringsCandidates = new ArrayList<>();
          byString.add(stringsCandidates);
        } else {
          stringsCandidates = byString.get(stringNum);
        }
        // once the first note in the chord has been done, we don't want subsequent chord note
        // candidates on strings lower than the tonic string
        if (!(tonic != null && stringNum <= tonic.getStringNum())) {

          // go down the string looking for chord note candidates
          for (Fret fret : stringFrets) {

            // only creating fingerings a human can play in first 4 frets!
            if (fret.getFretNum() > 4) {
              break;
            }
            // found a candidate
            if (chordScaleNote.getNote() == fret.getNote()) {
              stringsCandidates.add(new ChordFret(fret, chordScaleNote.getInterval().get()));
              // tonic by default the first note in chord
              if (tonic == null) {
                tonic = fret;
              }
              break;
            }
          }
        }
        stringNum++;
      }
    }

    // sort the candidates by fret num - they were recorded in chord order ie C first, then G...
    for (List<ChordFret> stringChordFrets : byString) {
      stringChordFrets.sort(Comparator.comparing(cf -> cf.getFret().getFretNum()));
    }

    
    ChordFret [][] candidates = new ChordFret[strings.size()][5];
    for (int stringNum = 0; stringNum < strings.size(); stringNum++) {
      for (int fretNum=0; fretNum<5;fretNum++) {
        final int f = fretNum;
        final int s = stringNum;
       candidates[stringNum][fretNum] = byString.stream().flatMap(List::stream).filter(cf->cf.getFret().getFretNum() == f && cf.getFret().getStringNum() == s).findFirst().orElse(null);
      }
    }
    
    // now we have the fret sorted string candidates


    List<Integer> stringCandidateCounts =
        byString.stream().map(sl -> sl.size()).collect(Collectors.toList());
    System.err.println("string candidate counts : " + stringCandidateCounts);

    // Map<Note, Long> noteCandidateCounts =
    // stringCandidates.stream().flatMap(Collection::stream).collect(Collectors.groupingBy(e ->
    // ((ChordFret)e).getFret().getNote(), Collectors.counting()));
    Map<Note, List<ChordFret>> noteCandidates = new HashMap<>();
    for (ChordFret chordFret : byString.stream().flatMap(List::stream)
        .collect(Collectors.toList())) {
      List<ChordFret> notesChordFrets = noteCandidates.get(chordFret.getFret().getNote());
      if (notesChordFrets == null) {
        notesChordFrets = new ArrayList<>();
        noteCandidates.put(chordFret.getFret().getNote(), notesChordFrets);
      }
      notesChordFrets.add(chordFret);
    }

    for (Map.Entry<Note, List<ChordFret>> entry : noteCandidates.entrySet()) {
      for (ChordFret cf : entry.getValue()) {
        System.out.println(cf.getFret().getNote() + " s" + cf.getFret().getStringNum() + " f"
            + cf.getFret().getFretNum());
      }
    }


    // // for each string
    // boolean tonicFound=false;
    // for (int stringNum = 0; stringNum < stringCandidates.size(); stringNum++) {
    // List<ChordFret> stringsCandidates = stringCandidates.get(stringNum);
    //
    //
    //
    //
    //
    // // find the chosen candidate from the last string at the end of the tone list
    // ChordFret prevTone = null;
    // if (chordFrets.size() > 0) {
    // prevTone = chordFrets.get(chordFrets.size() - 1);
    // }
    // ChordFret tone = null;
    // // now go thru this strings candidates
    // for (ChordFret thisChordTone : stringsCandidates) {
    // if (prevTone != null) {
    // // was the prev string chosen tone identical to this one? don't want repeats
    // // if same note and occtave move on to next string candidate
    // if (!prevTone.getFret().equalsTonally(thisChordTone.getFret())) {
    // tone = thisChordTone;
    // break;
    // }
    // } else {
    // // was no prev string tone chosen either because we are on string 0
    // // or none suitable (too low down on fretboard) on prev string
    // tone = thisChordTone;
    // break;
    // }
    // }
    // // after process of elimination did we find a suitable tone?
    // if (tone != null) {
    // chordFrets.add(tone);
    // } else {
    // chordFrets.add(new ChordFret(new Fret(-1, null, -1, stringNum, null, 0), ScaleInterval.P1));
    // }
    // }



    // for each string
    for (int stringNum = 0; stringNum < byString.size(); stringNum++) {
      // System.err.println("Candidates: " + stringCandidates.get(stringNum));

      List<ChordFret> stringsCandidates = byString.get(stringNum);
      // find the chosen candidate from the last string at the end of the tone list
      ChordFret prevTone = null;
      if (chordFrets.size() > 0) {
        prevTone = chordFrets.get(chordFrets.size() - 1);
      }
      ChordFret tone = null;
      // now go thru this strings candidates
      for (ChordFret thisChordTone : stringsCandidates) {
        if (prevTone != null) {
          // was the prev string chosen tone identical to this one? don't want repeats
          // if same note and occtave move on to next string candidate
          if (!prevTone.getFret().equalsTonally(thisChordTone.getFret())) {
            tone = thisChordTone;
            break;
          }
        } else {
          // was no prev string tone chosen either because we are on string 0
          // or none suitable (too low down on fretboard) on prev string
          tone = thisChordTone;
          break;
        }
      }
      // after process of elimination did we find a suitable tone?
      if (tone != null) {
        chordFrets.add(tone);
      } else {
        chordFrets.add(new ChordFret(new Fret(-1, null, -1, stringNum, null, 0), ScaleInterval.P1));
      }
    }



    display(chordFrets, options);
  }

  public void showArpeggio(Chord chord) {
    showArpeggio(chord, defaultOptions);
  }

  public void showArpeggio(Chord chord, Options options) {
    List<ChordFret> chordFret = new ArrayList<>();

    out.println();
    out.println(StringUtils.center(chord.getTitle(), 30));
    out.println(StringUtils.center("(" + instrument.getLabel() + " ["
        + instrument.getStringNotes().stream().map(Note::name).collect(Collectors.joining(","))
        + "])", 30));
    out.println();

    List<List<Fret>> fretboardFrets = instrument.getFretsByFret();

    for (List<Fret> fretFrets : fretboardFrets) {
      for (Fret fret : fretFrets) {
        Optional<ScaleNote> chordScaleNoteForFret = chord.getChordNotes().stream()
            .filter(cn -> fret.getNote().equals(cn.getNote())).findAny();
        if (chordScaleNoteForFret.isPresent()) {
          chordFret.add(new ChordFret(fret, chordScaleNoteForFret.get().getInterval().get()));
        }
      }
    }
    display(chordFret, options);
  }

  public void showScale(Scale scale) {
    showScale(scale, defaultOptions);
  }

  public void showScale(Scale scale, Options options) {
    List<ChordFret> tones = new ArrayList<>();

    out.println();
    out.println(StringUtils.center(scale.getTitle(), 30));
    out.println(StringUtils.center("(" + instrument.getLabel() + " ["
        + instrument.getStringNotes().stream().map(Note::name).collect(Collectors.joining(","))
        + "])", 30));
    out.println();

    List<List<Fret>> fretboardFrets = instrument.getFretsByFret();

    for (List<Fret> fretFrets : fretboardFrets) {
      for (Fret fret : fretFrets) {
        if (fret.getNote() == null) {
          continue; // must be fret 1-5 of the 5th string on banjo
        }
        Optional<ScaleNote> scaleNoteForFret = scale.getScaleNotes().stream()
            .filter(sn -> fret.getNote().equals(sn.getNote())).findAny();
        if (scaleNoteForFret.isPresent()) {
          tones.add(new ChordFret(fret, scaleNoteForFret.get().getInterval().get()));
        }
      }
    }
    display(tones, options);
  }

  private void display(List<ChordFret> chordFrets, Options options) {
    ColourMap colourMap = new ColourMap();
    List<List<Fret>> fretboardFrets = instrument.getFretsByFret();
    List<Integer> deadStrings = new ArrayList<>();
    int lowestFret = chordFrets.stream()
        .max(Comparator.comparingInt(ct -> Integer.valueOf(ct.getFret().getFretNum()))).get()
        .getFret().getFretNum();

    for (ChordFret chordFret : chordFrets) {
      if (chordFret.getFret().getNote() == null) {
        deadStrings.add(chordFret.getFret().getStringNum());
      }
    }

    int fretNum = 0;
    for (List<Fret> frets : fretboardFrets) {
      String inlay = inlays.contains(fretNum) ? String.format(" %-2s ", fretNum) : "    ";
      out.print(inlay);
      String sep = (fretNum == 0) ? "" : "┃";
      int stringNum = 0;
      for (Fret fret : frets) {
        String ldr = (stringNum == 0 && fretNum != 0) ? "┃" : (fretNum == 0 ? " " : "");

        Optional<ChordFret> chordFret =
            chordFrets.stream().filter(ct -> fret == ct.getFret()).findAny();

        if (chordFret.isPresent()) {
          String fretStr = null;
          if (options.isIntervals()) {
            fretStr = chordFret.get().getInterval().getLabel();
          } else {
            fretStr = chordFret.get().getFret().getNote().getLabel();
          }
          if (options.isColour()) {
            Colour col = colourMap.get(fret.getNote());
            out.print(String.format("%s %s%-2s%s%s", ldr, col, fretStr, Colour.RESET, sep));

          } else {
            out.print(String.format("%s %-2s%s", ldr, fretStr, sep));
          }
        } else {
          String mark = " ";
          if (fretNum == 0 && deadStrings.contains(stringNum)) {
            mark = "X";
          }
          out.print(String.format("%s %s %s", ldr, mark, sep));
        }
        stringNum++;
      }
      out.println(inlay);
      if (fretNum >= 5 && fretNum >= lowestFret + 1) {
        break;
      }
      if (fretNum == 0) {
        out.println(createFretLine("┏", "┳", "┓"));
      } else if (fretNum < instrument.getFrets()) {
        out.println(createFretLine("┣", "╋", "┫"));
      }
      fretNum++;
    }
    out.println(createFretLine("┗", "┻", "┛"));
    out.println();
    out.println();
  }

  private String createFretLine(String begin, String mid, String end) {
    int strings = instrument.getStringCount();
    StringBuilder sb = new StringBuilder("    ");
    sb.append(begin);
    for (int n = 0; n < strings; n++) {
      sb.append("━━━");
      if (n < strings - 1) {
        sb.append(mid);
      } else {
        sb.append(end);
      }
    }
    return sb.toString();
  }

  @Data
  @AllArgsConstructor
  public class Options {
    private final boolean intervals;
    private final boolean colour;
  }


  @Data
  @RequiredArgsConstructor
  class ChordFret {

    @NonNull
    private final Fret fret;

    @NonNull
    private final ScaleInterval interval;

    private int weighting;
  }

}
