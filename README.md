# clj-krach
## What is it?
Glue code to use [Leipzig](https://github.com/ctford/leipzig) with [Sonic Pi](https://github.com/sonic-pi-net/sonic-pi).
## How does it work
- start Sonic Pi
- start a repl in a project that has access to clj-krach
- write some krach flavored leipzig
- use the function krach to convert leipzigs edn data structures into Sonic Pi compatible ruby code and tell sonic pi to run it

## Krach flavored Leipzig?
Leipzig models music as a sequence of notes, each of which is a map. They are ordered by :time:

`[{:time 0
  :pitch 67
  :duration 2
  :part :melody}
 {:time 2
  :pitch 71
  :duration 2
  :part :melody}]`

To use more of Sonic Pis functionality you can extend this basic maps in several ways.

### basic scenarios
Every note must know when it is played. So this is the simplest legal note.
`(krach [{:time 0}])`
Without a pitch clj-krach ignores it and nothing is played.
`(krach [{:time 0 :pitch 67}])`
When we add a pitch we get a sound. 
- Since we don't wish for a special duration, the note is played for 1 second.
- Since we don't tell the note which part to play, it defaults to the beep synth as it would in Sonic Pi.
`(krach [{:time 0 
          :pitch 67 
          :duration 2 
          :part :piano}
         {:time 2
          :pitch 71
          :duration 2
          :part :mod_saw}])`
If we add a :part key with another keyword as a value, clj-krach assumes that we want to play the Sonic Pi synth with that name.

### options
Sonic Pi synth options can be added as a map under the :options key.
`(krach [{:time 0
          :pitch 67
          :duration 2
          :part :prophet
          :options {:attack 0.5
                    :release 1
                    :cutoff 50}}])`

### effects
Instead of a simple keyword the :part key can itself be a map with a :part key. A vector of effect descriptions can be added under the :fx key. 
`(krach [{:pitch 52
           :time 0
           :duration 2
           :part {:part :mod_saw
                  :fx [{:fx :distortion, :options {:distort 0.5}}
                       {:fx :nlpf, :options {:cutoff 75}}
                       {:fx :reverb, :options {:room 0.2}}]}}])`

### control
To control the sound of a single note during its lifetime add a :control key with a vector of time offsets and options. Notice that the slide options can be put in the main :options map as defaults or in the :options map of a control event to set them for this event only.
`(krach/krach [{:pitch 45
               :time 0
               :duration 12
               :part :subpulse
               :options {:note_slide 1}
               :control [{:offset 3, :options {:note 50}}
                         {:offset 6, :options {:note 40 :note_slide 0.1}}
                         {:offset 9, :options {:note 52}}]}])`

### playing a sample
Add a :type key to the map for non-synth commands. Supply options as usual.
`(krach [{:type :sample
           :time 0
           :part :loop_amen
           :options {:beat_stretch 4}}])`
A sample can not be controlled, but effects can be applyed.
`(krach [{:type :sample
           :time 0
           :part {:part "/path/to/my-sample.wav"
                  :fx [{:fx :distortion :options {:distort 0.9}}]}}])`

### playing midi
TODO

## Why is it called that?
Chris Ford called his Clojurescript wrapper for the Web Audio API cljs-bach. Krach is german for noise. Go figure.