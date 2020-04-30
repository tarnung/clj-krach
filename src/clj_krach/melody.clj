(ns clj-krach.melody
  (:require [leipzig.melody :as l]))

(defn update-if [m ks f & args]
  (if (contains? (get-in m (drop-last ks)) (last ks))
    (apply update-in m ks f args)
    m))

(defn format-nr [nr]
  (Double/parseDouble (format "%.3f" (double nr))))

(defn format-options [kvs]
  (apply str (interpose "," (map (fn [[k v]] (str (name k) ": " v)) kvs))))

(defn adsr [a d s r notes]
  (map (fn [note] 
         (let [proportion (/ (:duration note) (+ a d s r))]
           (-> note
               (assoc-in [:options :attack] (format-nr (* a proportion)))
               (assoc-in [:options :decay] (format-nr (* d proportion)))
               (assoc-in [:options :sustain] (format-nr (* s proportion)))
               (assoc-in [:options :release] (format-nr (* r proportion))))))
       notes))

(defn tempo [timing notes]
  (->> notes
       (l/tempo timing)
       (map (fn [note] 
              (-> note
                  (update-if [:options :attack] timing)
                  (update-if [:options :decay] timing)
                  (update-if [:options :sustain] timing)
                  (update-if [:options :release] timing))))))

(defn sample [part notes]
  (map #(assoc % :type :sample :part part) notes))

(defn opts [key value notes] 
  (map #(assoc-in % [:options key] value) notes))

(defn amp [amp-value notes]
  (map #(assoc-in % [:options :amp] amp-value) notes))

(defn pan [pan-value notes]
  (map #(assoc-in % [:options :pan] pan-value) notes))