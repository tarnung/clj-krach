(ns clj-krach.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [overtone.osc :as osc]
            [clj-krach.melody :as k]))

(comment 
  {:type keyword?
   :time pos-int?
   :pitch pos-int?
   :duration pos-int?
   :part (or keyword?
             {:part keyword?
              :fn ifn?
              :defaults {}
              :fx [{:fx keyword?
                    :options {}}]})
   :options {}
   :control [{:type :control
              :sym symbol?
              :offset pos-int?
              :options {}}]})

;; ----------

(defonce sonic-pi-client (atom nil))

(defn init []
  (let [log (slurp (str (System/getProperty "user.home") "/.sonic-pi/log/gui.log"))
        gui-port (Integer/parseInt (second (re-find #"Server listen to gui port\s+(\d+)" log)))
        #_#_server-port (Integer/parseInt (second (re-find #"GUI listen to server port\s+(\d+)" log)))
        client (osc/osc-client "127.0.0.1" gui-port)]
    (reset! sonic-pi-client client)))

(defn stop []
  (osc/osc-send @sonic-pi-client "/stop-all-jobs" 10))

(defn send-code [code]
  (osc/osc-send @sonic-pi-client "/run-code" 10 code))

(defn spit-and-run [code]
  (spit "temp.rb" code)
  (send-code (str "run_file \""  (string/replace (.getAbsolutePath (io/file "temp.rb")) "\\" "/") "\"")))

;; ----------

(defn resolve-part [note]
  (let [f (get-in note [:part :fn])]
   (cond (-> note :part keyword?) note
         (-> note :part string?) note
         (-> note :part map?) (cond-> note
                                true (assoc :part (-> note :part :part))
                                f f)
         :else note)))

(defn resolve-control [{:keys [options] {:keys [defaults]} :part :as note}]
  (let [sym (gensym)]
    (->> note
         :control
         (map #(assoc %
                      :type :control
                      :sym sym
                      :time (+ (:time note)
                               (:offset %))))
         (map (fn [c] (update c :options #(merge defaults options %))))
         (concat [(assoc note :sym sym)]))))

(defn synth-to-code 
  [{:keys [pitch duration part sym options]
    :or {part :beep}}]
  (when-not (or (not pitch) (zero? pitch) (= :r pitch))
    (str (when sym (str sym "=")) "synth " part
         ", note: " (k/format-nr pitch)
         (when (and duration (not (:release options))) 
           (str ", release: " (k/format-nr duration))))))

(defn control-to-code [{:keys [sym]}]
  (str "control " sym))

(defn sample-to-code [{:keys [part]}]
  (str "sample " part))

(defn midi-to-code [{:keys [pitch part]}]
  (when (not (or (not pitch) (zero? pitch) (= :r pitch))) 
    (str "midi_pc " part "\n"
         "midi_note_on " (k/format-nr pitch))))

(defn note-to-code [{:keys [type options] :as note}]
  (let [code (case type
               :sample (sample-to-code note)
               :control (control-to-code note)
               #_#_:midi (midi-to-code note)
               (synth-to-code note))
        opts (k/format-options options)]
    (when code (apply str code (when (seq opts) [", " opts])))))

(defn notes-to-code [notes]
  (->> notes
       (map (fn [note] [(:time note) (note-to-code note)]))
       (sort-by first)
       (concat [[0 ""]])
       (reduce (fn [[current-time acc] [time code]] 
                 (let [dt (double (- time current-time))]
                  [time (str acc
                             (when (not (zero? dt)) (str "sleep " (k/format-nr dt) "\n"))
                             code "\n")])))
       second))

(defn open-fx [fx]
  (apply str
         (map (fn [{:keys [fx options]}] (str "with_fx " fx
                          (when (seq options)
                            (str ", " (k/format-options options ))) " do\n"))
              fx)))

(defn close-fx [fx]
  (apply str (repeat (count fx) "end\n")))

(defn format-thread [notes]
  (let [fx (get-in (first notes) [:part :fx])
        defaults (get-in (first notes) [:part :defaults])]
    (str "in_thread do\n"
         (open-fx fx)
         (when (seq defaults) (apply str "use_synth_defaults "
                                     (k/format-options defaults) "\n"))
         (->> notes
              (map resolve-part)
              (mapcat resolve-control)
              (sort-by :time)
              notes-to-code)
         (close-fx fx)
         "end\n")))

(defn leipzig->sonic-pi [notes]
  (->> notes 
       (group-by :part)
       (map second)
       (map (fn [notes] 
              (->> notes 
                   (group-by (fn [note] (int (/ (:time note) 10))))
                   (map second)
                   (map format-thread))))
       (apply concat)
       (apply str)))

(defn krach [notes]
  (when-not @sonic-pi-client (init))
  (->> notes
       leipzig->sonic-pi
       spit-and-run))

#_(defn live-loop 
  ([notes]
   (live-loop nil notes))
  ([sync notes]
   (let [part (-> notes first :part)]
    (str "live_loop " part " do\n"
         (when sync (str "sync " sync "\n"))
         (notes-to-code notes)
         "end\n"))))

#_(defn jam [loop-duration notes]
  (->> notes
       (partition-by :part)
       (mapv (partial live-loop :jam_loop))
       (apply str "live_loop " :jam_loop " do\n"
            "sleep " loop-duration "\n"
            "end\n")))

(comment
  (osc/osc-send-bundle)
  (osc/osc-send (osc/osc-client "127.0.0.1" 4560) "/test" "Hello, World!"))